package dev.pyprotocolgutter

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.VfsTestUtil
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile

/** One PyCharm project often holds unrelated repos as separate modules; a heavy test is needed to have two modules. */
class ModuleScopeTest : HeavyPlatformTestCase() {

    private fun sourceRoot(module: com.intellij.openapi.module.Module): VirtualFile =
        tempDir.createVirtualDir().also { PsiTestUtil.addSourceRoot(module, it) }

    private fun pyClass(root: VirtualFile, path: String, text: String, name: String): PyClass {
        val file = PsiManager.getInstance(project).findFile(VfsTestUtil.createFile(root, path, text)) as PyFile
        return file.findTopLevelClass(name)!!
    }

    fun testOtherModuleIsNotSearched() {
        val mainRoot = sourceRoot(module)
        val otherRoot = sourceRoot(createModule("other_repo"))
        VfsTestUtil.createFile(mainRoot, "typing.py", "class Protocol: pass\n")
        val storage = pyClass(
            mainRoot, "store.py",
            "from typing import Protocol\n\nclass Storage(Protocol):\n    def save(self, key: str) -> None: ...\n\n" +
                "class FileStore:\n    def save(self, key: str) -> None: ...\n",
            "Storage",
        )
        val otherStore = pyClass(otherRoot, "other_store.py", "class OtherStore:\n    def save(self, key: str) -> None: ...\n", "OtherStore")

        assertEquals(listOf("FileStore"), protocolSearchFor(storage)!!.implementations(storage).map { it.name })
        assertEmpty(protocolSearchFor(otherStore)!!.protocolsOf(otherStore))
    }
}
