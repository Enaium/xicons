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

package task

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import utility.IconsType
import utility.extractSvgViewBoxAttributes
import utility.kotlinBuilder
import utility.svg
import java.io.File

/**
 * @author Enaium
 */
open class GenerateComposeTask : DefaultTask() {
    @TaskAction
    fun execute() {
        val icons = project.rootProject.layout.buildDirectory.get().asFile.toPath()
            .resolve("icons")
            .resolve("node_modules")
            .resolve("@sicons")

        icons.toFile().listFiles().forEach { dir ->
            val iconNames = mutableListOf<String>()
            val sourceDir = project.projectDir.resolve("xicons-compose-${dir.name}/build/generated/commonMain/kotlin")
            if (sourceDir.exists()) {
                sourceDir.deleteRecursively()
            }

            val generatedTypes = mutableSetOf<IconsType>()
            val iconsByType = mutableMapOf<IconsType, MutableList<Pair<String, String>>>()

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
                val svgName = svg.nameWithoutExtension.replace("24", "")
                iconNames.add(svgName)

                val type = IconsType.entries.find { svgName.endsWith(it.text) } ?: IconsType.DEFAULT
                generatedTypes.add(type)

                val propertyName = svgName.removeSuffix(type.text)
                iconsByType.getOrPut(type) { mutableListOf() }.add(propertyName to propertyName)

                generateIconFile(dir, svg, svgName, sourceDir)
            }

            generateIconsCollection(dir.name, sourceDir, generatedTypes, iconsByType)
        }
    }

    private fun generateIconFile(dir: File, svg: File, svgName: String, sourceDir: File) {
        val content = svg.readText()
        val viewBox = extractSvgViewBoxAttributes(content).firstOrNull() ?: "0 0 24 24"

        val iconBuilder = kotlinBuilder("cn.enaium.xicons.compose.icons.${dir.name}", svgName)
            .indent("    ")
            .addImport("androidx.compose.ui.graphics", "SolidColor")
            .addImport("androidx.compose.ui.graphics.vector", "ImageVector", "group", "path")
            .addImport("androidx.compose.ui.unit", "dp")
            .addImport("androidx.compose.ui.graphics", "PathFillType")
            .addImport("androidx.compose.ui.graphics", "StrokeCap")
            .addImport("androidx.compose.ui.graphics", "StrokeJoin")

        val type = IconsType.entries.find { iconsType -> svgName.endsWith(iconsType.text) } ?: IconsType.DEFAULT
        val svg = svgName.removeSuffix(type.text)

        val imageVector = ClassName("androidx.compose.ui.graphics.vector", "ImageVector")
        iconBuilder.addProperty(
            PropertySpec.builder(svg, imageVector)
                .receiver(
                    ClassName.bestGuess(
                        "cn.enaium.xicons.compose.icons.${dir.name.uppercaseFirstChar()}Icons."
                                + type.text
                    )
                )
                .addModifiers(KModifier.PUBLIC)
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addCode(buildImageVectorInitializer(svg, content, viewBox))
                        .build()
                )
                .build()
        )
        iconBuilder.addProperty(
            PropertySpec.builder("_${svg}", imageVector.copy(nullable = true))
                .mutable(true)
                .addModifiers(KModifier.PRIVATE)
                .initializer("null")
                .build()
        )

        val file = iconBuilder.build()
        file.writeTo(sourceDir)
    }

    private fun buildImageVectorInitializer(svgName: String, content: String, viewBox: String): CodeBlock {
        val viewBoxParts = viewBox.split(" ")
        val width = viewBoxParts[2].toDouble()
        val height = viewBoxParts[3].toDouble()

        val needsScaling = width > 24
        val scale = if (needsScaling) 24.0 / width else 1.0
        val translateY = (24.0 - height * scale) / 2.0

        val builder = CodeBlock.builder()
            .add("if (_${svgName} != null) {\n")
            .indent()
            .add("return _${svgName}!!\n")
            .unindent()
            .add("}\n")
            .add("_${svgName} = ImageVector.Builder(\n")
            .indent()
            .add("name = %S,\n", svgName)
            .add("defaultWidth = 24.0.dp,\n")
            .add("defaultHeight = 24.0.dp,\n")
            .add("viewportWidth = 24.0f,\n")
            .add("viewportHeight = 24.0f\n")
            .unindent()
            .add(").apply {\n")
            .indent()

        if (needsScaling) {
            builder.add("group(scaleX = %Lf, scaleY = %Lf, translationY = %Lf) {\n", scale, scale, translateY)
            builder.indent()
        }

        svg(content, true).forEach { shape ->
            val params = mutableListOf<String>()
            val fillColor = if (shape.fill) {
                if (shape.fillOpacity < 1f) {
                    "SolidColor(androidx.compose.ui.graphics.Color.Black.copy(alpha = ${shape.fillOpacity}f))"
                } else {
                    "SolidColor(androidx.compose.ui.graphics.Color.Black)"
                }
            } else "null"
            params.add("fill = $fillColor")
            params.add("stroke = " + if (shape.stroke) "SolidColor(androidx.compose.ui.graphics.Color.Black)" else "null")
            params.add("strokeLineWidth = ${shape.strokeWidth}f")
            params.add("strokeLineCap = StrokeCap.${(shape.strokeLineCap ?: "butt").replaceFirstChar { it.uppercase() }}")
            params.add("strokeLineJoin = StrokeJoin.${(shape.strokeLineJoin ?: "miter").replaceFirstChar { it.uppercase() }}")
            params.add("pathFillType = " + if (shape.evenOdd) "PathFillType.EvenOdd" else "PathFillType.NonZero")

            builder.add("path(\n")
            builder.indent()
            params.forEachIndexed { index, param ->
                builder.add("%L", param)
                if (index < params.size - 1) builder.add(",\n") else builder.add("\n")
            }
            builder.unindent()
            builder.add(") {\n")
            builder.indent()
            shape.commands.forEach { command ->
                builder.addStatement(command)
            }
            builder.unindent()
            builder.add("}\n")
        }

        if (needsScaling) {
            builder.unindent()
            builder.add("}\n")
        }

        builder.unindent()
        builder.add("}.build()\n")
        builder.add("return _${svgName}!!")

        return builder.build()
    }


    private fun generateIconsCollection(
        dirName: String,
        packageDir: File,
        generatedTypes: Set<IconsType>,
        iconsByType: Map<IconsType, List<Pair<String, String>>>
    ) {
        val collectionBuilder = kotlinBuilder("cn.enaium.xicons.compose.icons", "${dirName.uppercaseFirstChar()}Icons")
            .addImport("androidx.compose.ui.graphics.vector", "ImageVector")

        val mainObject = TypeSpec.objectBuilder("${dirName.uppercaseFirstChar()}Icons")
            .addModifiers(KModifier.PUBLIC)

        // Style grouping objects (Filled/Outlined/...) are kept purely as
        // namespaces for the icon properties; no `all` lists are generated,
        // so unused ImageVectors can be pruned by the compiler/shrinker.
        generatedTypes.forEach { type ->
            val icons = iconsByType[type].orEmpty()
            val subObject = TypeSpec.objectBuilder(type.text)
                .addModifiers(KModifier.PUBLIC)
            icons.forEach { (name, _) ->
                collectionBuilder.addImport("cn.enaium.xicons.compose.icons.$dirName", name)
            }
            mainObject.addType(subObject.build())
        }

        collectionBuilder.addType(mainObject.build())

        val collectionFile = collectionBuilder.build()

        collectionFile.writeTo(packageDir)
    }
}
