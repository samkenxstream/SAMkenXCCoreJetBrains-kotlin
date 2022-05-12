/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import javax.inject.Inject

open class D8Exec
@Inject
constructor() : AbstractExecTask<D8Exec>(D8Exec::class.java) {
    @Transient
    @get:Internal
    lateinit var d8: D8RootExtension

    init {
        onlyIf {
            !inputFileProperty.isPresent || inputFileProperty.asFile.map { it.exists() }.get()
        }
    }

    @Input
    var d8Args: MutableList<String> = mutableListOf()

    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    @InputFile
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    override fun exec() {
        val newArgs = mutableListOf<String>()
        newArgs.addAll(d8Args)
        if (inputFileProperty.isPresent) {
            val inputFile = inputFileProperty.asFile.get()
            workingDir = workingDir ?: inputFile.parentFile
            newArgs.add(inputFile.canonicalPath)
        }
        args?.let {
            newArgs.add("--")
            newArgs.addAll(it)
        }
        this.args = newArgs
        super.exec()
    }

    companion object {
        fun create(
            compilation: KotlinJsCompilation,
            name: String,
            configuration: D8Exec.() -> Unit = {}
        ): TaskProvider<D8Exec> {
            val target = compilation.target
            val project = target.project
            val d8 = D8RootPlugin.apply(project.rootProject)
            return project.registerTask(name) {
                it.d8 = d8
                it.executable = d8.requireConfigured().executablePath.absolutePath
                it.dependsOn(compilation.compileKotlinTaskProvider)
                it.configuration()
            }
        }
    }
}