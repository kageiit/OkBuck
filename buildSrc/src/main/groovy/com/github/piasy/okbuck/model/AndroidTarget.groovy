package com.github.piasy.okbuck.model

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.tasks.InvokeManifestMerger
import com.android.builder.model.ClassField
import com.android.builder.model.SourceProvider
import com.github.piasy.okbuck.dependency.DependencyCache
import com.github.piasy.okbuck.util.FileUtil
import groovy.transform.ToString
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException

/**
 * An Android target
 */
abstract class AndroidTarget extends Target {

    final String applicationId
    final String versionName
    final Integer versionCode
    final int minSdk
    final int targetSdk
    final String manifest

    AndroidTarget(Project project, String name) {
        super(project, name)

        applicationId = baseVariant.applicationId
        versionName = baseVariant.mergedFlavor.versionName
        versionCode = baseVariant.mergedFlavor.versionCode
        minSdk = baseVariant.mergedFlavor.minSdkVersion.apiLevel
        targetSdk = baseVariant.mergedFlavor.targetSdkVersion.apiLevel
        manifest = extractMergedManifest()
    }

    protected abstract BaseVariant getBaseVariant()

    @Override
    protected Set<File> sourceDirs() {
        return baseVariant.sourceSets.collect { SourceProvider provider ->
            provider.javaDirectories
        }.flatten() as Set<File>
    }

    @Override
    protected Set<String> compileConfigurations() {
        return ["compile", "${buildType}Compile", "${flavor}Compile", "${name}Compile"]
    }

    List<String> getBuildConfigFields() {
        return ["String BUILD_TYPE = \"${buildType}\"",
                "String FLAVOR = \"${flavor}\"",
        ].plus(baseVariant.mergedFlavor.buildConfigFields.collect {
            String key, ClassField classField ->
                "${classField.type} ${key} = ${classField.value}"
        })
    }

    String getFlavor() {
        return baseVariant.flavorName
    }

    String getBuildType() {
        return baseVariant.buildType.name
    }

    Set<ResBundle> getResources() {
        Set<String> resources = [] as Set
        Set<String> assets = [] as Set

        baseVariant.sourceSets.each { SourceProvider provider ->
            resources.addAll(getAvailable(provider.resDirectories))
            assets.addAll(getAvailable(provider.assetsDirectories))
        }

        Map<String, String> resourceMap = resources.collectEntries { String res ->
            [project.file(res).parentFile.path, res]
        }
        Map<String, String> assetMap = assets.collectEntries { String asset ->
            [project.file(asset).parentFile.path, asset]
        }

        return resourceMap.keySet().plus(assetMap.keySet()).collect { key ->
            new ResBundle(identifier, resourceMap.get(key, null), assetMap.get(key, null))
        } as Set
    }

    Set<String> getAidl() {
        baseVariant.sourceSets.collect { SourceProvider provider ->
            getAvailable(provider.aidlDirectories)
        }.flatten() as Set<String>
    }

    Set<String> getJniLibs() {
        baseVariant.sourceSets.collect { SourceProvider provider ->
            getAvailable(provider.jniLibsDirectories)
        }.flatten() as Set<String>
    }

    protected String extractMergedManifest() {
        Set<String> manifests = [] as Set

        baseVariant.sourceSets.each { SourceProvider provider ->
            manifests.addAll(getAvailable(Collections.singletonList(provider.manifestFile)))
        }

        if (manifests.empty) {
            return null
        }

        File mainManifest = project.file(manifests[manifests.size() - 1])

        List<File> secondaryManifests = []
        secondaryManifests.addAll(manifests.collect {
            String manifestFile -> project.file(manifestFile)
        })
        secondaryManifests.remove(mainManifest)

        File mergedManifest = project.file("${project.buildDir}/okbuck/${name}/AndroidManifest.xml")
        mergedManifest.parentFile.mkdirs()
        mergedManifest.createNewFile()
        mergedManifest.text = ""

        String manifestMergeTaskname = "okbuckMerge${name}Manifest"
        InvokeManifestMerger manifestMerger
        try {
            manifestMerger = (InvokeManifestMerger) project.tasks.getByName(manifestMergeTaskname)
        } catch (UnknownTaskException ignored) {
            manifestMerger = project.tasks.create("okbuckMerge${name}Manifest",
                    InvokeManifestMerger, {
                it.mainManifestFile = mainManifest;
                it.secondaryManifestFiles = secondaryManifests;
                it.outputFile = mergedManifest
            })
        }

        manifestMerger.doFullTaskAction()

        XmlSlurper slurper = new XmlSlurper()
        GPathResult manifestXml = slurper.parse(mergedManifest)

        try {
            manifestXml.@':versionCode' = versionCode.toString()
            manifestXml.@':versionName' = versionName
        } catch (Exception ignored) {
        }

        manifestXml.appendNode({
            'uses-sdk'(':minSdkVersion': new Integer(minSdk).toString(),
                    ':targetSdkVersion': new Integer(targetSdk).toString()) {}
        })

        def builder = new StreamingMarkupBuilder()
        builder.setUseDoubleQuotes(true)
        mergedManifest.text = builder.bind {
            mkp.yield manifestXml
        } as String

        mergedManifest.text = mergedManifest.text
                .replaceAll(":minSdkVersion", "android:minSdkVersion")
                .replaceAll(":targetSdkVersion", "android:targetSdkVersion")
                .replaceAll(":versionCode", "android:versionCode")
                .replaceAll(":versionName", "android:versionName")
                .replaceAll("<manifest ", '<manifest xmlns:android="http://schemas.android.com/apk/res/android" ')

        return FileUtil.getRelativePath(project.projectDir, mergedManifest)
    }

    @ToString(includeNames = true)
    static class ResBundle {

        String id
        String resDir
        String assetsDir

        ResBundle(String identifier, String resDir, String assetsDir) {
            this.resDir = resDir
            this.assetsDir = assetsDir
            id = DependencyCache.md5("${identifier}:${resDir}:${assetsDir}")
        }
    }

    @Override
    def getProp(Map map, defaultValue) {
        return map.get("${identifier}${name}", map.get("${identifier}${flavor}",
                map.get("${identifier}${buildType}", map.get(identifier, defaultValue))))
    }
}
