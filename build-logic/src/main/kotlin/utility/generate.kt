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
import com.palantir.javapoet.CodeBlock
import com.palantir.javapoet.FieldSpec
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.ParameterizedTypeName
import com.palantir.javapoet.TypeSpec
import org.gradle.api.Project
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.util.ArrayList
import javax.lang.model.element.Modifier

/**
 * @author Enaium
 */
fun Project.generateJava(
    extendPath: ClassName,
    pathIcon: ClassName,
    packageName: String,
    scaleStrokeWidth: Boolean
) {
    val icons =
        project.rootProject.layout.buildDirectory.get().asFile.toPath()
            .resolve("icons")
            .resolve("node_modules")
            .resolve("@sicons")


    icons.toFile().listFiles().forEach { dir ->
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

            val content = svg.readText()
            val viewBox = extractSvgViewBoxAttributes(content).firstOrNull()?.split(" ")
            val scale = viewBox?.let { parts ->
                val width = parts[2].toDouble()
                if (width > 24) 24.0 / width else 1.0
            } ?: 1.0
            val translateY = viewBox?.let { parts ->
                val height = parts[3].toDouble()
                (24.0 - height * scale) / 2.0
            } ?: 0.0
            val shapes = svg(content)

            val listType = ParameterizedTypeName.get(ClassName.get(List::class.java), extendPath)

            val pathsMethod = MethodSpec
                .methodBuilder("paths")
                .addModifiers(Modifier.PUBLIC)
                .returns(listType)

            val pathMethod = MethodSpec
                .methodBuilder("path")
                .addModifiers(Modifier.PUBLIC)
                .returns(extendPath)
                .addStatement("return paths().get(0)")

            val arrayListType = ParameterizedTypeName.get(ClassName.get(ArrayList::class.java), extendPath)
            val block = CodeBlock.builder()
            block.addStatement($$"$T paths = new $T()", listType, arrayListType)
            shapes.forEachIndexed { index, shape ->
                block.addStatement($$"$T path$L = new $T()", extendPath, index, extendPath)
                if (shape.evenOdd) {
                    block.addStatement($$"path$L.evenOdd()", index)
                }
                if (shape.fill && shape.fillOpacity < 1.0) {
                    block.addStatement($$"path$L.setFillOpacity($L)", index, shape.fillOpacity)
                }
                shape.commands.forEach {
                    block.addStatement("path${index}.${it}")
                }
                if (!shape.fill) {
                    block.addStatement("path${index}.setFillEnabled(false)")
                }
                if (shape.stroke) {
                    val width = if (scaleStrokeWidth) shape.strokeWidth * scale else shape.strokeWidth
                    block.addStatement($$"path$L.setStrokeWidth($L)", index, width)
                    shape.strokeLineCap?.let { block.addStatement($$"path$L.setStrokeLineCap($S)", index, it) }
                    shape.strokeLineJoin?.let { block.addStatement($$"path$L.setStrokeLineJoin($S)", index, it) }
                }
                if (scale != 1.0) {
                    block.addStatement($$"path$L.scale($L, $L)", index, scale, scale)
                }
                if (translateY != 0.0) {
                    block.addStatement($$"path$L.translate(0.0, $L)", index, translateY)
                }
                block.addStatement($$"paths.add(path$L)", index)
            }
            block.addStatement("return paths")

            pathsMethod.addCode(block.build())

            val svgName = svg.nameWithoutExtension.replace("24", "")
            val type = TypeSpec.classBuilder(svgName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(pathIcon)
                .addMethod(pathMethod.build())
                .addMethod(pathsMethod.build())
                .build()

            val file = javaBuilder("${packageName}.${dir.name}", type).build()
            file.writeTo(sourceDir)
        }

        // No collection/factory classes are generated: each icon is its own
        // standalone class (cn.enaium.xicons.swing.icons.<lib>.<Name>), so
        // unused icons can be pruned by the linker/shrinker. Callers that
        // need to enumerate icons scan the classpath at runtime.
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
