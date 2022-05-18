/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import java.io.Serializable

@Suppress("unchecked_cast")
internal class MutableExtrasImpl(
    initialEntries: Iterable<Extras.Entry<*>> = emptyList()
) : MutableExtras, AbstractExtras(), Serializable {

    private val extras: MutableMap<Extras.Key<*>, Extras.Entry<*>> =
        initialEntries.associateByTo(mutableMapOf()) { it.key }

    override val keys: Set<Extras.Key<*>>
        get() = extras.keys

    override val entries: Set<Extras.Entry<*>>
        get() = extras.values.toSet()

    override val size: Int
        get() = extras.size

    override fun isEmpty(): Boolean = extras.isEmpty()

    override fun <T : Any> set(key: Extras.Key<T>, value: T): T? {
        return put(Extras.Entry(key, value))
    }

    override fun <T : Any> put(entry: Extras.Entry<T>): T? {
        return extras.put(entry.key, entry)?.let { it.value as T }
    }

    override fun putAll(from: Iterable<Extras.Entry<*>>) {
        this.extras.putAll(from.associateBy { it.key })
    }

    override fun <T : Any> get(key: Extras.Key<T>): T? {
        return extras[key]?.let { it.value as T }
    }

    override fun <T : Any> remove(key: Extras.Key<T>): T? {
        return extras.remove(key)?.let { it.value as T }
    }

    override fun clear() {
        extras.clear()
    }

    internal companion object {
        private const val serialVersionUID = 0L
    }
}

@Suppress("unchecked_cast")
internal class ImmutableExtrasImpl private constructor(
    private val extras: Map<Extras.Key<*>, Extras.Entry<*>>
) : AbstractExtras(), Serializable {
    constructor(extras: Iterable<Extras.Entry<*>>) : this(extras.associateBy { it.key })

    constructor(extras: Array<out Extras.Entry<*>>) : this(extras.associateBy { it.key })

    override val keys: Set<Extras.Key<*>> = extras.keys

    override fun isEmpty(): Boolean = extras.isEmpty()

    override val size: Int = extras.size

    override val entries: Set<Extras.Entry<*>> = extras.values.toSet()

    override fun <T : Any> get(key: Extras.Key<T>): T? {
        return extras[key]?.let { it.value as T }
    }

    internal companion object {
        private const val serialVersionUID = 0L
    }

    /* Replace during serialization */
    private fun writeReplace(): Any = Surrogate(entries)

    private class Surrogate(private val entries: Set<Extras.Entry<*>>) : Serializable {
        fun readResolve(): Any = ImmutableExtrasImpl(entries)

        private companion object {
            private const val serialVersionUID = 0L
        }
    }
}

abstract class AbstractExtras : Extras {

    override val size: Int get() = keys.size

    override fun isEmpty(): Boolean = keys.isEmpty()

    override fun contains(element: Extras.Entry<*>): Boolean =
        entries.contains(element)

    override fun containsAll(elements: Collection<Extras.Entry<*>>): Boolean =
        entries.containsAll(elements)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Extras) return false
        if (other.entries != this.entries) return false
        return true
    }

    override fun hashCode(): Int {
        return 31 * entries.hashCode()
    }

    override fun toString(): String {
        return "Extras($entries)"
    }
}

abstract class AbstractEmptyExtras : Extras {
    final override val size: Int = 0

    final override val keys: Set<Extras.Key<*>> = emptySet()

    final override val entries: Set<Extras.Entry<*>> = emptySet()

    final override fun isEmpty(): Boolean = true

    final override fun <T : Any> get(key: Extras.Key<T>): T? = null

    override fun contains(key: Extras.Key<*>): Boolean = false

    override fun contains(element: Extras.Entry<out Any>): Boolean = false

    override fun containsAll(elements: Collection<Extras.Entry<out Any>>): Boolean =
        emptySet<Nothing>().containsAll(elements)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Extras) return false
        if (other.isEmpty()) return true
        return false
    }

    override fun hashCode(): Int {
        /* Random Magic Number */
        return 24112010
    }
}

internal object EmptyExtras : AbstractEmptyExtras(), Serializable {
    @Suppress("unused") // Necessary for java.io.Serializable stability
    private const val serialVersionUID = 0L

    /* Ensure single instance, even after deserialization */
    private fun readResolve(): Any = EmptyExtras
}
