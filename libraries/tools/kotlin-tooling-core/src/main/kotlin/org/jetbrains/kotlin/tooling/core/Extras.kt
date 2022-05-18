/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import java.io.Serializable

/**
 * A generic container holding typed and scoped values.
 * ### Attaching and getting simple typed values:
 * ```kotlin
 * val extras = mutableExtrasOf()
 * extras[keyOf<Int>()] = 42 // Attach arbitrary Int value
 * extras[keyOf<String>()] = "Hello" // Attach arbitrary String value
 *
 * extras[keyOf<Int>()] // -> returns 42
 * extras[keyOf<String>] // -> returns "Hello"
 * ```
 *
 * ### Attaching multiple values with the same type by naming the keys
 * ```kotlin
 * val extras = mutableExtrasOf()
 * extras[keyOf<Int>("a")] = 1 // Attach Int with name 'a'
 * extras[keyOf<Int>("b")] = 2 // Attach Int with name 'b'
 *
 * extras[keyOf<Int>("a")] // -> returns 1
 * extras[keyOf<Int>("b")] // -> returns 2
 * ```
 *
 * ### Creating immutable extras
 * ```kotlin
 * val extras = extrasOf(
 *     keyOf<Int>() withValue 1,
 *     keyOf<String>() withValue "Hello"
 * )
 * ```
 *
 * ### Converting to immutable extras
 * ```kotlin
 * val extras = mutableExtrasOf(
 *     keyOf<Int>() withValue 0
 * )
 *
 * // Captures the content, similar to `.toList()` or `.toSet()`
 * val immutableExtras = extras.toExtras()
 * ```
 *
 * ### Use case example: Filtering Extras
 * ```kotlin
 * val extras = extrasOf(
 *     keyOf<Int>() withValue 0,
 *     keyOf<Int>("keep") withValue 1,
 *     keyOf<String>() withValue "Hello"
 * )
 *
 * val filteredExtras = extras
 *     .filter { (key, value) -> key.id.name == "keep" || value is String }
 *     .toExtras()
 * ```
 */
interface Extras : Collection<Extras.Entry<out Any>> {
    class Key<T : Any> @PublishedApi internal constructor(
        internal val type: ReifiedTypeSignature<T>,
        val name: String? = null,
    ) : Serializable {

        val stableString: String
            get() {
                return if (name == null) type.signature
                else "${type.signature};$name"
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Key<*>) return false
            if (name != other.name) return false
            if (type != other.type) return false
            return true
        }

        override fun hashCode(): Int {
            var result = name?.hashCode() ?: 0
            result = 31 * result + type.hashCode()
            return result
        }

        override fun toString(): String = stableString

        companion object {
            fun fromString(stableString: String): Key<*> {
                @OptIn(UnsafeApi::class) return if (stableString.contains(';')) {
                    val split = stableString.split(';', limit = 2)
                    Key(ReifiedTypeSignature(split[0]), split[1])
                } else Key(ReifiedTypeSignature(stableString))
            }

            private const val serialVersionUID = 0L
        }
    }

    class Entry<T : Any>(val key: Key<T>, val value: T) : Serializable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Entry<*>) return false
            if (other.key != key) return false
            if (other.value != value) return false
            return true
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + value.hashCode()
            return result
        }

        override fun toString(): String = "$key=$value"

        operator fun component1() = key
        operator fun component2() = value

        internal companion object {
            private const val serialVersionUID = 0L
        }
    }

    val keys: Set<Key<*>>
    val entries: Set<Entry<*>>
    operator fun <T : Any> get(key: Key<T>): T?
    operator fun contains(key: Key<*>): Boolean = key in keys
    override fun iterator(): Iterator<Entry<out Any>> = entries.iterator()
    fun isNotEmpty() = !isEmpty()
}

interface MutableExtras : Extras {
    /**
     * @return The previous value or null if no previous value was set
     */
    operator fun <T : Any> set(key: Extras.Key<T>, value: T): T?

    fun <T : Any> put(entry: Extras.Entry<T>): T?

    fun putAll(from: Iterable<Extras.Entry<*>>)

    fun <T : Any> remove(key: Extras.Key<T>): T?

    fun clear()
}
