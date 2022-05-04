/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.IdSignatureDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryBytesSource
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrFragmentAndBinaryAst
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.javaFile
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import java.io.File
import java.util.Collections


class IncrementalCache(private val library: KotlinLibrary, cachePath: String) {
    companion object {
        private const val moduleHeader = "cache.ic.header.bin"

        private const val binaryAstSuffix = "ic.ast.bin"
        private const val metadataSuffix = "ic.metadata.bin"
    }

    private var forceRebuildJs = false
    private val cacheDir = File(cachePath)
    private val signatureToIndexMappingFromMetadata = mutableMapOf<KotlinSourceFile, MutableMap<IdSignature, Int>>()

    class CacheHeader(
        val klibFileHash: ICHash = ICHash(), val configHash: ICHash = ICHash()
    ) {
        fun toProtoStream(out: CodedOutputStream) {
            klibFileHash.toProtoStream(out)
            configHash.toProtoStream(out)
        }

        companion object {
            fun fromProtoStream(input: CodedInputStream): CacheHeader {
                val klibFileHash = ICHash.fromProtoStream(input)
                val configHash = ICHash.fromProtoStream(input)
                return CacheHeader(klibFileHash, configHash)
            }
        }
    }

    private var cacheHeader = File(cacheDir, moduleHeader).useCodedInputIfExists {
        CacheHeader.fromProtoStream(this)
    } ?: CacheHeader()

    private fun loadCachedFingerprints() = File(cacheDir, moduleHeader).useCodedInputIfExists {
        // skip cache header
        CacheHeader.fromProtoStream(this@useCodedInputIfExists)
        buildMapUntil(readInt32()) {
            val file = KotlinSourceFile.fromProtoStream(this@useCodedInputIfExists)
            put(file, ICHash.fromProtoStream(this@useCodedInputIfExists))
        }
    } ?: emptyMap()

    private class KotlinLibraryHeader(
        val sourceFiles: List<KotlinSourceFile>, val signatureDeserializers: Map<KotlinSourceFile, IdSignatureDeserializer>
    )

    private val kotlinLibraryHeader: KotlinLibraryHeader by lazy {
        val filesCount = library.fileCount()
        val extReg = ExtensionRegistryLite.newInstance()
        val files = buildListUntil(filesCount) {
            val fileProto = IrFile.parseFrom(library.file(it).codedInputStream, extReg)
            add(KotlinSourceFile(fileProto.fileEntry.name))
        }

        val deserializers = buildMapUntil(filesCount) {
            put(files[it], IdSignatureDeserializer(IrLibraryFileFromBytes(object : IrLibraryBytesSource() {
                private fun err(): Nothing = error("Not supported")
                override fun irDeclaration(index: Int): ByteArray = err()
                override fun type(index: Int): ByteArray = err()
                override fun signature(index: Int): ByteArray = library.signature(index, it)
                override fun string(index: Int): ByteArray = library.string(index, it)
                override fun body(index: Int): ByteArray = err()
                override fun debugInfo(index: Int): ByteArray? = null
            }), null))
        }

        KotlinLibraryHeader(files, deserializers)
    }

    private class KotlinSourceFileMetadataFromDisk(
        override val inverseDependencies: KotlinSourceFileMap<Set<IdSignature>>,
        override val directDependencies: KotlinSourceFileMap<Set<IdSignature>>,

        override val importedInlineFunctions: Map<IdSignature, ICHash>
    ) : KotlinSourceFileMetadata()

    private object KotlinSourceFileMetadataNotExist : KotlinSourceFileMetadata() {
        override val inverseDependencies = KotlinSourceFileMap<Set<IdSignature>>(emptyMap())
        override val directDependencies = KotlinSourceFileMap<Set<IdSignature>>(emptyMap())

        override val importedInlineFunctions = emptyMap<IdSignature, ICHash>()
    }

    private val kotlinLibrarySourceFileMetadata = mutableMapOf<KotlinSourceFile, KotlinSourceFileMetadata>()

