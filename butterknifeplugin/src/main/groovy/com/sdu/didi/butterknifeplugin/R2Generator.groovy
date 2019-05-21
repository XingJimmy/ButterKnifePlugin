package com.sdu.didi.butterknifeplugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class R2Generator extends DefaultTask {

    File outputDir
    FileCollection rFile
    String packageName
    String className

    @Inject
    R2Generator(File outputDir, FileCollection rFile, String packageName, String className) {
        this.outputDir = outputDir
        this.rFile = rFile
        this.packageName = packageName
        this.className = className
    }

    @TaskAction
    void brewJava() {
        if (outputDir != null && rFile != null) {
            new FinalRClassBuilder(packageName, className)
                    .readSymbolTable(rFile.getSingleFile())
                    .build()
                    .writeTo(outputDir)
        }
    }
}