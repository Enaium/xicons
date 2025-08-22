/*
 * Copyright (c) 2025 Enaium
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package utility

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.FieldSpec
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeSpec
import org.gradle.api.Project
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import javax.lang.model.element.Modifier

/**
 * @author Enaium
 */
fun Project.generateJava(extendPath: ClassName, pathIcon: ClassName, packageName: String) {
    val icons =
        project.rootProject.layout.buildDirectory.get().asFile.toPath()
            .resolve("icons")
            .resolve("node_modules")
            .resolve("@sicons")


    icons.toFile().listFiles().forEach { dir ->
        val iconNames = mutableListOf<String>()
        val sourceDir = project.projectDir.resolve("build/generated/${dir.name}/java")
        if (sourceDir.exists()) {
            sourceDir.deleteRecursively()
        }
        dir.listFiles()?.filter { file ->
            file.extension == "svg" && !listOf(
                "12",
                "16",
                "20",
                "28",
                "32",
                "48"
            ).any { size -> file.nameWithoutExtension.contains(size) }
        }?.forEach { svg ->

            val method = MethodSpec
                .methodBuilder("path")
                .addModifiers(Modifier.PUBLIC)
                .returns(extendPath)
                .addStatement($$"$T path = new $T()", extendPath, extendPath)

            val content = svg.readText()
            svg(content).forEach {
                method.addStatement("path.${it}")
            }

            extractSvgViewBoxAttributes(content).forEach {
                val width = it.split(" ")[2]
                if (width.toInt() > 24) {
                    val scale = 24.0 / width.toDouble()
                    method.addStatement($$"path.scale($L, $L)", scale, scale)
                }
            }

            method.addStatement("return path")

            val svgName = svg.nameWithoutExtension.replace("24", "")
            iconNames.add(svgName)
            val type = TypeSpec.classBuilder(svgName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(pathIcon)
                .addMethod(method.build())
                .build()

            val file = javaBuilder("${packageName}.${dir.name}", type).build()
            file.writeTo(sourceDir)
        }

        val emptyTypes = mutableSetOf<IconsType>()

        fun generateAllIcons(iconsType: IconsType) {
            val type = TypeSpec.classBuilder("${dir.name.uppercaseFirstChar()}${iconsType.text}Icons")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)

            iconNames.filter { name ->
                fun suffix(iconsType: IconsType): Boolean {
                    return name.endsWith(iconsType.text)
                }
                when (iconsType) {
                    IconsType.DEFAULT -> IconsType.entries.filter { it != IconsType.DEFAULT }.all { !suffix(it) }

                    IconsType.REGULAR,
                    IconsType.FILLED,
                    IconsType.OUTLINED,
                    IconsType.ROUND,
                    IconsType.SHARP,
                    IconsType.TWOTONE -> suffix(iconsType)
                }
            }.forEach { name ->
                val icon = ClassName.get("${packageName}.${dir.name}", name)
                type.addField(
                    FieldSpec.builder(icon, name.let { name ->
                        val suffixes = IconsType.entries.filter { it != IconsType.DEFAULT }.map { it.text }
                        suffixes.find { suffix -> name.endsWith(suffix) }?.let { name.dropLast(it.length) } ?: name
                    }).addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .initializer($$"new $T()", icon)
                        .build()
                )
            }

            type.build().takeIf { it.fieldSpecs().isNotEmpty() }?.also { type ->
                val file = javaBuilder(packageName, type).build()

                file.writeTo(sourceDir)
            } ?: emptyTypes.add(iconsType)
        }

        IconsType.entries.forEach {
            generateAllIcons(it)
        }

        val type = TypeSpec.classBuilder("${dir.name.uppercaseFirstChar()}Icons")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)

        IconsType.entries.filter { it !in emptyTypes }.forEach {
            val iconType =
                ClassName.get(packageName, "${dir.name.uppercaseFirstChar()}${it.text}Icons")
            type.addField(
                FieldSpec.builder(
                    iconType,
                    it.text
                ).addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer($$"new $T()", iconType)
                    .build()
            )
        }
        val file = javaBuilder(packageName, type.build()).build()

        file.writeTo(sourceDir)
    }
}

enum class IconsType(val text: String) {
    DEFAULT("Default"),
    REGULAR("Regular"),
    FILLED("Filled"),
    OUTLINED("Outlined"),
    ROUND("Round"),
    SHARP("Sharp"),
    TWOTONE("Twotone")
}