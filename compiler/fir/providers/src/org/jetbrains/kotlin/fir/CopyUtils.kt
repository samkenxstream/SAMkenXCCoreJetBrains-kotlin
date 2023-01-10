/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.typeAttributeExtensions
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.resolve.directExpansionType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId

inline fun FirFunctionCall.copyAsImplicitInvokeCall(
    setupCopy: FirImplicitInvokeCallBuilder.() -> Unit
): FirImplicitInvokeCall {
    val original = this

    return buildImplicitInvokeCall {
        source = original.source
        annotations.addAll(original.annotations)
        typeArguments.addAll(original.typeArguments)
        explicitReceiver = original.explicitReceiver
        dispatchReceiver = original.dispatchReceiver
        extensionReceiver = original.extensionReceiver
        argumentList = original.argumentList
        calleeReference = original.calleeReference

        setupCopy()
    }
}

fun FirTypeRef.resolvedTypeFromPrototype(
    type: ConeKotlinType
): FirResolvedTypeRef {
    return if (type is ConeErrorType) {
        buildErrorTypeRef {
            source = this@resolvedTypeFromPrototype.source
            this.type = type
            diagnostic = type.diagnostic
        }
    } else {
        buildResolvedTypeRef {
            source = this@resolvedTypeFromPrototype.source
            this.type = type
            delegatedTypeRef = when (val original = this@resolvedTypeFromPrototype) {
                is FirResolvedTypeRef -> original.delegatedTypeRef
                is FirUserTypeRef -> original
                else -> null
            }
            annotations += this@resolvedTypeFromPrototype.annotations
        }
    }
}

fun FirTypeRef.errorTypeFromPrototype(
    diagnostic: ConeDiagnostic
): FirErrorTypeRef {
    return buildErrorTypeRef {
        source = this@errorTypeFromPrototype.source
        this.diagnostic = diagnostic
    }
}

fun List<FirAnnotation>.computeTypeAttributes(session: FirSession, predefined: List<ConeAttribute<*>> = emptyList()): ConeAttributes {
    if (this.isEmpty()) {
        if (predefined.isEmpty()) return ConeAttributes.Empty
        return ConeAttributes.create(predefined)
    }
    val attributes = mutableListOf<ConeAttribute<*>>()
    attributes += predefined
    val customAnnotations = mutableListOf<FirAnnotation>()
    for (annotation in this) {
        when (annotation.tryExpandClassId(session)) {
            CompilerConeAttributes.Exact.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.Exact
            CompilerConeAttributes.NoInfer.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.NoInfer
            CompilerConeAttributes.ExtensionFunctionType.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.ExtensionFunctionType
            CompilerConeAttributes.ContextFunctionTypeParams.ANNOTATION_CLASS_ID ->
                attributes +=
                    CompilerConeAttributes.ContextFunctionTypeParams(
                        annotation.extractContextReceiversCount() ?: 0
                    )

            CompilerConeAttributes.UnsafeVariance.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.UnsafeVariance
            else -> {
                val attributeFromPlugin = session.extensionService.typeAttributeExtensions.firstNotNullOfOrNull {
                    it.extractAttributeFromAnnotation(annotation)
                }
                if (attributeFromPlugin != null) {
                    attributes += attributeFromPlugin
                } else {
                    customAnnotations += annotation
                }
            }
        }
    }
    if (customAnnotations.isNotEmpty()) {
        attributes += CustomAnnotationTypeAttribute(customAnnotations)
    }
    return ConeAttributes.create(attributes)
}

private fun FirAnnotation.tryExpandClassId(session: FirSession): ClassId? {
    return when (val directlyExpanded = coneClassLikeType?.directExpansionType(session) { it.expandedConeType }) {
        null -> coneClassLikeType?.classId // mutually recursive typealiases
        else -> directlyExpanded.fullyExpandedType(session).classId
    }
}

private fun FirAnnotation.extractContextReceiversCount() =
    (argumentMapping.mapping[StandardNames.CONTEXT_FUNCTION_TYPE_PARAMETER_COUNT_NAME] as? FirConstExpression<*>)?.value as? Int
