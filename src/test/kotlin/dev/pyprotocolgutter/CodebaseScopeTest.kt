package dev.pyprotocolgutter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.python.psi.PyFile

/** One PyCharm project (even one module) often holds several git repos side by side; they must not mix. */
class CodebaseScopeTest : BasePlatformTestCase() {

    fun testOtherRepoInSameProjectIsNotSearched() {
        myFixture.addFileToProject("typing.py", "class Protocol: pass\n")
        myFixture.addFileToProject("repo_a/.git/HEAD", "")
        myFixture.addFileToProject("repo_b/.git/HEAD", "")
        val repoA = myFixture.addFileToProject(
            "repo_a/store.py",
            "from typing import Protocol\n\nclass Storage(Protocol):\n    def save(self, key: str) -> None: ...\n\n" +
                "class FileStore:\n    def save(self, key: str) -> None: ...\n",
        ) as PyFile
        val repoB = myFixture.addFileToProject(
            "repo_b/other_store.py",
            "class OtherStore:\n    def save(self, key: str) -> None: ...\n",
        ) as PyFile
        val storage = repoA.findTopLevelClass("Storage")!!
        val otherStore = repoB.findTopLevelClass("OtherStore")!!

        assertEquals(listOf("FileStore"), protocolSearchFor(storage)!!.implementations(storage).map { it.name })
        assertEmpty(protocolSearchFor(otherStore)!!.protocolsOf(otherStore))
    }
}
