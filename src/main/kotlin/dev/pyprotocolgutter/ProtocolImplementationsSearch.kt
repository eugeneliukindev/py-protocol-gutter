package dev.pyprotocolgutter

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.util.Processor
import com.jetbrains.python.psi.PyClass

/** Adds implicit implementers and functions matching a callback protocol to Go to Implementation (Ctrl+Alt+B) on a Protocol. */
class ProtocolImplementationsSearch : QueryExecutorBase<PsiElement, DefinitionsScopedSearch.SearchParameters>(true) {
    override fun processQuery(params: DefinitionsScopedSearch.SearchParameters, consumer: Processor<in PsiElement>) {
        val protocol = params.element as? PyClass ?: return
        val search = protocolSearchFor(protocol) ?: return
        (search.implementations(protocol) + search.matchingFunctions(protocol)).all(consumer::process)
    }
}
