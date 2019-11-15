/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID

inline fun <T> Project.forEachModule(key: Key<T>, block: (T, ModuleData, rootProjectPath: String) -> Unit) {
    for (projectInfo in ProjectDataManager.getInstance().getExternalProjectsData(this, GRADLE_SYSTEM_ID)) {
        val projectStructure = projectInfo.externalProjectStructure ?: continue
        val rootProjectPath = projectStructure.data.linkedExternalProjectPath
        for (moduleNode in ExternalSystemApiUtil.findAll(projectStructure, ProjectKeys.MODULE)) {
            block(
                ExternalSystemApiUtil.find(moduleNode, key)?.data ?: continue,
                moduleNode.data,
                rootProjectPath
            )
        }
    }
}