package dev.pyprotocolgutter

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.types.TypeEvalContext

/**
 * Entry point wiring: the index of the element's module and a type context for its file.
 * Null for code outside any module (a library file): there is nothing of the user's to search.
 */
fun protocolSearchFor(element: PsiElement): ProtocolSearch? {
    val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
    return ProtocolSearch(StubClassIndex(module), TypeEvalContext.codeAnalysis(element.project, element.containingFile))
}
