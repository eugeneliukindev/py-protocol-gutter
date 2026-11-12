package dev.pyprotocolgutter

import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.typing.getProtocolMembers
import com.jetbrains.python.codeInsight.typing.isProtocol
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.types.PyClassLikeType
import com.jetbrains.python.psi.types.PyTypeChecker
import com.jetbrains.python.psi.types.TypeEvalContext

/** Where candidate classes come from: stub indexes in the IDE. */
interface ClassIndex {
    /** Classes that define a method with this name in their own body. */
    fun classesDefining(methodName: String): Collection<PyClass>

    /** Classes that list `Protocol` among their bases. */
    fun protocols(): Collection<PyClass>

    /** Module-level functions: candidates for callback protocols. */
    fun topLevelFunctions(): Collection<PyFunction>
}

/**
 * Structural (implicit) Protocol conformance, the way Go interfaces work.
 * Explicit subclasses are left out: PyCharm already marks those.
 */
class ProtocolSearch(private val index: ClassIndex, private val context: TypeEvalContext) {

    fun implementations(protocol: PyClass): List<PyClass> {
        if (!protocol.isProtocol(context)) return emptyList()
        val protocolType = protocol.getType(context)?.toInstance() ?: return emptyList()
        // shortcut: candidates come from method names only, so attribute-only protocols and classes
        // that inherit every method from a base are not found; upgrade by also indexing attributes and walking inheritors.
        val methodNames = protocolType.getProtocolMembers(context)
            .mapNotNull { (it.element as? PyFunction)?.name }
            .distinct()
        return methodNames.flatMap(index::classesDefining)
            .distinct()
            .filter { it != protocol && satisfies(it, protocol, protocolType) }
            .sortedBy { it.qualifiedName ?: it.name }
    }

    fun protocolsOf(cls: PyClass): List<PyClass> {
        if (cls.isProtocol(context)) return emptyList()
        return index.protocols()
            .filter { it.isProtocol(context) }
            .filter { protocol -> protocol.getType(context)?.toInstance()?.let { satisfies(cls, protocol, it) } == true }
            .sortedBy { it.qualifiedName ?: it.name }
    }

    /** True for a callback protocol: its only member is `__call__`, and that `__call__` does not accept anything at all. */
    fun isCallbackProtocol(protocol: PyClass): Boolean = callbackProtocolType(protocol) != null

    /** Module-level functions whose signature matches a callback protocol. Empty for any other protocol. */
    fun matchingFunctions(protocol: PyClass): List<PyFunction> {
        val protocolType = callbackProtocolType(protocol) ?: return emptyList()
        // shortcut: every module-level function is type-checked, callers compute this lazily; add a parameter-count prefilter if it gets slow.
        return index.topLevelFunctions()
            .filter { PyTypeChecker.match(protocolType, context.getType(it), context) }
            .sortedBy { it.qualifiedName ?: it.name }
    }

    /** Callback protocols this function matches. */
    fun protocolsOf(function: PyFunction): List<PyClass> =
        index.protocols()
            .filter { protocol -> callbackProtocolType(protocol)?.let { PyTypeChecker.match(it, context.getType(function), context) } == true }
            .sortedBy { it.qualifiedName ?: it.name }

    private fun callbackProtocolType(protocol: PyClass): PyClassLikeType? {
        if (!protocol.isProtocol(context)) return null
        val protocolType = protocol.getType(context)?.toInstance() ?: return null
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
