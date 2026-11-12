package dev.pyprotocolgutter

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction

class ProtocolSearchTest : BasePlatformTestCase() {

    private lateinit var classes: Map<String, PyClass>
    private lateinit var functions: Map<String, PyFunction>

    override fun setUp() {
        super.setUp()
        // No Python SDK in light tests, so a local typing module gives Protocol its "typing.Protocol" name.
        myFixture.addFileToProject("typing.py", "class Protocol: pass\n")
        val file = myFixture.configureByText(
            "store.py",
            """
            from typing import Protocol

            class Storage(Protocol):
                def save(self, key: str) -> None: ...

            class FileStore:
                def save(self, key: str) -> None: ...

            class WrongSignature:
                def save(self, key: int, extra: int) -> None: ...

            class Explicit(Storage):
                def save(self, key: str) -> None: ...

            class Unrelated:
                def load(self) -> None: ...

            class Handler(Protocol):
                def __call__(self, event: str) -> int: ...

            class AnyCallback(Protocol):
                def __call__(self, *args, **kwargs) -> None: ...

            def on_event(event: str) -> int: ...

            def wrong_event(event: int, extra: int) -> int: ...
            """.trimIndent(),
        ) as PyFile
        classes = file.topLevelClasses.associateBy { it.name!! }
        functions = file.topLevelFunctions.associateBy { it.name!! }
    }

    private fun names(found: List<PsiNamedElement>) = found.map { it.name }

    fun testProtocolListsOnlyImplicitMatches() {
        val storage = classes.getValue("Storage")
        assertEquals(listOf("FileStore"), names(protocolSearchFor(storage)!!.implementations(storage)))
    }

    fun testClassListsProtocolsItSatisfies() {
        val search = protocolSearchFor(classes.getValue("FileStore"))!!
        assertEquals(listOf("Storage"), names(search.protocolsOf(classes.getValue("FileStore"))))
        assertEmpty(search.protocolsOf(classes.getValue("WrongSignature")))
        assertEmpty(search.protocolsOf(classes.getValue("Unrelated")))
        assertEmpty(search.protocolsOf(classes.getValue("Storage")))
    }

    fun testGoToImplementationIncludesImplicitImplementers() {
        val found = DefinitionsScopedSearch.search(classes.getValue("Storage")).findAll()
        assertContainsElements(found, classes.getValue("FileStore"), classes.getValue("Explicit"))
        assertDoesntContain(found, classes.getValue("WrongSignature"))
    }

    fun testGutterIconsAppear() {
        val gutters = myFixture.findAllGutters().mapNotNull { it.tooltipText }
        assertContainsElements(
            gutters,
            "Implemented by: FileStore", "Satisfies protocol: Storage",
            "Satisfies protocol: Handler", "Matching functions: click to search",
        )
    }

    fun testCallbackProtocolListsMatchingFunctions() {
        val handler = classes.getValue("Handler")
        val search = protocolSearchFor(handler)!!
        assertEquals(listOf("on_event"), names(search.matchingFunctions(handler)))
        assertEmpty(search.matchingFunctions(classes.getValue("Storage")))
    }

    fun testCallbackProtocolThatAcceptsAnythingIsIgnored() {
        val anyCallback = classes.getValue("AnyCallback")
        assertEmpty(protocolSearchFor(anyCallback)!!.matchingFunctions(anyCallback))
    }

    fun testFunctionListsCallbackProtocolsItSatisfies() {
        val search = protocolSearchFor(functions.getValue("on_event"))!!
        assertEquals(listOf("Handler"), names(search.protocolsOf(functions.getValue("on_event"))))
        assertEmpty(search.protocolsOf(functions.getValue("wrong_event")))
    }

    fun testGoToImplementationIncludesMatchingFunctions() {
        val found = DefinitionsScopedSearch.search(classes.getValue("Handler")).findAll()
        assertContainsElements(found, functions.getValue("on_event"))
        assertDoesntContain(found, functions.getValue("wrong_event"))
    }
}
