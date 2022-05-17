/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.analysis.cfa.FirCallsEffectAnalyzer
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeLabel
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

abstract class EventOccurrencesRangeInfo<E : EventOccurrencesRangeInfo<E, K>, K : Any>(
    map: Map<K, EventOccurrencesRange> = mapOf()
) : ControlFlowInfo<E, K, EventOccurrencesRange>(map) {

    override fun merge(other: E): E =
        operation(other, EventOccurrencesRange::or)

    fun plus(other: E): E =
        when {
            isEmpty() -> other
            other.isEmpty() ->
                @Suppress("UNCHECKED_CAST")
                this as E
            else -> operation(other, EventOccurrencesRange::plus)
        }

    private inline fun operation(other: E, op: (EventOccurrencesRange, EventOccurrencesRange) -> EventOccurrencesRange): E {
        @Suppress("UNCHECKED_CAST")
        val result = LinkedHashMap((this as E).map)
        for (symbol in keys.union(other.keys)) {
            val kind1 = this[symbol] ?: EventOccurrencesRange.ZERO
            val kind2 = other[symbol] ?: EventOccurrencesRange.ZERO
            result[symbol] = op.invoke(kind1, kind2)
        }
        @Suppress("UNCHECKED_CAST")
        return if (this is FirCallsEffectAnalyzer.LambdaInvocationInfo)
            FirCallsEffectAnalyzer.LambdaInvocationInfo(result as Map<FirBasedSymbol<*>, EventOccurrencesRange>) as E
        else
            PropertyInitializationInfo(result as Map<FirPropertySymbol, EventOccurrencesRange>) as E
    }
}

class PropertyInitializationInfo(
    map: Map<FirPropertySymbol, EventOccurrencesRange> = mapOf()
) : EventOccurrencesRangeInfo<PropertyInitializationInfo, FirPropertySymbol>(map) {
    companion object {
        val EMPTY = PropertyInitializationInfo()
    }

    override val constructor: (Map<FirPropertySymbol, EventOccurrencesRange>) -> PropertyInitializationInfo =
        ::PropertyInitializationInfo

    override val empty: () -> PropertyInitializationInfo =
        ::EMPTY
}

class PathAwarePropertyInitializationInfo(
    map: Map<EdgeLabel, PropertyInitializationInfo> = mapOf()
) : PathAwareControlFlowInfo<PathAwarePropertyInitializationInfo, PropertyInitializationInfo>(map) {
    companion object {
        val EMPTY = PathAwarePropertyInitializationInfo(mapOf(NormalPath to PropertyInitializationInfo.EMPTY))
    }

    override val constructor: (Map<EdgeLabel, PropertyInitializationInfo>) -> PathAwarePropertyInitializationInfo =
        ::PathAwarePropertyInitializationInfo

    override val empty: () -> PathAwarePropertyInitializationInfo =
        ::EMPTY
}
