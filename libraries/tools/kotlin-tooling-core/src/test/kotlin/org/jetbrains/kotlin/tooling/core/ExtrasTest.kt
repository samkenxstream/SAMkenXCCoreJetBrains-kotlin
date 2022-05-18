@file:Suppress("RemoveExplicitTypeArguments")

package org.jetbrains.kotlin.tooling.core

import kotlin.test.*

class ExtrasTest {

    data class Box<T>(val value: T)

    @Test
    fun `test - isEmpty`() {
        assertTrue(mutableExtrasOf().isEmpty())
        assertTrue(extrasOf().isEmpty())
        assertTrue(emptyExtras().isEmpty())

        assertFalse(mutableExtrasOf().isNotEmpty())
        assertFalse(extrasOf().isNotEmpty())
        assertFalse(emptyExtras().isNotEmpty())
    }

    @Test
    fun `test - add and get`() {
        val extras = mutableExtrasOf()
        assertNull(extras[keyOf<String>()])
        assertNull(extras[keyOf<String>("a")])
        assertNull(extras[keyOf<String>("b")])

        extras[keyOf()] = "22222"
        assertEquals("22222", extras[keyOf()])
        assertNull(extras[keyOf("a")])
        assertNull(extras[keyOf("b")])

        extras[keyOf("a")] = "value a"
        assertEquals("22222", extras[keyOf()])
        assertEquals("value a", extras[keyOf("a")])
        assertNull(extras[keyOf("b")])

        extras[keyOf("b")] = "value b"
        assertEquals("22222", extras[keyOf()])
        assertEquals("value a", extras[keyOf("a")])
        assertEquals("value b", extras[keyOf("b")])

        assertNull(extras[keyOf("c")])
    }

    @Test
    fun `test - ids`() {
        val stringKey = keyOf<String>()
        val stringKeyA = keyOf<String>("a")
        val intKey = keyOf<Int>()
        val intKeyA = keyOf<Int>("a")

        val keys = setOf(stringKey, stringKeyA, intKey, intKeyA)

        val extras = extrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        val mutableExtras = mutableExtrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        assertEquals(keys, extras.keys)
        assertEquals(keys, mutableExtras.keys)
    }

    @Test
    fun `test - keys`() {
        val stringKey = keyOf<String>()
        val stringKeyA = keyOf<String>("a")
        val intKey = keyOf<Int>()
        val intKeyA = keyOf<Int>("a")

        val keys = listOf(stringKey, stringKeyA, intKey, intKeyA)

        val extras = extrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        val mutableExtras = mutableExtrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        assertEquals(keys, extras.entries.map { it.key })
        assertEquals(keys, mutableExtras.entries.map { it.key })
    }

    @Test
    fun `test - equality`() {
        val stringKey = keyOf<String>()
        val stringKeyA = keyOf<String>("a")
        val intKey = keyOf<Int>()
        val intKeyA = keyOf<Int>("a")

        val extras = extrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        val mutableExtras = mutableExtrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        assertEquals(extras, mutableExtras)
        assertEquals(extras, mutableExtras.toExtras())
        assertEquals(extras.toExtras(), mutableExtras.toExtras())
        assertEquals(extras.toMutableExtras(), mutableExtras.toExtras())
        assertEquals(extras.toMutableExtras(), mutableExtras)
        assertEquals(mutableExtras, extras)

        assertNotEquals(extras, extras + keyOf<Int>("b").withValue(2))
        assertNotEquals(mutableExtras, mutableExtras + keyOf<Int>("b").withValue(2))
    }

    @Test
    fun `test - equality - empty`() {
        val extras0 = extrasOf()
        val extras1 = mutableExtrasOf()
        val extras2 = mutableExtrasOf()

        assertNotSame(extras0, extras1)
        assertNotSame(extras1, extras2)
        assertEquals(extras0, extras1)
        assertEquals(extras1, extras2)
        assertEquals(extras2, extras1)
        assertEquals(emptyExtras(), extras0)
        assertEquals(emptyExtras(), extras1)
        assertEquals(extras0, emptyExtras())
        assertEquals(extras1, emptyExtras())
        assertEquals(emptyExtras(), extras2)
        assertEquals(extras2, emptyExtras())

        assertEquals(extras0.hashCode(), extras1.hashCode())
        assertEquals(extras1.hashCode(), extras2.hashCode())
        assertEquals(extras1.hashCode(), emptyExtras().hashCode())
    }

    @Test
    fun `test - overwrite - mutable`() {
        val key = keyOf<Int>()
        val extras = mutableExtrasOf()
        assertNull(extras.set(key, 1))
        assertEquals(1, extras[key])
        assertEquals(1, extras.set(key, 2))
        assertEquals(2, extras[key])
    }

