package dev.pyprotocolgutter

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.search.ProjectScope
import com.jetbrains.python.psi.types.TypeEvalContext

/**
 * Entry point wiring: an index of the element's codebase and a type context for its file.
 * Null for code outside the project content (a library file): there is nothing of the user's to search.
 */
fun protocolSearchFor(element: PsiElement): ProtocolSearch? {
    val file = element.containingFile?.virtualFile ?: return null
    val scope = codebaseScope(element.project, file) ?: return null
    return ProtocolSearch(StubClassIndex(element.project, scope), TypeEvalContext.codeAnalysis(element.project, element.containingFile))
}

/**
 * The codebase a file belongs to: its git repository (the nearest folder with `.git` above it), or,
 * outside git, its content root. One PyCharm project often holds several unrelated repos, and they must not mix.
 * Libraries are left out, otherwise every typeshed protocol (Sized, Iterable...) and venv class would bury the useful hits.
 */
private fun codebaseScope(project: Project, file: VirtualFile): GlobalSearchScope? {
    val contentRoot = ProjectFileIndex.getInstance(project).getContentRootForFile(file) ?: return null
    val repoRoot = generateSequence(file.parent) { it.parent }.firstOrNull { it.findChild(GIT_DIR) != null }
    return GlobalSearchScopesCore.directoryScope(project, repoRoot ?: contentRoot, true)
        .intersectWith(ProjectScope.getContentScope(project))
        .intersectWith(GlobalSearchScope.notScope(ProjectScope.getLibrariesScope(project)))
}

// A worktree or a submodule has a `.git` file instead of a folder; findChild finds both.
private const val GIT_DIR = ".git"
