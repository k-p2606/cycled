package com.example.cycled.domain

import java.io.File

/** Files the local unit tests share: the app's model asset and the expected ML cases. */
object TestAssets {
    /**
     * The app's src/main/assets/cycled_model.json. Gradle runs unit tests from the module
     * directory (app/); the second path covers runs started from the repository root.
     */
    fun modelJson(): String {
        val candidates = listOf("src/main/assets/cycled_model.json", "app/src/main/assets/cycled_model.json")
        val file = candidates.map(::File).firstOrNull { it.isFile }
            ?: error("cycled_model.json not found; tried $candidates from ${File("").absolutePath}")
        return file.readText()
    }

    fun model(): CycleModel = parseCycleModel(modelJson())

    /** app/src/test/resources/ml_cases.json, from the test classpath. */
    fun mlCasesJson(): String {
        val url = TestAssets::class.java.classLoader!!.getResource("ml_cases.json")
            ?: error("ml_cases.json not found on the test classpath")
        return url.readText()
    }
}
