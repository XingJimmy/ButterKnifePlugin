package com.sdu.didi.butterknifeplugin

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec

import javax.lang.model.element.Modifier

class FinalRClassBuilder {

    private final String ANNOTATION_PACKAGE = "androidx.annotation"
    private def SUPPORTED_TYPES = ["anim", "array", "attr", "bool", "color", "dimen",
                                         "drawable", "id", "integer", "layout", "menu", "plurals", "string", "style", "styleable"]

    def packageName
    def className

    private def resourceTypes = new HashMap<String, TypeSpec.Builder>()

    FinalRClassBuilder(packageName, className) {
        this.packageName = packageName
        this.className = className
    }

    JavaFile build() {
        def result = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        SUPPORTED_TYPES.each { type ->
            resourceTypes.get(type).with { builder ->
                if (builder != null) {
                    result.addType(builder.build())
                }
            }
        }
        return new JavaFile.Builder(packageName, result.build())
                .addFileComment("Generated code from Butter Knife gradle plugin. Do not modify!")
                .build()
    }

    private addResourceField(String type, String fieldName, String fieldValue) {
        if (!type in SUPPORTED_TYPES) {
            return
        }
        def fieldSpecBuilder = new FieldSpec.Builder(TypeName.INT, fieldName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(fieldValue)

        fieldSpecBuilder.addAnnotation(getSupportAnnotationClass(type))

        def resourceType = resourceTypes.get(type)
        if (!resourceType) {
            resourceType = TypeSpec.classBuilder(type).addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            resourceTypes.put(type, resourceType)
        }
        resourceType.addField(fieldSpecBuilder.build())
    }

    private ClassName getSupportAnnotationClass(String type) {
        return ClassName.get(ANNOTATION_PACKAGE, capitalize(type, Locale.US) + "Res")
    }

    // TODO https://youtrack.jetbrains.com/issue/KT-28933
    private String capitalize(String src, Locale locale) {
        if (src && src.length() > 1) {
            return src.substring(0, 1).toUpperCase(locale) + src.substring(1)
        } else {
            return src
        }
    }

    FinalRClassBuilder readSymbolTable(File symbolTable) {
        println("readSymbolTable " + symbolTable)
        if (symbolTable && symbolTable.exists()) {
            println("symbolTable " + symbolTable.getAbsolutePath())
            symbolTable.eachLine {
                processLine(it)
            }
        }
        return this
    }

    private processLine(String line) {
        println("line " + line)
        def values = line.split(' ')
        if (values.length < 4) {
            return
        }
        def javaType = values[0]
        if (!"int".equals(javaType)) {
            return
        }
        def symbolType = values[1]
        if (!symbolType in SUPPORTED_TYPES) {
            return
        }
        def name = values[2]
        def value = values[3]
        addResourceField(symbolType, name, value)
    }
}