    private fun KotlinSourceFile.getCacheFile(suffix: String) = File(cacheDir, "${File(path).name}.${path.stringHashForIC()}.$suffix")

    private fun commitCacheHeader(fingerprints: List<Pair<KotlinSourceFile, ICHash>>) = File(cacheDir, moduleHeader).useCodedOutput {
        cacheHeader.toProtoStream(this)
        writeInt32NoTag(fingerprints.size)
        for ((srcFile, fingerprint) in fingerprints) {
            srcFile.toProtoStream(this)
            fingerprint.toProtoStream(this)
        }
    }

    fun buildModuleArtifactAndCommitCache(
        moduleName: String,
        rebuiltFileFragments: Map<KotlinSourceFile, JsIrFragmentAndBinaryAst>,
        signatureToIndexMapping: Map<KotlinSourceFile, Map<IdSignature, Int>>
    ): ModuleArtifact {
        val fileArtifacts = kotlinLibraryHeader.sourceFiles.map { srcFile ->
            val binaryAstFile = srcFile.getCacheFile(binaryAstSuffix)
            val rebuiltFileFragment = rebuiltFileFragments[srcFile]
            if (rebuiltFileFragment != null) {
                binaryAstFile.apply { recreate() }.writeBytes(rebuiltFileFragment.binaryAst)
            }

            commitSourceFileMetadata(srcFile, signatureToIndexMapping[srcFile] ?: emptyMap())
            SrcFileArtifact(srcFile.path, rebuiltFileFragment?.fragment, binaryAstFile)
        }

        return ModuleArtifact(moduleName, fileArtifacts, cacheDir, forceRebuildJs)
    }

    data class ModifiedFiles(
        val modified: Map<KotlinSourceFile, KotlinSourceFileMetadata> = emptyMap(),
        val newFiles: Collection<KotlinSourceFile> = emptyList(),
        val removed: Collection<KotlinSourceFile> = emptyList()
    )

    fun collectModifiedFiles(configHash: ICHash): ModifiedFiles {
        val klibFileHash = library.libraryFile.javaFile().fileHashForIC()
        cacheHeader = when {
            cacheHeader.configHash != configHash -> {
                cacheDir.deleteRecursively()
                CacheHeader(klibFileHash, configHash)
            }
            cacheHeader.klibFileHash != klibFileHash -> CacheHeader(klibFileHash, configHash)
            else -> return ModifiedFiles()
        }

        val cachedFingerprints = loadCachedFingerprints()
        val deletedFiles = cachedFingerprints.keys.toMutableSet()
        val newFiles = mutableListOf<KotlinSourceFile>()

        val newFingerprints = kotlinLibraryHeader.sourceFiles.mapIndexed { index, file -> file to library.fingerprint(index) }
        val modifiedFiles = buildMap(newFingerprints.size) {
            for ((file, fileNewFingerprint) in newFingerprints) {
                val oldFingerprint = cachedFingerprints[file]
                if (oldFingerprint == null) {
                    newFiles += file
                }
                if (oldFingerprint != fileNewFingerprint) {
                    val metadata = fetchSourceFileMetadata(file, false)
                    put(file, metadata)
                }
                deletedFiles.remove(file)
            }
        }

        for (deletedFile in deletedFiles) {
            deletedFile.getCacheFile(binaryAstSuffix).delete()
            deletedFile.getCacheFile(metadataSuffix).delete()
        }
        forceRebuildJs = deletedFiles.isNotEmpty()
        commitCacheHeader(newFingerprints)

        return ModifiedFiles(modifiedFiles, newFiles, deletedFiles)
    }

    fun fetchSourceFileFullMetadata(sourceFile: KotlinSourceFile): KotlinSourceFileMetadata {
        return fetchSourceFileMetadata(sourceFile, true)
    }

    fun updateSourceFileMetadata(sourceFile: KotlinSourceFile, sourceFileMetadata: KotlinSourceFileMetadata) {
        kotlinLibrarySourceFileMetadata[sourceFile] = sourceFileMetadata
    }

