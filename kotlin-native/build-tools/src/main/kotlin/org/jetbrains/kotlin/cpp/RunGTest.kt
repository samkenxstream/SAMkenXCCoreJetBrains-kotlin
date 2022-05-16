/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Run [googletest](https://google.github.io/googletest) test binary from [executable].
 *
 * Test reports are placed in [reportFileUnprocessed] and in [reportFile] (decorates each test with [testName]).
 *
 * @see CompileToBitcodePlugin
 */
abstract class RunGTest : DefaultTask() {
    /**
     * Decorating test names in the report.
     *
     * Useful when CI merges different test results of the same test but for different test targets.
     */
    @get:Input
    abstract val testName: Property<String>

    /**
     * Test executable
     */
    @get:InputFile
    abstract val executable: RegularFileProperty

    /**
     * Test report with each test name decorated with [testName].
     */
    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    /**
     * Undecorated test report.
     */
    @get:OutputFile
    abstract val reportFileUnprocessed: RegularFileProperty

    /**
     * Run a subset of tests.
     *
     * Follows [googletest](https://google.github.io/googletest/advanced.html#running-a-subset-of-the-tests) syntax:
     * a ':'-separated list of glob patterns.
     *
     * Examples:
     * * `SomeTest*` - run every test starting with `SomeTest`.
     * * `SomeTest*:*stress*` - run every test starting with `SomeTest` and also every test containing `stress`.
     * * `SomeTest*:-SomeTest.flakyTest` - Run every test starting with `SomeTest` except `SomeTest.flakyTest`.
     */
    @get:Input
    @get:Optional
    abstract val filter: Property<String>

    /**
     * Suppression rules for TSAN.
     */
    @get:InputFile
    @get:Optional
    abstract val tsanSuppressionsFile: RegularFileProperty

    @TaskAction
    fun run() {
        reportFileUnprocessed.asFile.get().parentFile.mkdirs()

        // Do not run this in workers, because we don't want this task to run in parallel.
        // TODO: Consider using build services https://docs.gradle.org/current/userguide/build_services.html#concurrent_access_to_the_service to have configurable parallel execution.
        project.exec {
            executable = this@RunGTest.executable.asFile.get().absolutePath

            filter.orNull?.also {
                args("--gtest_filter=${it}")
            }
            args("--gtest_output=xml:${reportFileUnprocessed.asFile.get().absolutePath}")
            tsanSuppressionsFile.orNull?.also {
                environment("TSAN_OPTIONS", "suppressions=${it.asFile.absolutePath}")
            }
        }

        reportFile.asFile.get().parentFile.mkdirs()

        // TODO: Better to use proper XML parsing.
        var contents = reportFileUnprocessed.asFile.get().readText()
        contents = contents.replace("<testsuite name=\"", "<testsuite name=\"${testName.get()}.")
        contents = contents.replace("classname=\"", "classname=\"${testName.get()}.")
        reportFile.asFile.get().writeText(contents)
    }
}
