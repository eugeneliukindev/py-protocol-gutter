package dev.pyprotocolgutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import javax.swing.Icon

class ProtocolLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getName(): String = "Python Protocol: implicit implementations"

    override fun getIcon(): Icon = AllIcons.Gutter.ImplementedMethod

    // Everything is computed in the slow pass: index lookups and type matching are too heavy for the fast one.
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        for (element in elements) {
            when (val owner = element.parent) {
                is PyClass -> if (owner.nameIdentifier == element) addClassMarkers(element, owner, result)
                is PyFunction -> if (owner.nameIdentifier == element && owner.parent is PyFile) addFunctionMarker(element, owner, result)
            }
        }
    }

    private fun addClassMarkers(element: PsiElement, cls: PyClass, result: MutableCollection<in LineMarkerInfo<*>>) {
        val search = protocolSearchFor(element) ?: return
        marker(element, search.implementations(cls), AllIcons.Gutter.ImplementedMethod, IMPLEMENTED_BY)?.let(result::add)
        marker(element, search.protocolsOf(cls), AllIcons.Gutter.ImplementingMethod, SATISFIES)?.let(result::add)
        if (search.isCallbackProtocol(cls)) {
            // Scanning every function of the module on each re-highlight is too slow, so the list is built on click.
            result.add(
                NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
                    .setTargets(NotNullLazyValue.lazy { protocolSearchFor(cls)?.matchingFunctions(cls).orEmpty() })
                    .setPopupTitle(MATCHING_FUNCTIONS)
                    .setEmptyPopupText("No functions match this callback protocol")
                    .setTooltipText("$MATCHING_FUNCTIONS: click to search")
                    .createLineMarkerInfo(element),
            )
        }
    }

    private fun addFunctionMarker(element: PsiElement, function: PyFunction, result: MutableCollection<in LineMarkerInfo<*>>) {
        val search = protocolSearchFor(element) ?: return
        marker(element, search.protocolsOf(function), AllIcons.Gutter.ImplementingMethod, SATISFIES)?.let(result::add)
    }

    private fun marker(element: PsiElement, targets: List<PsiNamedElement>, icon: Icon, title: String): LineMarkerInfo<*>? {
        if (targets.isEmpty()) return null
        return NavigationGutterIconBuilder.create(icon)
            .setTargets(targets)
            .setPopupTitle(title)
            .setTooltipText("$title: " + targets.joinToString { it.name ?: "?" })
            .createLineMarkerInfo(element)
    }

    private companion object {
        const val IMPLEMENTED_BY = "Implemented by"
        const val SATISFIES = "Satisfies protocol"
        const val MATCHING_FUNCTIONS = "Matching functions"
    }
}
