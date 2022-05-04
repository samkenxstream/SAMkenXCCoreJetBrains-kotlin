/* * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrFragmentAndBinaryAst
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import java.io.File


fun interface CacheExecutor {
    fun execute(
        mainModule: IrModuleFragment,
        allModules: Collection<IrModuleFragment>,
        deserializer: JsIrLinker,
        configuration: CompilerConfiguration,
        dirtyFiles: Collection<IrFile>,
        exportedDeclarations: Set<FqName>,
        mainArguments: List<String>?
    ): List<JsIrFragmentAndBinaryAst>
}

class CacheUpdater(
    mainModule: String,
    allModules: Collection<String>,
    icCachePaths: Collection<String>,
    private val compilerConfiguration: CompilerConfiguration,
    private val irFactory: () -> IrFactory,
    private val mainArguments: List<String>?,
    private val executor: CacheExecutor
) {
    private val hashCalculator = InlineFunctionTransitiveHashCalculator()
    private val transitiveHashes
        get() = hashCalculator.transitiveHashes

    private val libraries = loadLibraries(allModules)
    private val dependencyGraph = buildDependenciesGraph(libraries)
    private val configHash = compilerConfiguration.configHashForIC()

    private val cacheMap = libraries.values.zip(icCachePaths).toMap()

    private val mainLibraryFile = KotlinLibraryFile(File(mainModule).canonicalPath)
    private val mainLibrary = libraries[mainLibraryFile] ?: error("Main library not found in libraries: ${mainLibraryFile.path}")

    private val incrementalCaches = libraries.entries.associate { (libFile, lib) ->
        val cachePath = cacheMap[lib] ?: error("Cannot find cache path for library ${lib.libraryName}")
        libFile to IncrementalCache(lib, cachePath)
    }

    class DirtyFileStat(
        var newAdded: Boolean = false,
        var modified: Boolean = false,
        var updatedExports: Boolean = false,
        var updatedInlineImports: Boolean = false,
        var removed: Boolean = false
    ) {
        override fun toString() = buildList(5) {
            if (newAdded) {
                push("added new file")
            } else if (modified) {
                push("modified file")
            }
            if (updatedExports) {
                push("updated exports")
            }
            if (updatedInlineImports) {
                push("updated inline imports")
            }
            if (removed) {
                push("removed file")
            }
        }.joinToString()
    }

    private val dirtyFileStats = KotlinSourceFileMutableMap<DirtyFileStat>()

    fun getDirtyFileStats(): KotlinSourceFileMap<DirtyFileStat> = dirtyFileStats

    private fun MutableMap<KotlinSourceFile, DirtyFileStat>.updateDirtFileStat(
        srcFile: KotlinSourceFile,
        setNewAdded: Boolean = false,
        setModified: Boolean = false,
        setUpdatedExports: Boolean = false,
        setUpdatedInlineImports: Boolean = false,
        setRemoved: Boolean = false
    ) {
        when (val stat = this[srcFile]) {
            null -> this[srcFile] = DirtyFileStat(setNewAdded, setModified, setUpdatedExports, setUpdatedInlineImports, setRemoved)
            else -> {
                stat.newAdded = stat.newAdded or setNewAdded
                stat.modified = stat.modified or setModified
                stat.updatedExports = stat.updatedExports or setUpdatedExports
                stat.updatedInlineImports = stat.updatedInlineImports or setUpdatedInlineImports
                stat.removed = stat.removed or setRemoved
            }
        }
    }

    private fun loadLibraries(allModules: Collection<String>): Map<KotlinLibraryFile, KotlinLibrary> {
        val allResolvedDependencies = jsResolveLibraries(
            allModules,
            compilerConfiguration[JSConfigurationKeys.REPOSITORIES] ?: emptyList(),
            compilerConfiguration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
        )

        return allResolvedDependencies.getFullList().associateBy { KotlinLibraryFile(it) }
    }

    private fun buildDependenciesGraph(libraries: Map<KotlinLibraryFile, KotlinLibrary>): Map<KotlinLibrary, List<KotlinLibrary>> {
        val nameToKotlinLibrary = libraries.values.associateBy { it.moduleName }
        return libraries.values.associateWith {
            it.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { depName ->
                nameToKotlinLibrary[depName] ?: error("No Library found for $depName")
            }
        }
    }

    private class DirtyFileExports(
        override val inverseDependencies: KotlinSourceFileMutableMap<Set<IdSignature>> = KotlinSourceFileMutableMap()
    ) : KotlinSourceFileExports() {
        override fun getExportedSignatures(): Set<IdSignature> = allExportedSignatures

        val allExportedSignatures = mutableSetOf<IdSignature>()
    }

    private class DirtyFileMetadata(
        val importedSignatures: Collection<IdSignature>,

        val oldDirectDependencies: KotlinSourceFileMap<*>,

        override val inverseDependencies: KotlinSourceFileMutableMap<MutableSet<IdSignature>> = KotlinSourceFileMutableMap(),
        override val directDependencies: KotlinSourceFileMutableMap<MutableSet<IdSignature>> = KotlinSourceFileMutableMap(),

        override val importedInlineFunctions: MutableMap<IdSignature, ICHash> = mutableMapOf()
    ) : KotlinSourceFileMetadata() {
        fun addInverseDependency(lib: KotlinLibraryFile, src: KotlinSourceFile, signature: IdSignature) =
            inverseDependencies.addSignature(lib, src, signature)

        fun addDirectDependency(lib: KotlinLibraryFile, src: KotlinSourceFile, signature: IdSignature) =
            directDependencies.addSignature(lib, src, signature)
    }

    private class UpdatedDependenciesMetadata(oldMetadata: KotlinSourceFileMetadata) : KotlinSourceFileMetadata() {
        private val oldInverseDependencies = oldMetadata.inverseDependencies
        private val newExportedSignatures: Set<IdSignature> by lazy { inverseDependencies.flatSignatures() }

        var importedInlineFunctionsModified = false

        override val inverseDependencies = oldMetadata.inverseDependencies.toMutable()
        override val directDependencies = oldMetadata.directDependencies
        override val importedInlineFunctions = oldMetadata.importedInlineFunctions

        override fun getExportedSignatures(): Set<IdSignature> = newExportedSignatures

        fun isExportedSignaturesUpdated() = newExportedSignatures != oldInverseDependencies.flatSignatures()
    }

    private fun collectExportedSymbolsForDirtyFiles(
        dirtyFiles: KotlinSourceFileMap<KotlinSourceFileMetadata>
    ): KotlinSourceFileMutableMap<KotlinSourceFileExports> {
        val exportedSymbols = KotlinSourceFileMutableMap<KotlinSourceFileExports>()

        for ((libFile, srcFiles) in dirtyFiles) {
            val exportedSymbolFiles = mutableMapOf<KotlinSourceFile, KotlinSourceFileExports>()
            for ((srcFile, srcFileMetadata) in srcFiles) {
                val loadingFileExports = DirtyFileExports()
                for ((dependentLib, dependentFiles) in srcFileMetadata.inverseDependencies) {
                    val dependentCache = incrementalCaches[dependentLib] ?: continue
                    val dirtyLibFiles = dirtyFiles[dependentLib] ?: emptyMap()
                    for (dependentFile in dependentFiles.keys) {
                        if (dependentFile !in dirtyLibFiles) {
                            val dependentSrcFileMetadata = dependentCache.fetchSourceFileFullMetadata(dependentFile)
                            dependentSrcFileMetadata.directDependencies[libFile, srcFile]?.let {
                                loadingFileExports.inverseDependencies[dependentLib, dependentFile] = it
                                loadingFileExports.allExportedSignatures += it
                            }
                        }
                    }
                }
                exportedSymbolFiles[srcFile] = loadingFileExports
            }
            if (exportedSymbolFiles.isNotEmpty()) {
                exportedSymbols[libFile] = exportedSymbolFiles
            }
        }
        return exportedSymbols
    }

    private fun rebuildDirtySourceMetadata(
        jsIrLinker: JsIrLinker,
        loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>,
        dirtySrcFiles: KotlinSourceFileMap<KotlinSourceFileExports>,
    ): KotlinSourceFileMap<DirtyFileMetadata> {
        val idSignatureToFile = mutableMapOf<IdSignature, Pair<KotlinLibraryFile, KotlinSourceFile>>()
        val updatedMetadata = KotlinSourceFileMutableMap<DirtyFileMetadata>()

        for ((lib, irModule) in loadedFragments) {
            val moduleDeserializer = jsIrLinker.moduleDeserializer(irModule.descriptor)
            val incrementalCache = incrementalCaches[lib] ?: error("TODO ERROR")
            for (fileDeserializer in moduleDeserializer.fileDeserializers()) {
                val libSrcFile = KotlinSourceFile(fileDeserializer.file)
                val allTopLevelSignatures = fileDeserializer.reversedSignatureIndex
                idSignatureToFile.putAll(allTopLevelSignatures.keys.asSequence().map { it to (lib to libSrcFile) })

                val reachableSignatures = fileDeserializer.symbolDeserializer.signatureDeserializer.signatureToIndexMapping()
                val importedSignatures = reachableSignatures.keys - allTopLevelSignatures.keys
                val metadata = incrementalCache.fetchSourceFileFullMetadata(libSrcFile)
                updatedMetadata[lib, libSrcFile] = DirtyFileMetadata(importedSignatures, metadata.directDependencies)
            }
        }

        for ((libFile, srcFiles) in updatedMetadata) {
            val libDirtySrcFiles = dirtySrcFiles[libFile] ?: continue
            for ((srcFile, internalHeader) in srcFiles) {
                val dirtySrcFile = libDirtySrcFiles[srcFile] ?: continue

                dirtySrcFile.inverseDependencies.forEachFile { dependentLibFile, dependentSrcFile, signatures ->
                    signatures.forEach { internalHeader.addInverseDependency(dependentLibFile, dependentSrcFile, it) }
                }

                for (importedSignature in internalHeader.importedSignatures) {
                    val (signatureFromLib, signatureFromFile) = idSignatureToFile[importedSignature] ?: continue
                    internalHeader.addDirectDependency(signatureFromLib, signatureFromFile, importedSignature)
                    transitiveHashes[importedSignature]?.let { internalHeader.importedInlineFunctions[importedSignature] = it }

                    val dependencyCacheData = updatedMetadata[signatureFromLib, signatureFromFile] ?: error("TODO ERROR")
                    dependencyCacheData.addInverseDependency(libFile, srcFile, importedSignature)
                }
            }
        }

        val result = KotlinSourceFileMutableMap<DirtyFileMetadata>()

        for ((libFile, sourceFiles) in dirtySrcFiles) {
            val incrementalCache = incrementalCaches[libFile] ?: error("TODO ERROR")
            val srcFileUpdatedMetadata = updatedMetadata[libFile] ?: error("TODO ERROR")
            for (sourceFile in sourceFiles.keys) {
                val srcMetadata = srcFileUpdatedMetadata[sourceFile] ?: error("TODO ERROR")
                incrementalCache.updateSourceFileMetadata(sourceFile, srcMetadata)
                result[libFile, sourceFile] = srcMetadata
            }
        }

        return result
    }

    private fun KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>.addNewMetadata(
        libFile: KotlinLibraryFile, srcFile: KotlinSourceFile, oldMetadata: KotlinSourceFileMetadata
    ) = this[libFile, srcFile] ?: UpdatedDependenciesMetadata(oldMetadata).also {
        this[libFile, srcFile] = it
    }

    private fun KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>.addDependenciesWithUpdatedSignatures(
        libFile: KotlinLibraryFile, srcFile: KotlinSourceFile, srcFileMetadata: DirtyFileMetadata
    ) {
        // go through dependencies and collect dependencies with updated signatures
        for ((dependencyLibFile, dependencySrcFiles) in srcFileMetadata.directDependencies) {
            val dependencyCache = incrementalCaches[dependencyLibFile] ?: error("TODO message1")
            for ((dependencySrcFile, newSignatures) in dependencySrcFiles) {
                val dependencySrcMetadata = dependencyCache.fetchSourceFileFullMetadata(dependencySrcFile)
                val oldSignatures = dependencySrcMetadata.inverseDependencies[libFile, srcFile] ?: emptySet()
                if (oldSignatures == newSignatures) {
                    continue
                }
                val newMetadata = addNewMetadata(dependencyLibFile, dependencySrcFile, dependencySrcMetadata)
                newMetadata.inverseDependencies[libFile, srcFile] = newSignatures
            }
        }
    }

    private fun KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>.addDependenciesWithRemovedInverseDependencies(
        libFile: KotlinLibraryFile, srcFile: KotlinSourceFile, srcFileMetadata: DirtyFileMetadata
    ) {
        // go through old dependencies and look for removed dependencies
        for ((oldDependencyLibFile, oldDependencySrcFiles) in srcFileMetadata.oldDirectDependencies) {
            val dependencyCache = incrementalCaches[oldDependencyLibFile] ?: error("TODO message")
            val newDirectDependencyFiles = srcFileMetadata.directDependencies[oldDependencyLibFile] ?: emptyMap()
            for (oldDependencySrcFile in oldDependencySrcFiles.keys) {
                if (oldDependencySrcFile in newDirectDependencyFiles) {
                    continue
                }
                val dependencySrcMetadata = dependencyCache.fetchSourceFileFullMetadata(oldDependencySrcFile)
                if (dependencySrcMetadata.inverseDependencies[libFile, srcFile] != null) {
                    val newMetadata = addNewMetadata(oldDependencyLibFile, oldDependencySrcFile, dependencySrcMetadata)
                    newMetadata.inverseDependencies.removeFile(libFile, srcFile)
                }
            }
        }
    }

    private fun KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>.addDependentsWithUpdatedImports(srcFileMetadata: DirtyFileMetadata) {
        // go through dependent files and check if their inline imports were modified
        for ((dependentLibFile, dependentSrcFiles) in srcFileMetadata.inverseDependencies) {
            val dependentCache = incrementalCaches[dependentLibFile] ?: error("TODO message1")
            for (dependentSrcFile in dependentSrcFiles.keys) {
                val dependentSrcMetadata = dependentCache.fetchSourceFileFullMetadata(dependentSrcFile)
                val importedInlineModified = dependentSrcMetadata.importedInlineFunctions.any {
                    transitiveHashes[it.key]?.let { newHash -> newHash != it.value } ?: false
                }
                if (importedInlineModified) {
                    val newMetadata = addNewMetadata(dependentLibFile, dependentSrcFile, dependentSrcMetadata)
                    newMetadata.importedInlineFunctionsModified = true
                }
            }
        }
    }

    private fun collectFilesWithModifiedExportsOrInlineImports(
        loadedDirtyFiles: KotlinSourceFileMap<DirtyFileMetadata>
    ): KotlinSourceFileMap<UpdatedDependenciesMetadata> {
        val filesWithModifiedExports = KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>()

        loadedDirtyFiles.forEachFile { libFile, srcFile, srcFileMetadata ->
            filesWithModifiedExports.addDependenciesWithUpdatedSignatures(libFile, srcFile, srcFileMetadata)
            filesWithModifiedExports.addDependenciesWithRemovedInverseDependencies(libFile, srcFile, srcFileMetadata)
            filesWithModifiedExports.addDependentsWithUpdatedImports(srcFileMetadata)
        }

        return filesWithModifiedExports
    }

    private fun collectFilesToRebuildSignatures(
        filesWithModifiedExports: KotlinSourceFileMap<UpdatedDependenciesMetadata>
    ): KotlinSourceFileMap<KotlinSourceFileExports> {
        val libFilesToRebuild = KotlinSourceFileMutableMap<KotlinSourceFileExports>()

        for ((libFile, srcFiles) in filesWithModifiedExports) {
            val filesToRebuild by lazy(LazyThreadSafetyMode.NONE) { libFilesToRebuild.getOrPutFiles(libFile) }
            val fileStats by lazy(LazyThreadSafetyMode.NONE) { dirtyFileStats.getOrPutFiles(libFile) }
            val cache = incrementalCaches[libFile] ?: error("TODO message")

            for ((srcFile, srcFileMetadata) in srcFiles) {
                val isSignatureUpdated = srcFileMetadata.isExportedSignaturesUpdated()
                if (isSignatureUpdated || srcFileMetadata.importedInlineFunctionsModified) {
                    // if exported signatures or imported inline functions were modified - rebuild
                    filesToRebuild[srcFile] = srcFileMetadata
                    if (isSignatureUpdated) {
                        fileStats.updateDirtFileStat(srcFile, setUpdatedExports = true)
                    }
                    if (srcFileMetadata.importedInlineFunctionsModified) {
                        fileStats.updateDirtFileStat(srcFile, setUpdatedInlineImports = true)
                    }
                } else {
                    // if signatures and inline functions are the same - just update cache metadata
                    cache.updateSourceFileMetadata(srcFile, srcFileMetadata)
                }
            }
        }

        return libFilesToRebuild
    }

    private fun buildModuleArtifactsAndCommitCache(
        jsIrLinker: JsIrLinker,
        loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>,
        rebuiltFileFragments: List<JsIrFragmentAndBinaryAst>
    ): List<ModuleArtifact> {
        val fragmentToLibName = loadedFragments.entries.associate { it.value to it.key }

        val rebuiltSrcFiles = rebuiltFileFragments.groupBy { fragmentToLibName[it.irFile.module] ?: error("TODO message") }

        val visited = mutableSetOf<KotlinLibrary>()
        val artifacts = mutableListOf<ModuleArtifact>()
        fun addArtifact(lib: KotlinLibrary) {
            if (visited.add(lib)) {
                dependencyGraph[lib]?.forEach(::addArtifact)

                val libFile = KotlinLibraryFile(lib)
                val incrementalCache = incrementalCaches[libFile] ?: error("TODO message")
                val libFragment = loadedFragments[libFile] ?: error("TODO message")
                val libRebuiltFiles = rebuiltSrcFiles[libFile]?.associateBy { KotlinSourceFile(it.irFile) } ?: emptyMap()
                val moduleDeserializer = jsIrLinker.moduleDeserializer(libFragment.descriptor)

                val signatureToIndexMapping = moduleDeserializer.fileDeserializers().associate {
                    KotlinSourceFile(it.file) to it.symbolDeserializer.signatureDeserializer.signatureToIndexMapping()
                }
                artifacts += incrementalCache.buildModuleArtifactAndCommitCache(
                    moduleName = libFragment.name.asString(),
                    rebuiltFileFragments = libRebuiltFiles,
                    signatureToIndexMapping = signatureToIndexMapping
                )
            }
        }

        addArtifact(mainLibrary)

        return artifacts
    }

    private fun loadModifiedFiles(): KotlinSourceFileMap<KotlinSourceFileMetadata> {
        return KotlinSourceFileMap(incrementalCaches.entries.associate { (lib, cache) ->
            val (dirtyFiles, newFiles, removedFiles) = cache.collectModifiedFiles(configHash)

            if (dirtyFiles.isNotEmpty() || newFiles.isNotEmpty() || removedFiles.isNotEmpty()) {
                val fileStats = dirtyFileStats.getOrPutFiles(lib)
                val isCleanLoad = newFiles.size == dirtyFiles.size && removedFiles.isEmpty()
                if (!isCleanLoad) {
                    newFiles.forEach { fileStats.updateDirtFileStat(it, setNewAdded = true) }
                    dirtyFiles.forEach { fileStats.updateDirtFileStat(it.key, setModified = true) }
                    removedFiles.forEach { fileStats.updateDirtFileStat(it, setRemoved = true) }
                }
            }

            lib to dirtyFiles
        })
    }

    fun actualizeCaches(eventCallback: (String) -> Unit): List<ModuleArtifact> {
        dirtyFileStats.clear()

        val modifiedFiles = loadModifiedFiles()
        val dirtyFileExports = collectExportedSymbolsForDirtyFiles(modifiedFiles)

        val jsIrLinkerLoader = JsIrLinkerLoader(compilerConfiguration, mainLibrary, dependencyGraph, irFactory())
        var loadedIr = jsIrLinkerLoader.loadIr(dirtyFileExports)

        eventCallback("initial loading of updated files")

        var iterations = 0
        var lastDirtyFiles: KotlinSourceFileMap<KotlinSourceFileExports> = dirtyFileExports

        while (true) {
            hashCalculator.updateTransitiveHashes(loadedIr.loadedFragments.values)

            val dirtyHeaders = rebuildDirtySourceMetadata(loadedIr.linker, loadedIr.loadedFragments, lastDirtyFiles)

            val filesWithModifiedExportsOrImports = collectFilesWithModifiedExportsOrInlineImports(dirtyHeaders)

            val filesToRebuild = collectFilesToRebuildSignatures(filesWithModifiedExportsOrImports)

            eventCallback("actualization iteration $iterations")
            if (filesToRebuild.isEmpty()) {
                break
            }

            loadedIr = jsIrLinkerLoader.loadIr(filesToRebuild)
            iterations++

            lastDirtyFiles = filesToRebuild
            dirtyFileExports.copyFilesFrom(filesToRebuild)
        }

        if (iterations != 0) {
            loadedIr = jsIrLinkerLoader.loadIr(dirtyFileExports)
            eventCallback("final loading of updated files")
        }

        val rebuiltFragments = executor.execute(
            mainModule = loadedIr.loadedFragments[mainLibraryFile] ?: error("TODO error"),
            allModules = loadedIr.loadedFragments.values,
            deserializer = loadedIr.linker,
            configuration = compilerConfiguration,
            dirtyFiles = loadedIr.loadedFragments.flatMap { (libFile, libFragment) ->
                dirtyFileExports[libFile]?.let { libDirtyFiles ->
                    libFragment.files.filter { file -> KotlinSourceFile(file) in libDirtyFiles }
                } ?: emptyList()
            },
            exportedDeclarations = emptySet(),
            mainArguments = mainArguments
        )
        eventCallback("updated files processing (lowering)")

        val artifacts = buildModuleArtifactsAndCommitCache(loadedIr.linker, loadedIr.loadedFragments, rebuiltFragments)
        eventCallback("cache committing")

        return artifacts
    }
}


// Used for tests only
//fun rebuildCacheForDirtyFiles2(
//    library: KotlinLibrary,
//    configuration: CompilerConfiguration,
//    dependencyGraph: Map<KotlinLibrary, List<KotlinLibrary>>,
//    dirtyFiles: Collection<String>?,
//    artifactCache: ArtifactCache,
//    irFactory: IrFactory,
//    exportedDeclarations: Set<FqName>,
//    mainArguments: List<String>?,
//): String {
//    val jsIrLinkerProcessor = JsIrLinkerLoader(configuration, library, dependencyGraph, irFactory)
//    val (jsIrLinker, currentIrModule, irModules) = jsIrLinkerProcessor.processJsIrLinker(dirtyFiles)
//
//    buildCacheForModuleFiles(
//        currentIrModule, irModules, jsIrLinker, configuration, dirtyFiles, artifactCache, exportedDeclarations, mainArguments
//    )
//    return currentIrModule.name.asString()
//}

fun buildCacheForModuleFiles(
    currentModule: IrModuleFragment,
    allModules: Collection<IrModuleFragment>,
    deserializer: JsIrLinker,
    configuration: CompilerConfiguration,
    dirtyFiles: Collection<IrFile>,
    exportedDeclarations: Set<FqName>,
    mainArguments: List<String>?
): List<JsIrFragmentAndBinaryAst> {
    return compileWithIC(
        currentModule,
        allModules = allModules,
        filesToLower = dirtyFiles,
        configuration = configuration,
        deserializer = deserializer,
        mainArguments = mainArguments,
        exportedDeclarations = exportedDeclarations,
    )
}
