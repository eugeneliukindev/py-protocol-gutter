package dev.pyprotocolgutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyClass
import javax.swing.Icon

class ProtocolLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getName(): String = "Python Protocol: implicit implementations"

    override fun getIcon(): Icon = AllIcons.Gutter.ImplementedMethod

    // Everything is computed in the slow pass: index lookups and type matching are too heavy for the fast one.
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        for (element in elements) {
            val cls = element.parent as? PyClass ?: continue
            if (cls.nameIdentifier != element) continue
            val search = protocolSearchFor(element)
            marker(element, search.implementations(cls), AllIcons.Gutter.ImplementedMethod, "Implemented by")?.let(result::add)
            marker(element, search.protocolsOf(cls), AllIcons.Gutter.ImplementingMethod, "Satisfies protocol")?.let(result::add)
        }
    }

    private fun marker(element: PsiElement, targets: List<PyClass>, icon: Icon, title: String): LineMarkerInfo<*>? {
        if (targets.isEmpty()) return null
        return NavigationGutterIconBuilder.create(icon)
            .setTargets(targets)
            .setPopupTitle(title)
            .setTooltipText("$title: " + targets.joinToString { it.name ?: "?" })
            .createLineMarkerInfo(element)
    }
}
