package dev.pyprotocolgutter

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.types.TypeEvalContext

/** Entry point wiring: the real index and a type context for the file the user is looking at. */
fun protocolSearchFor(element: PsiElement): ProtocolSearch =
    ProtocolSearch(StubClassIndex(element.project), TypeEvalContext.codeAnalysis(element.project, element.containingFile))
