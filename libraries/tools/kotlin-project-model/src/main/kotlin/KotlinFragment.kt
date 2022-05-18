/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment
import org.jetbrains.kotlin.tooling.core.closure
import org.jetbrains.kotlin.tooling.core.withClosure
import java.io.File

interface KotlinFragment {
    val containingModule: KotlinModule

    val fragmentName: String
    val directRefinesDependencies: Iterable<KotlinFragment>

    val languageSettings: LanguageSettings?

    // TODO: scopes
    val declaredModuleDependencies: Iterable<KotlinModuleDependency>

    // TODO: should this be source roots or source files?
    val kotlinSourceRoots: Iterable<File>

    val refinesClosure: Set<KotlinFragment>
        get() = this.closure { it.directRefinesDependencies }

    val withRefinesClosure: Set<KotlinFragment>
        get() = this.withClosure { it.directRefinesDependencies }

    companion object
}

interface KotlinVariant : KotlinFragment {
    val variantAttributes: Map<KotlinAttributeKey, String>
}

val KotlinFragment.fragmentAttributeSets: Map<KotlinAttributeKey, Set<String>>
    get() = mutableMapOf<KotlinAttributeKey, MutableSet<String>>().apply {
        containingModule.variantsContainingFragment(this@fragmentAttributeSets).forEach { variant ->
            variant.variantAttributes.forEach { (attribute, value) ->
                getOrPut(attribute) { mutableSetOf() }.add(value)
            }
        }
    }


val KotlinVariant.platform get() = variantAttributes[KotlinPlatformTypeAttribute]

open class BasicKotlinFragment(
    override val containingModule: KotlinModule,
    override val fragmentName: String,
    override val languageSettings: LanguageSettings? = null
) : KotlinFragment {

    override val directRefinesDependencies: MutableSet<BasicKotlinFragment> = mutableSetOf()

    override val declaredModuleDependencies: MutableSet<KotlinModuleDependency> = mutableSetOf()

    override var kotlinSourceRoots: Iterable<File> = emptyList()
    override fun toString(): String = "fragment $fragmentName"
}

class BasicKotlinVariant(
    containingModule: KotlinModule,
    fragmentName: String,
    languageSettings: LanguageSettings? = null
) : BasicKotlinFragment(
    containingModule,
    fragmentName,
    languageSettings
), KotlinVariant {
    override val variantAttributes: MutableMap<KotlinAttributeKey, String> = mutableMapOf()
    override fun toString(): String = "variant $fragmentName"
}
