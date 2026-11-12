package dev.pyprotocolgutter

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.stubs.PyClassAttributesIndex
import com.jetbrains.python.psi.stubs.PySuperClassIndex

/** Stub-index lookups limited to one codebase, see [codebaseScope]. */
class StubClassIndex(private val project: Project, private val scope: GlobalSearchScope) : ClassIndex {
    override fun classesDeclaring(memberName: String): Collection<PyClass> =
        StubIndex.getElements(PyClassAttributesIndex.KEY, memberName, project, scope, PyClass::class.java)

    override fun protocols(): Collection<PyClass> =
        StubIndex.getElements(PySuperClassIndex.KEY, "Protocol", project, scope, PyClass::class.java)

    override fun topLevelFunctions(): Collection<PyFunction> {
        val psiManager = PsiManager.getInstance(project)
        return FileTypeIndex.getFiles(PythonFileType.INSTANCE, scope)
            .mapNotNull { psiManager.findFile(it) as? PyFile }
            .flatMap { it.topLevelFunctions }
    }
}
