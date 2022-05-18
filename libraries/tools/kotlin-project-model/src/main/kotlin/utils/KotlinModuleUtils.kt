/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.utils

import org.jetbrains.kotlin.project.model.KotlinModule
import org.jetbrains.kotlin.project.model.KotlinFragment
import org.jetbrains.kotlin.project.model.KotlinVariant
import org.jetbrains.kotlin.tooling.core.closure

fun KotlinModule.variantsContainingFragment(fragment: KotlinFragment): Iterable<KotlinVariant> =
    variants.filter { variant -> fragment in variant.withRefinesClosure }

fun KotlinModule.findRefiningFragments(fragment: KotlinFragment): Iterable<KotlinFragment> {
    return fragment.closure { seedFragment ->
        fragments.filter { otherFragment -> seedFragment in otherFragment.declaredRefinesDependencies }
    }
}
