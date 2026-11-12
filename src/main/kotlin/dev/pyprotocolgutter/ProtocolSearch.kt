package dev.pyprotocolgutter

import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.typing.getProtocolMembers
import com.jetbrains.python.codeInsight.typing.isProtocol
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.impl.PyBuiltinCache
import com.jetbrains.python.psi.types.PyClassLikeType
import com.jetbrains.python.psi.types.PyTypeChecker
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.pyi.PyiUtil

/** Where candidate classes come from: stub indexes in the IDE. */
interface ClassIndex {
    /** Classes that declare a method, class attribute or instance attribute with this name in their own body. */
    fun classesDeclaring(memberName: String): Collection<PyClass>

    /** Classes that list `Protocol` among their bases. */
    fun protocols(): Collection<PyClass>

    /** Module-level functions: candidates for callback protocols. */
    fun topLevelFunctions(): Collection<PyFunction>
}

/**
 * Structural (implicit) Protocol conformance, the way Go interfaces work.
 * Explicit subclasses are left out: PyCharm already marks those.
 * Protocols that plain `object` satisfies (only `__hash__`, `__eq__`, `__str__`...) are skipped: every class matches them.
 */
class ProtocolSearch(private val index: ClassIndex, private val context: TypeEvalContext) {

    fun implementations(protocol: PyClass): List<PyClass> {
        val protocolType = meaningfulProtocolType(protocol) ?: return emptyList()
        // shortcut: a class that inherits every protocol member from a base is not found (its base is); upgrade by walking inheritors.
        val memberNames = protocolType.getProtocolMembers(context).mapNotNull { it.name }.distinct()
        return memberNames.flatMap(index::classesDeclaring)
            .distinct()
            .filter { it != protocol && satisfies(it, protocol, protocolType) }
            .sortedBy { it.qualifiedName ?: it.name }
    }

    fun protocolsOf(cls: PyClass): List<PyClass> {
        if (cls.isProtocol(context)) return emptyList()
        return index.protocols()
            .filter { protocol -> meaningfulProtocolType(protocol)?.let { satisfies(cls, protocol, it) } == true }
            .sortedBy { it.qualifiedName ?: it.name }
    }

    /** True for a callback protocol: its only member is `__call__`, and that `__call__` does not accept anything at all. */
    fun isCallbackProtocol(protocol: PyClass): Boolean = callbackProtocolType(protocol) != null

    /** Module-level functions whose signature matches a callback protocol. Empty for any other protocol. */
    fun matchingFunctions(protocol: PyClass): List<PyFunction> {
        val protocolType = callbackProtocolType(protocol) ?: return emptyList()
        // shortcut: every module-level function is type-checked, callers compute this lazily; add a parameter-count prefilter if it gets slow.
        return index.topLevelFunctions()
            .filter { !PyiUtil.isOverload(it, context) && PyTypeChecker.match(protocolType, context.getType(it), context) }
            .sortedBy { it.qualifiedName ?: it.name }
    }

    /** Callback protocols this function matches. Empty for an `@overload` stub: only the implementation counts. */
    fun protocolsOf(function: PyFunction): List<PyClass> {
        if (PyiUtil.isOverload(function, context)) return emptyList()
        return index.protocols()
            .filter { protocol -> callbackProtocolType(protocol)?.let { PyTypeChecker.match(it, context.getType(function), context) } == true }
            .sortedBy { it.qualifiedName ?: it.name }
    }

    private fun meaningfulProtocolType(protocol: PyClass): PyClassLikeType? {
        if (!protocol.isProtocol(context)) return null
        val protocolType = protocol.getType(context)?.toInstance() ?: return null
        val objectType = PyBuiltinCache.getInstance(protocol).objectType
        if (objectType != null && PyTypeChecker.match(protocolType, objectType, context)) return null
        return protocolType
    }

    private fun callbackProtocolType(protocol: PyClass): PyClassLikeType? {
        val protocolType = meaningfulProtocolType(protocol) ?: return null
        if (protocolType.getProtocolMembers(context).mapNotNull { it.name }.toSet() != setOf(PyNames.CALL)) return null
        val call = protocol.findMethodByName(PyNames.CALL, true, context) ?: return null
        // (*args, **kwargs) matches every function: that is noise, not a contract.
        if (call.parameterList.hasPositionalContainer() && call.parameterList.hasKeywordContainer()) return null
        return protocolType
    }

    private fun satisfies(cls: PyClass, protocol: PyClass, protocolType: PyClassLikeType): Boolean =
        !cls.isProtocol(context) &&
            !cls.isSubclass(protocol, context) &&
            PyTypeChecker.match(protocolType, cls.getType(context)?.toInstance(), context)
}