    private fun fetchSourceFileMetadata(sourceFile: KotlinSourceFile, loadSignatures: Boolean) =
        kotlinLibrarySourceFileMetadata.getOrPut(sourceFile) {
            val signatureToIndexMapping = signatureToIndexMappingFromMetadata.getOrPut(sourceFile) { mutableMapOf() }
            fun IdSignatureDeserializer.deserializeIdSignatureAndSave(index: Int): IdSignature {
                val signature = deserializeIdSignature(index)
                signatureToIndexMapping[signature] = index
                return signature
            }

            sourceFile.getCacheFile(metadataSuffix).useCodedInputIfExists {
                val deserializer: IdSignatureDeserializer by lazy {
                    kotlinLibraryHeader.signatureDeserializers[sourceFile] ?: error("TODO write an error")
                }

                fun readDependencies() = buildMapUntil(readInt32()) {
                    val libraryFile = KotlinLibraryFile.fromProtoStream(this@useCodedInputIfExists)
                    val depends = buildMapUntil(readInt32()) {
                        val dependencySrcFile = KotlinSourceFile.fromProtoStream(this@useCodedInputIfExists)
                        val dependencySignatures = if (loadSignatures) {
                            buildSetUntil(readInt32()) { add(deserializer.deserializeIdSignatureAndSave(readInt32())) }
                        } else {
                            repeat(readInt32()) { readInt32() }
                            emptySet()
                        }
                        put(dependencySrcFile, dependencySignatures)
                    }
                    put(libraryFile, depends)
                }

                val directDependencies = KotlinSourceFileMap(readDependencies())
                val reverseDependencies = KotlinSourceFileMap(readDependencies())

                val importedInlineFunctions = if (loadSignatures) {
                    buildMapUntil(readInt32()) {
                        val signature = deserializer.deserializeIdSignatureAndSave(readInt32())
                        val transitiveHash = ICHash.fromProtoStream(this@useCodedInputIfExists)
                        put(signature, transitiveHash)
                    }
                } else {
                    emptyMap()
                }

                KotlinSourceFileMetadataFromDisk(reverseDependencies, directDependencies, importedInlineFunctions)
            } ?: KotlinSourceFileMetadataNotExist
        }

    private fun commitSourceFileMetadata(sourceFile: KotlinSourceFile, signatureToIndexMapping: Map<IdSignature, Int>) {
        val headerCacheFile = sourceFile.getCacheFile(metadataSuffix)
        val sourceFileMetadata = kotlinLibrarySourceFileMetadata[sourceFile] ?: error("TODO message")
        if (sourceFileMetadata.isEmpty()) {
            headerCacheFile.delete()
            return
        }
        if (sourceFileMetadata is KotlinSourceFileMetadataFromDisk) {
            return
        }

        val signatureToIndexMappingSaved = signatureToIndexMappingFromMetadata[sourceFile] ?: emptyMap()
        fun serializeSignature(signature: IdSignature): Int {
            val index = signatureToIndexMapping[signature] ?: signatureToIndexMappingSaved[signature]
            return index ?: icError("cannot find index in $sourceFile for $signature")
        }

        headerCacheFile.useCodedOutput {
            fun writeDepends(depends: KotlinSourceFileMap<Set<IdSignature>>) {
                writeInt32NoTag(depends.size)
                for ((libFile, srcFiles) in depends) {
                    libFile.toProtoStream(this)
                    writeInt32NoTag(srcFiles.size)
                    for ((srcFile, signatures) in srcFiles) {
                        srcFile.toProtoStream(this)
                        writeInt32NoTag(signatures.size)
                        for (signature in signatures) {
                            writeInt32NoTag(serializeSignature(signature))
                        }
                    }
                }
            }

            writeDepends(sourceFileMetadata.directDependencies)
            writeDepends(sourceFileMetadata.inverseDependencies)

            writeInt32NoTag(sourceFileMetadata.importedInlineFunctions.size)
            for ((signature, transitiveHash) in sourceFileMetadata.importedInlineFunctions) {
                writeInt32NoTag(serializeSignature(signature))
                transitiveHash.toProtoStream(this)
            }
        }
    }
}
