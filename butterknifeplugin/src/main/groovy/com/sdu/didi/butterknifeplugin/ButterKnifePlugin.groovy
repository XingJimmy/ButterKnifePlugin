package com.sdu.didi.butterknifeplugin

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.internal.res.GenerateLibraryRFileTask
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.concurrent.atomic.AtomicBoolean

class ButterKnifePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (project.plugins.hasPlugin(LibraryPlugin)) {
            project.extensions.getByType(LibraryExtension).with {
                configGenerateR2(project, libraryVariants)
            }
        }
    }

    void configGenerateR2(Project project, DomainObjectSet<BaseVariant> variants) {
        variants.all { BaseVariant variant ->
            def outputDir = new File(project.buildDir.getAbsolutePath() + "/generated/source/r2/${variant.dirName}")

            def rPackage = getPackageName(variant)
            def once = new AtomicBoolean()

            variant.outputs.all { BaseVariantOutput output ->

                def processResources = output.processResourcesProvider.get()

                // Though there might be multiple outputs, their R files are all the same. Thus, we only
                // need to configure the task once with the R.java input and action.
                if (once.compareAndSet(false, true)) {
                    def tempFile
                    if (processResources instanceof GenerateLibraryRFileTask) {
                        tempFile = ((GenerateLibraryRFileTask)processResources).textSymbolOutputFile
                    } else if (processResources instanceof LinkApplicationAndroidResourcesTask) {
                        tempFile = ((LinkApplicationAndroidResourcesTask)processResources).textSymbolOutputFile
                    } else {
                        throw RuntimeException("Minimum supported Android Gradle Plugin is 3.1.0")
                    }
                    def rFile = project.files(tempFile).builtBy(processResources)

                    println("outputDir " + outputDir)
                    println("rFile " + rFile)
                    // task to generate R2.java
                    def task = project.tasks.create("generate${variant.name.capitalize()}R2", R2Generator
                            , outputDir, rFile, rPackage, "R2")
                    // set R2.java to compile
                    variant.registerJavaGeneratingTask(task, outputDir)
                    // dependson ProcessAndroidResources Task
                    task.dependsOn(processResources)
                }
            }
        }
    }

    // Parse the variant's main manifest file in order to get the package id which is used to create
    // R.java in the right place.
    private String getPackageName(BaseVariant variant) {
        def slurper = new XmlSlurper(false, false)
        def list = []
        variant.sourceSets.each {
            list.add(it.manifestFile)
        }
        // According to the documentation, the earlier files in the list are meant to be overridden by the later ones.
        // So the first file in the sourceSets list should be main.
        def result = slurper.parse(list[0])
        return result.getProperty("@package").toString()
    }

}