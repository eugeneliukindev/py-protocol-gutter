package dev.pyprotocolgutter

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.stubs.PyFunctionNameIndex
import com.jetbrains.python.psi.stubs.PySuperClassIndex

/**
 * Searches one module, not the whole project: a PyCharm project often holds unrelated repos as separate modules.
 * Libraries are left out too, otherwise every typeshed protocol (Sized, Iterable...) would bury the useful hits.
 */
class StubClassIndex(private val module: Module) : ClassIndex {
    /** Implementers of a protocol can live in its module or in modules that depend on it. */
    override fun classesDefining(methodName: String): Collection<PyClass> =
        PyFunctionNameIndex.find(methodName, module.project, GlobalSearchScope.moduleWithDependentsScope(module))
            .mapNotNull { it.containingClass }

    /** A class can only satisfy protocols it can see: its module and the modules it depends on. */
    override fun protocols(): Collection<PyClass> =
        StubIndex.getElements(
            PySuperClassIndex.KEY, "Protocol", module.project,
            GlobalSearchScope.moduleWithDependenciesScope(module), PyClass::class.java,
        )

    /** Functions matching a callback protocol live where its implementers do: its module and the modules depending on it. */
    override fun topLevelFunctions(): Collection<PyFunction> {
        val psiManager = PsiManager.getInstance(module.project)
        return FileTypeIndex.getFiles(PythonFileType.INSTANCE, GlobalSearchScope.moduleWithDependentsScope(module))
            .mapNotNull { psiManager.findFile(it) as? PyFile }
            .flatMap { it.topLevelFunctions }
    }
}
