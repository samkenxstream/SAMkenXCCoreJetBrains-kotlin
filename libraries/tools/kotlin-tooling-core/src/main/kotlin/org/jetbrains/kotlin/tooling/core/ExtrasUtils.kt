/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

/**
 *  Creates a value based key for accessing any [Extras] container
 *
 * @param T The type of data that is stored in the extras container
 * ```kotlin
 * keyOf<Int>() == keyOf<Int>()
 * keyOf<Int>() != keyOf<String>()
 * keyOf<List<Int>>() == keyOf<List<Int>>()
 * keyOf<List<*>>() != keyOf<List<Int>>()
 * ```
 *
 * @param name This typed keys can also be distinguished with an additional name. In this case
 * ```kotlin
 * keyOf<Int>() != keyOf<Int>("a")
 * keyOf<Int>("a") == keyOf<Int>("a")
 * keyOf<Int>("b") != keyOf<Int>("a")
 * keyOf<String>("a") != keyOf<Int>("a")
 * ```
 */
inline fun <reified T : Any> keyOf(name: String? = null): Extras.Key<T> =
    Extras.Key(reifiedTypeSignatureOf(), name)

fun emptyExtras(): Extras = EmptyExtras

fun extrasOf() = emptyExtras()

fun extrasOf(vararg entries: Extras.Entry<*>): Extras = ImmutableExtrasImpl(entries)

fun mutableExtrasOf(): MutableExtras = MutableExtrasImpl()

fun mutableExtrasOf(vararg entries: Extras.Entry<*>): MutableExtras = MutableExtrasImpl(entries.toList())

fun Iterable<Extras.Entry<*>>.toExtras(): Extras = ImmutableExtrasImpl(this)

fun Iterable<Extras.Entry<*>>.toMutableExtras(): MutableExtras = MutableExtrasImpl(this)

infix fun <T : Any> Extras.Key<T>.withValue(value: T): Extras.Entry<T> = Extras.Entry(this, value)

operator fun Extras.plus(entry: Extras.Entry<*>): Extras = ImmutableExtrasImpl(this.entries + entry)

operator fun Extras.plus(entries: Iterable<Extras.Entry<*>>): Extras = ImmutableExtrasImpl(this.entries + entries)

