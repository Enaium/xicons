/*
 * Copyright (c) 2026 Enaium
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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import utility.IconsType
import utility.extractSvgViewBoxAttributes
import utility.flattenCommands
import utility.kotlinBuilder
import utility.svg
import utility.tessellateContours
import java.io.File

/**
 * Generates ImGui icon data: per-library Kotlin files under
 * `cn.enaium.xicons.imgui.icons.<lib>` exposing lazy [Icon] constants, plus a
 * collection object `cn.enaium.xicons.imgui.<Lib>Icons` with per-style
 * `all` lists. Path geometry is flattened to polygons at generation time.
 *
 * @author Enaium
 */
open class GenerateImguiTask : DefaultTask() {
    @TaskAction
    fun execute() {
        val icons = project.rootProject.layout.buildDirectory.get().asFile.toPath()
            .resolve("icons")
            .resolve("node_modules")
            .resolve("@sicons")

        val projectLib = project.name.removePrefix("xicons-imgui-")
        val onlyLibs = project.findProperty("xiconsImguiLibs")?.toString()
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf(projectLib)

        icons.toFile().listFiles()
            .filter { dir -> onlyLibs == null || dir.name in onlyLibs }
            .forEach { dir ->
                val generatedRoot = project.projectDir.resolve("build/generated/commonMain/kotlin")
                if (generatedRoot.exists()) {
                    generatedRoot.deleteRecursively()
                }
                val sourceDir = generatedRoot.resolve("cn/enaium/xicons/imgui/icons/${dir.name}")



                dir.listFiles()?.filter { file ->
                    file.extension == "svg" && !listOf("12", "16", "20", "28", "32", "48")
                        .any { size -> file.nameWithoutExtension.contains(size) }
                }?.forEach { svgFile ->
                    val svgName = svgFile.nameWithoutExtension.replace("24", "")
                    val type = IconsType.entries.find { svgName.endsWith(it.text) } ?: IconsType.DEFAULT
                    val iconName = svgName.removeSuffix(type.text)

                    generateIconFile(dir.name, iconName, svgFile, generatedRoot)
                }
            }
        }
    }

    private fun generateIconFile(libName: String, iconName: String, svgFile: File, generatedRoot: File) {
        val dataName = svgFile.nameWithoutExtension + "_data"
        val content = svgFile.readText()
        val viewBox = extractSvgViewBoxAttributes(content).firstOrNull() ?: "0 0 24 24"
        val parts = viewBox.split(" ")
        val vbW = parts[2].toFloat()
        val vbH = parts[3].toFloat()

        val shapes = svg(content)
        val iconDataClass = ClassName("cn.enaium.xicons.imgui", "IconData")
        val builder = kotlinBuilder("cn.enaium.xicons.imgui.icons.$libName", svgFile.nameWithoutExtension.replace("24", ""))
            .indent("    ")

        // Each shape is emitted as its own private function so the bytecode
        // for a single <clinit> stays under the JVM 64KB method limit even
        // for icons with many filled contours.
        val shapeFns = ArrayList<FunSpec>()
        shapes.forEachIndexed { si, shape ->
            val loops = flattenCommands(shape.commands, maxOf(vbW, vbH).toDouble())
            val body = CodeBlock.builder()
            body.add("return IconData.Shape(\n")
            body.indent()
            body.add("fill = %L,\n", shape.fill)
            body.add("stroke = %L,\n", shape.stroke)
            body.add("strokeWidth = %Lf,\n", shape.strokeWidth.toFloat())
            body.add("fillOpacity = %Lf,\n", shape.fillOpacity.toFloat())
            if (shape.fill) {
                // Pre-tessellate filled contours with the even-odd rule so the
                // runtime only emits triangles (holes, concave outlines and
                // nested contours all work).
                val allPts = loops.flatten()
                if (allPts.isNotEmpty()) {
                    val minX = allPts.minOf { it[0] }; val maxX = allPts.maxOf { it[0] }
                    val minY = allPts.minOf { it[1] }; val maxY = allPts.maxOf { it[1] }
                    val tris = tessellateContours(loops, doubleArrayOf(minX, minY, maxX, maxY), 24, 24)
                    body.add("triangles = floatArrayOf(\n")
                    body.indent()
                    tris.forEach { t ->
                        body.add("%Lf, %Lf, %Lf, %Lf, %Lf, %Lf,\n", t[0], t[1], t[2], t[3], t[4], t[5])
                    }
                    body.unindent()
                    body.add("),\n")
                } else {
                    body.add("triangles = FloatArray(0),\n")
                }
            }
            // Fill-only shapes carry the whole geometry in [triangles];
            // [loops] are only needed for stroke rendering (and fill shapes
            // that also stroke, e.g. outlined icons).
            val needsLoops = shape.stroke || !shape.fill
            if (needsLoops) {
                body.add("loops = listOf(\n")
                body.indent()
                loops.forEach { loop ->
                    body.add("listOf(\n")
                    body.indent()
                    loop.forEach { p ->
                        body.add("%Lf to %Lf,\n", p[0].toFloat(), p[1].toFloat())
                    }
                    body.unindent()
                    body.add("),\n")
                }
                body.unindent()
                body.add("),\n")
            }
            body.unindent()
            body.add(")")
            val fn = FunSpec.builder("shape_${si}")
                .returns(iconDataClass.nestedClass("Shape"))
                .addCode("return " + body.build().toString())
                .addModifiers(KModifier.PRIVATE)
                .build()
            shapeFns.add(fn)
        }

        val shapeCode = CodeBlock.builder()
        shapeCode.add("IconData(\n")
        shapeCode.indent()
        shapeCode.add("viewBoxWidth = %Lf,\n", vbW)
        shapeCode.add("viewBoxHeight = %Lf,\n", vbH)
        shapeCode.add("shapes = listOf(\n")
        shapeCode.indent()
        shapes.indices.forEach { si ->
            shapeCode.add("shape_%L(),\n", si)
        }
        shapeCode.unindent()
        shapeCode.add("),\n")
        shapeCode.unindent()
        shapeCode.add(")")

        shapeFns.forEach { fn ->
            builder.addFunction(fn)
        }
        builder.addProperty(
            PropertySpec.builder(dataName, iconDataClass, KModifier.INTERNAL)
                .initializer(shapeCode.build())
                .build()
        )

        val iconClass = ClassName("cn.enaium.xicons.imgui", "Icon")
        val propertyName = svgFile.nameWithoutExtension.replace("24", "")
        builder.addProperty(
            PropertySpec.builder(propertyName, iconClass, KModifier.PUBLIC)
                .initializer("%T.fromData($dataName)", iconClass)
                .build()
        )

        builder.build().writeTo(generatedRoot)
    }

