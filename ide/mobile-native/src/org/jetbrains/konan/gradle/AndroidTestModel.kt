/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import org.gradle.api.Project
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.Serializable
import kotlin.reflect.full.memberFunctions

interface AndroidTestModel : Serializable {
    val testInstrumentationRunner: String
}

data class AndroidTestModelImpl(override val testInstrumentationRunner: String) : AndroidTestModel

class AndroidTestModelBuilder : ModelBuilderService {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder =
        ErrorMessageBuilder.create(project, e, "Gradle import errors")
            .withDescription("Unable to build Android Tests configuration")

    override fun canBuild(modelName: String?): Boolean =
        modelName == AndroidTestModel::class.java.name

    override fun buildAll(modelName: String?, project: Project): AndroidTestModel? {
        val id = getTestRunnerId(project) ?: return null
        return AndroidTestModelImpl(id)
    }

    private fun getTestRunnerId(project: Project): String? {
        val extension = try {
            project.extensions.getByName("android")
        } catch (e: Throwable) {
            return null
        }
        val getDefaultConfig = extension::class.memberFunctions.find { it.name == "getDefaultConfig" }
            ?: return null
        val defaultConfig = getDefaultConfig.call(extension) ?: return null

        val getTestInstrumentationRunner = defaultConfig::class.memberFunctions.find { it.name == "getTestInstrumentationRunner" }
            ?: return null
        return getTestInstrumentationRunner.call(defaultConfig) as? String
    }
}


@Order(ExternalSystemConstants.UNORDERED)
class AndroidTestModelResolver : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses() = setOf(AndroidTestModel::class.java)
    override fun getToolingExtensionsClasses() = setOf(AndroidTestModelBuilder::class.java, Unit::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        try {
            val model = resolverCtx.getExtraProject(gradleModule, AndroidTestModel::class.java)
            if (model != null) {
                ideModule.createChild(KEY, model)
            }
        } finally {
            super.populateModuleExtraModels(gradleModule, ideModule)
        }
    }

    companion object {
        val KEY = Key.create(AndroidTestModel::class.java, ProjectKeys.MODULE.processingWeight + 1)
    }
}