    @Test
    fun `test - overwrite - immutable`() {
        val key = keyOf<Int>()
        val extras0 = extrasOf()
        val extras1 = extras0 + (key withValue 1)
        assertNull(extras0[key])
        assertEquals(1, extras1[key])

        val extras2 = extras1 + (key withValue 2)
        assertNull(extras0[key])
        assertEquals(1, extras1[key])
        assertEquals(2, extras2[key])
    }

    @Test
    fun `test - key equality`() {
        assertEquals(keyOf<Int>(), keyOf<Int>())
        assertEquals(keyOf<List<String>>(), keyOf<List<String>>())
        assertEquals(keyOf<Int>("a"), keyOf<Int>("a"))
        assertNotEquals<Extras.Key<*>>(keyOf<Int>(), keyOf<String>())
        assertNotEquals<Extras.Key<*>>(keyOf<Int>("a"), keyOf<Int>())
        assertNotEquals<Extras.Key<*>>(keyOf<Int>("a"), keyOf<Int>("b"))
    }

    @Test
    fun `test - add two extras`() {
        val keyA = keyOf<Int>("a")
        val keyB = keyOf<Int>("b")
        val keyC = keyOf<Int>("c")
        val keyD = keyOf<Int>()
        val keyE = keyOf<Int>()

        val extras1 = extrasOf(
            keyA withValue 0,
            keyB withValue 1,
            keyC withValue 2,
            keyD withValue 3
        )

        val extras2 = extrasOf(
            keyC withValue 4,
            keyE withValue 5
        )

        val combinedExtras = extras1 + extras2

        assertEquals(
            extrasOf(
                keyA withValue 0,
                keyB withValue 1,
                keyC withValue 4,
                keyE withValue 5
            ),
            combinedExtras
        )
    }

    @Test
    fun `test - mutable extras - remove`() {
        val extras = mutableExtrasOf(
            keyOf<String>() withValue "2",
            keyOf<String>("other") withValue "4",
            keyOf<Int>() withValue 1,
            keyOf<Int>("cash") withValue 1
        )

        extras.remove(keyOf<Int>("sunny"))
        assertEquals(1, extras[keyOf<Int>("cash")])

        assertEquals(1, extras.remove(keyOf<Int>("cash")))
        assertNull(extras[keyOf<Int>("cash")])

        assertEquals(1, extras.remove(keyOf<Int>()))
        assertNull(extras[keyOf<Int>()])

        assertEquals("4", extras.remove(keyOf<String>("other")))

        assertNull(extras[keyOf<String>("other")])

        assertEquals(
            extrasOf(keyOf<String>() withValue "2"),
            extras.toExtras()
        )
    }

    @Test
    fun `test - mutable extras - clear`() {
        val extras = mutableExtrasOf(
            keyOf<String>() withValue "2",
            keyOf<String>("other") withValue "4",
            keyOf<Int>() withValue 1,
            keyOf<Int>("cash") withValue 1
        )

        assertFalse(extras.isEmpty(), "Expected non-empty extras")
        extras.clear()
        assertTrue(extras.isEmpty(), "Expected extras to be empty")

        assertEquals(emptyExtras(), extras)
        assertEquals(emptyExtras(), extras.toExtras())
    }

    @Test
    fun `test mutable extras - putAll`() {
        val extras = mutableExtrasOf(
            keyOf<String>() withValue "1",
            keyOf<String>("overwrite") withValue "2"
        )

        extras.putAll(
            extrasOf(
                keyOf<String>("overwrite") withValue "3",
                keyOf<Int>() withValue 1
            )
        )

        assertEquals(
            extrasOf(
                keyOf<String>() withValue "1",
                keyOf<String>("overwrite") withValue "3",
                keyOf<Int>() withValue 1
            ),
            extras
        )
    }

    @Test
    fun `test - filterType`() {
        val extras = mutableExtrasOf()
        extras[keyOf<Box<String>>()] = Box("first")
        extras[keyOf<Box<String>>("other")] = Box("second")
        extras[keyOf<Box<Int>>()] = Box(1)

        assertEquals<List<Box<String>>>(
            listOf(Box("first"), Box("second")),
            extras.filterIsType<Box<String>>().map { it.value }.toList()
        )

        assertEquals<List<Box<Int>>>(
            listOf(Box(1)), extras.filterIsType<Box<Int>>().map { it.value }.toList()
        )

        assertEquals(
            emptyList(), extras.filterIsType<Box<*>>().toList()
        )

        assertEquals(
            emptyList(), extras.filterIsType<Any>().toList()
        )
    }
}
