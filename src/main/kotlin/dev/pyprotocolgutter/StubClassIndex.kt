package dev.pyprotocolgutter

import com.intellij.openapi.project.Project
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.stubs.StubIndex
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.stubs.PyFunctionNameIndex
import com.jetbrains.python.psi.stubs.PySuperClassIndex

/** Project sources only: matching every typeshed protocol (Sized, Iterable...) would bury the useful hits. */
class StubClassIndex(private val project: Project) : ClassIndex {
    private val scope = ProjectScope.getProjectScope(project)

    override fun classesDefining(methodName: String): Collection<PyClass> =
        PyFunctionNameIndex.find(methodName, project, scope).mapNotNull { it.containingClass }

    override fun protocols(): Collection<PyClass> =
        StubIndex.getElements(PySuperClassIndex.KEY, "Protocol", project, scope, PyClass::class.java)
}
