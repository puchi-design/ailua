package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * PromptDomainBoundaryTest
 *
 * PASS 3C-2.1 architecture fix: the prompt package must have zero dependency
 * on `com.example.data.mock` — every input the Prompt Runtime consumes is a
 * real domain model (`LoreActivationResult` lives in `data.model`), so moving
 * the prompt code to commonMain later will not drag demo fixtures along.
 */
class PromptDomainBoundaryTest {

    private fun promptDirectory(): File {
        val candidates = listOf(
            File("app/src/main/java/com/example/data/ai/prompt"),
            File("src/main/java/com/example/data/ai/prompt"),
            File("../app/src/main/java/com/example/data/ai/prompt"),
            File("../src/main/java/com/example/data/ai/prompt"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("prompt source directory not found from ${File(".").absolutePath}")
    }

    @Test
    fun promptPackageHasNoMockDependency() {
        val directory = promptDirectory()
        val sources = directory.listFiles { file -> file.extension == "kt" }.orEmpty()
        assertTrue("no .kt sources found in ${directory.absolutePath}", sources.isNotEmpty())

        for (file in sources) {
            val text = file.readText()
            assertFalse("${file.name} references data.mock", text.contains("data.mock"))
        }
    }
}
