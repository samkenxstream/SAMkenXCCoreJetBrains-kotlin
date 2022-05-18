/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

interface KotlinVariant : KotlinFragment {
    val variantAttributes: Map<KotlinAttributeKey, String>
}

class BasicKotlinVariant(
    containingModule: KotlinModule, fragmentName: String, languageSettings: LanguageSettings? = null
) : BasicKotlinFragment(
    containingModule, fragmentName, languageSettings
), KotlinVariant {
    override val variantAttributes: MutableMap<KotlinAttributeKey, String> = mutableMapOf()
    override fun toString(): String = "variant $fragmentName"
}
