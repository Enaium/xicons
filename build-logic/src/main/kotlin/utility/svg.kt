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

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.util.*
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.*

/**
 * One drawable unit of an SVG icon: a filled and/or stroked path.
 *
 * @author Enaium
 */
class SvgShape(
    val commands: List<String>,
    val fill: Boolean,
    val stroke: Boolean,
    val strokeWidth: Double,
    val strokeLineCap: String?,
    val strokeLineJoin: String?,
    val evenOdd: Boolean,
    val fillOpacity: Double = 1.0
)

/**
 * Parse an SVG string into drawable shapes. All geometry is kept in the
 * original viewBox coordinate space; scaling is applied by callers.
 *
 * Shapes whose effective opacity is below [tintThreshold] are background
 * tint layers (e.g. antd/material twotone secondary layers) and are skipped
 * for monochrome rendering. If every shape would be skipped, all are kept.
 *
 * @author Enaium
 */
fun svg(content: String, f: Boolean = false, tintThreshold: Double = 0.5): List<SvgShape> {
    val document = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    }.newDocumentBuilder().parse(ByteArrayInputStream(content.toByteArray()))

    val shapes = mutableListOf<SvgShapeInternal>()
    walk(document.documentElement, Style(), shapes, f)

    // Keep every shape: multi-tone icons (antd twotone, material twotone)
    // carry secondary layers as low-opacity fills that must render as tints,
    // not be dropped. Callers that want monochrome rendering can apply the
    // threshold themselves.
    return shapes.map { it.asPublic }
}

/**
 * Extract the viewBox of an SVG document, e.g. "0 0 1024 1024".
 *
 * @author Enaium
 */
fun extractSvgViewBoxAttributes(svg: String): List<String> {
    val result = mutableListOf<String>()
    val p = Pattern.compile("<svg[^>]*\\sviewBox=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
    val m = p.matcher(svg)
    while (m.find()) result.add(m.group(1))
    return result
}

private data class SvgShapeInternal(
    val commands: List<String>,
    val fill: Boolean,
    val stroke: Boolean,
    val strokeWidth: Double,
    val strokeLineCap: String?,
    val strokeLineJoin: String?,
    val evenOdd: Boolean,
    val opacity: Double
)

private val SvgShapeInternal.asPublic: SvgShape
    get() = SvgShape(commands, fill, stroke, strokeWidth, strokeLineCap, strokeLineJoin, evenOdd, opacity)

private class Style(
    var fill: String? = null,
    var fillRule: String? = null,
    var fillOpacity: Double = 1.0,
    var opacity: Double = 1.0,
    var stroke: String? = null,
    var strokeWidth: Double = 1.0,
    var strokeLineCap: String? = null,
    var strokeLineJoin: String? = null,
    var transform: Matrix = Matrix.IDENTITY
) {
    fun effectiveOpacity(): Double = fillOpacity * opacity

    fun child(): Style = Style(
        fill, fillRule, fillOpacity, opacity, stroke, strokeWidth,
        strokeLineCap, strokeLineJoin, transform
    )
}

/** 2D affine matrix, applied as p' = M * p with row-vector layout. */
private class Matrix(
    val a: Double, val b: Double, val c: Double, val d: Double, val e: Double, val ff: Double
) {
    /** M * [x, y] */
    fun transform(x: Double, y: Double): DoubleArray =
        doubleArrayOf(a * x + c * y + e, b * x + d * y + ff)

    fun isIdentity(): Boolean = a == 1.0 && b == 0.0 && c == 0.0 && d == 1.0 && e == 0.0 && ff == 0.0

    /** this * other (apply other first, then this) */
    fun multiply(other: Matrix): Matrix = Matrix(
        a * other.a + c * other.b,
        b * other.a + d * other.b,
        a * other.c + c * other.d,
        b * other.c + d * other.d,
        a * other.e + c * other.ff + e,
        b * other.e + d * other.ff + ff
    )

    companion object {
        val IDENTITY = Matrix(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    }
}

private fun parseTransform(value: String?): Matrix {
    if (value.isNullOrBlank()) return Matrix.IDENTITY
    var m = Matrix.IDENTITY
    val p = Pattern.compile("(\\w+)\\s*\\(([^)]*)\\)")
    val matcher = p.matcher(value)
    while (matcher.find()) {
        val name = matcher.group(1)
        val args = matcher.group(2).split("[,\\s]+".toRegex()).filter { it.isNotBlank() }.map { it.toDouble() }
        val t = when (name) {
            "translate" -> if (args.size > 1) Matrix(1.0, 0.0, 0.0, 1.0, args[0], args[1])
            else Matrix(1.0, 0.0, 0.0, 1.0, args[0], 0.0)

            "scale" -> if (args.size > 1) Matrix(args[0], 0.0, 0.0, args[1], 0.0, 0.0)
            else Matrix(args[0], 0.0, 0.0, args[0], 0.0, 0.0)

            "rotate" -> {
                val rad = Math.toRadians(args[0])
                val cos = cos(rad)
                val sin = sin(rad)
                val rot = Matrix(cos, sin, -sin, cos, 0.0, 0.0)
                if (args.size > 2) {
                    // translate(cx,cy) * rotate * translate(-cx,-cy)
                    Matrix(1.0, 0.0, 0.0, 1.0, args[1], args[2])
                        .multiply(rot)
                        .multiply(Matrix(1.0, 0.0, 0.0, 1.0, -args[1], -args[2]))
                } else {
                    rot
                }
            }

            "skewX" -> Matrix(1.0, 0.0, tan(Math.toRadians(args[0])), 1.0, 0.0, 0.0)
            "skewY" -> Matrix(1.0, tan(Math.toRadians(args[0])), 0.0, 1.0, 0.0, 0.0)
            "matrix" -> Matrix(args[0], args[1], args[2], args[3], args[4], args[5])
            else -> Matrix.IDENTITY
        }
        // element transform applies inside the inherited (parent) transform
        m = m.multiply(t)
    }
    return m
}

private fun walk(node: Node, style: Style, shapes: MutableList<SvgShapeInternal>, f: Boolean) {
    if (node.nodeType != Node.ELEMENT_NODE) return
    val element = node as Element
    when (element.tagName) {
        "g", "svg" -> {
            val child = style.child()
            inherit(element, child)
            for (i in 0 until element.childNodes.length) {
                walk(element.childNodes.item(i), child, shapes, f)
            }
        }

        "path", "circle", "ellipse", "rect", "line", "polyline", "polygon" -> {
            val child = style.child()
            inherit(element, child)
            val shape = when (element.tagName) {
                "path" -> shapeFromPath(element, child, f)
                "circle" -> shapeFromCircle(element, child, f)
                "ellipse" -> shapeFromEllipse(element, child, f)
                "rect" -> shapeFromRect(element, child, f)
                "line" -> shapeFromLine(element, child, f)
                "polyline" -> shapeFromPoly(element, child, close = false, f)
                else -> shapeFromPoly(element, child, close = true, f)
            }
            shape?.let { shapes.add(it) }
        }
    }
}

private fun inherit(element: Element, style: Style) {
    style.fill = attr(element, "fill") ?: style.fill
    style.fillRule = attr(element, "fill-rule") ?: style.fillRule
    attr(element, "fill-opacity")?.toDoubleOrNull()?.let { style.fillOpacity = it }
    attr(element, "opacity")?.toDoubleOrNull()?.let { style.opacity = it }
    style.stroke = attr(element, "stroke") ?: style.stroke
    attr(element, "stroke-width")?.toDoubleOrNull()?.let { style.strokeWidth = it }
    style.strokeLineCap = attr(element, "stroke-linecap") ?: style.strokeLineCap
    style.strokeLineJoin = attr(element, "stroke-linejoin") ?: style.strokeLineJoin
    style.transform = style.transform.multiply(parseTransform(attr(element, "transform")))
}

private fun attr(element: Element, name: String): String? {
    val v = element.getAttribute(name)
    return v.ifEmpty { null }
}

private fun shapeFromPath(element: Element, style: Style, f: Boolean): SvgShapeInternal? {
    val d = attr(element, "d") ?: return null
    if (d.isBlank()) return null
    val commands = convertPathDataToJava(d, f, style.transform)
    return SvgShapeInternal(
        commands,
        fillEnabled(style),
        strokeEnabled(style),
        style.strokeWidth,
        style.strokeLineCap,
        style.strokeLineJoin,
        evenOdd(style),
        style.effectiveOpacity()
    )
}

private fun shapeFromCircle(element: Element, style: Style, f: Boolean): SvgShapeInternal? {
    val cx = attr(element, "cx")?.toDoubleOrNull() ?: 0.0
    val cy = attr(element, "cy")?.toDoubleOrNull() ?: 0.0
    val r = attr(element, "r")?.toDoubleOrNull() ?: return null
    return ellipseCommands(cx, cy, r, r, style, f)
}

private fun shapeFromEllipse(element: Element, style: Style, f: Boolean): SvgShapeInternal? {
    val cx = attr(element, "cx")?.toDoubleOrNull() ?: 0.0
    val cy = attr(element, "cy")?.toDoubleOrNull() ?: 0.0
    val rx = attr(element, "rx")?.toDoubleOrNull() ?: 0.0
    val ry = attr(element, "ry")?.toDoubleOrNull() ?: 0.0
    return ellipseCommands(cx, cy, rx, ry, style, f)
}

private fun ellipseCommands(
    cx: Double, cy: Double, rx: Double, ry: Double, style: Style, f: Boolean
): SvgShapeInternal? {
    if (rx <= 0 || ry <= 0) return null
    val k = 0.5522847498307936 // 4/3 * tan(pi/8)
    fun p(x: Double, y: Double): DoubleArray = style.transform.transform(x, y)
    val r = p(cx + rx, cy)
    val t = p(cx, cy - ry)
    val l = p(cx - rx, cy)
    val b = p(cx, cy + ry)
    val rt = p(cx + rx, cy - ry * k)
    val tr = p(cx + rx * k, cy - ry)
    val tl = p(cx - rx * k, cy - ry)
    val lt = p(cx - rx, cy - ry * k)
    val lb = p(cx - rx, cy + ry * k)
    val bl = p(cx - rx * k, cy + ry)
    val br = p(cx + rx * k, cy + ry)
    val rb = p(cx + rx, cy + ry * k)
    val cmds = mutableListOf<String>()
    cmds.add(fmtCmd("moveTo", r, f))
    cmds.add(fmtCmd("curveTo", doubleArrayOf(rt[0], rt[1], tr[0], tr[1], t[0], t[1]), f))
    cmds.add(fmtCmd("curveTo", doubleArrayOf(tl[0], tl[1], lt[0], lt[1], l[0], l[1]), f))
    cmds.add(fmtCmd("curveTo", doubleArrayOf(lb[0], lb[1], bl[0], bl[1], b[0], b[1]), f))
    cmds.add(fmtCmd("curveTo", doubleArrayOf(br[0], br[1], rb[0], rb[1], r[0], r[1]), f))
    cmds.add("close()")
    return SvgShapeInternal(
        cmds,
        fillEnabled(style),
        strokeEnabled(style),
        style.strokeWidth,
        style.strokeLineCap,
        style.strokeLineJoin,
        evenOdd(style),
        style.effectiveOpacity()
    )
}

private fun shapeFromRect(element: Element, style: Style, f: Boolean): SvgShapeInternal? {
    val x = attr(element, "x")?.toDoubleOrNull() ?: 0.0
    val y = attr(element, "y")?.toDoubleOrNull() ?: 0.0
    val w = attr(element, "width")?.toDoubleOrNull() ?: return null
    val h = attr(element, "height")?.toDoubleOrNull() ?: return null
    if (w <= 0 || h <= 0) return null
    var rx = attr(element, "rx")?.toDoubleOrNull() ?: 0.0
    var ry = attr(element, "ry")?.toDoubleOrNull() ?: 0.0
    fun p(x: Double, y: Double): DoubleArray = style.transform.transform(x, y)
    val cmds = mutableListOf<String>()
    if (rx == 0.0 && ry == 0.0) {
        cmds.add(fmtCmd("moveTo", p(x, y), f))
        cmds.add(fmtCmd("lineTo", p(x + w, y), f))
        cmds.add(fmtCmd("lineTo", p(x + w, y + h), f))
        cmds.add(fmtCmd("lineTo", p(x, y + h), f))
        cmds.add("close()")
    } else {
        rx = min(rx, w / 2)
        ry = min(ry, h / 2)
        if (rx == 0.0) rx = ry
        if (ry == 0.0) ry = rx
        cmds.add(fmtCmd("moveTo", p(x + rx, y), f))
        cmds.add(fmtCmd("lineTo", p(x + w - rx, y), f))
        cmds.add(fmtArc(p(x + w - rx, y), p(x + w, y + ry), rx, ry, 0.0, 1.0, f))
        cmds.add(fmtCmd("lineTo", p(x + w, y + h - ry), f))
        cmds.add(fmtArc(p(x + w, y + h - ry), p(x + w - rx, y + h), rx, ry, 0.0, 1.0, f))
        cmds.add(fmtCmd("lineTo", p(x + rx, y + h), f))
        cmds.add(fmtArc(p(x + rx, y + h), p(x, y + h - ry), rx, ry, 0.0, 1.0, f))
        cmds.add(fmtCmd("lineTo", p(x, y + ry), f))
        cmds.add(fmtArc(p(x, y + ry), p(x + rx, y), rx, ry, 0.0, 1.0, f))
        cmds.add("close()")
    }
    return SvgShapeInternal(
        cmds,
        fillEnabled(style),
        strokeEnabled(style),
        style.strokeWidth,
        style.strokeLineCap,
        style.strokeLineJoin,
        evenOdd(style),
        style.effectiveOpacity()
    )
}

private fun shapeFromLine(element: Element, style: Style, f: Boolean): SvgShapeInternal? {
    val x1 = attr(element, "x1")?.toDoubleOrNull() ?: 0.0
    val y1 = attr(element, "y1")?.toDoubleOrNull() ?: 0.0
    val x2 = attr(element, "x2")?.toDoubleOrNull() ?: 0.0
    val y2 = attr(element, "y2")?.toDoubleOrNull() ?: 0.0
    val p1 = style.transform.transform(x1, y1)
    val p2 = style.transform.transform(x2, y2)
    val cmds = mutableListOf<String>()
    cmds.add(fmtCmd("moveTo", p1, f))
    cmds.add(fmtCmd("lineTo", p2, f))
    return SvgShapeInternal(
        cmds,
        fillEnabled(style),
        strokeEnabled(style),
        style.strokeWidth,
        style.strokeLineCap,
        style.strokeLineJoin,
        evenOdd(style),
        style.effectiveOpacity()
    )
}

private fun shapeFromPoly(element: Element, style: Style, close: Boolean, f: Boolean): SvgShapeInternal? {
    val points = attr(element, "points") ?: return null
    val list = points.split("[,\\s]+".toRegex()).filter { it.isNotBlank() }.map { it.toDouble() }
    if (list.size < 4) return null
    val cmds = mutableListOf<String>()
    cmds.add(fmtCmd("moveTo", style.transform.transform(list[0], list[1]), f))
    var i = 2
    while (i + 1 < list.size) {
        cmds.add(fmtCmd("lineTo", style.transform.transform(list[i], list[i + 1]), f))
        i += 2
    }
    if (close) cmds.add("close()")
    return SvgShapeInternal(
        cmds,
        fillEnabled(style),
        strokeEnabled(style),
        style.strokeWidth,
        style.strokeLineCap,
        style.strokeLineJoin,
        evenOdd(style),
        style.effectiveOpacity()
    )
}

private fun fillEnabled(style: Style): Boolean {
    val fill = style.fill ?: return true
    return fill != "none"
}

private fun strokeEnabled(style: Style): Boolean {
    val stroke = style.stroke ?: return false
    return stroke != "none"
}

private fun evenOdd(style: Style): Boolean = style.fillRule == "evenodd"

private fun fmt(v: Double, f: Boolean): String {
    return String.format(Locale.US, "%.2f", v).let {
        if (f) {
            "${it}f"
        } else {
            it
        }
    }
}

private fun fmtCmd(name: String, nums: DoubleArray, f: Boolean): String {
    val formatted = nums.joinToString(", ") { fmt(it, f) }
    return "$name($formatted)"
}

private fun fmtArc(from: DoubleArray, to: DoubleArray, rx: Double, ry: Double, large: Double, sweep: Double, f: Boolean): String {
    return String.format(
        "arcTo(%s, %s, %s, %s, %s, %s, %s)",
        fmt(rx, f),
        fmt(ry, f),
        fmt(0.0, f),
        if (large != 0.0) "true" else "false",
        if (sweep != 0.0) "true" else "false",
        fmt(to[0], f),
        fmt(to[1], f)
    )
}

private fun convertPathDataToJava(d: String, f: Boolean = false, transform: Matrix = Matrix.IDENTITY): List<String> {
    val cursor = PathCursor(d)
    val lines = mutableListOf<String>()

    var currentX = 0.0
    var currentY = 0.0
    var subStartX = 0.0
    var subStartY = 0.0
    var lastC2X: Double? = null
    var lastC2Y: Double? = null
    var lastQX: Double? = null
    var lastQY: Double? = null
    var prev: Char? = null

    fun emitLineLike(name: String, x: Double, y: Double) {
        val t = transform.transform(x, y)
        lines.add(fmtCmd("lineTo", t, f))
    }

    fun emitCurve(x1: Double, y1: Double, x2: Double, y2: Double, x: Double, y: Double) {
        val c1 = transform.transform(x1, y1)
        val c2 = transform.transform(x2, y2)
        val p = transform.transform(x, y)
        lines.add(fmtCmd("curveTo", doubleArrayOf(c1[0], c1[1], c2[0], c2[1], p[0], p[1]), f))
    }

    fun emitQuad(x1: Double, y1: Double, x: Double, y: Double) {
        val c = transform.transform(x1, y1)
        val p = transform.transform(x, y)
        lines.add(fmtCmd("quadTo", doubleArrayOf(c[0], c[1], p[0], p[1]), f))
    }

    while (cursor.hasMore()) {
        when (val cmd = cursor.nextCommand(prev)) {
            'M', 'm' -> {
                val rel = cmd == 'm'
                var x = cursor.nextNumber()
                var y = cursor.nextNumber()
                if (rel) {
                    x += currentX
                    y += currentY
                }
                val t = transform.transform(x, y)
                lines.add(fmtCmd("moveTo", t, f))
                subStartX = x
                currentX = subStartX
                subStartY = y
                currentY = subStartY
                prev = 'M'
                lastQY = null
                lastQX = lastQY
                lastC2Y = lastQX
                lastC2X = lastC2Y
                while (cursor.hasNumberAhead()) {
                    x = cursor.nextNumber()
                    y = cursor.nextNumber()
                    if (rel) {
                        x += currentX
                        y += currentY
                    }
                    emitLineLike("lineTo", x, y)
                    currentX = x
                    currentY = y
                    prev = 'L'
                }
            }

            'L', 'l' -> {
                val rel = cmd == 'l'
                while (cursor.hasNumberAhead()) {
                    var x = cursor.nextNumber()
                    var y = cursor.nextNumber()
                    if (rel) {
                        x += currentX
                        y += currentY
                    }
                    emitLineLike("lineTo", x, y)
                    currentX = x
                    currentY = y
                }
                prev = 'L'
                lastQY = null
                lastQX = lastQY
                lastC2Y = lastQX
                lastC2X = lastC2Y
            }

            'H', 'h' -> {
                val rel = cmd == 'h'
                while (cursor.hasNumberAhead()) {
                    var x = cursor.nextNumber()
                    if (rel) x += currentX
                    if (transform.isIdentity()) {
                        lines.add(String.format("horizontalLineTo(%s)", fmt(x, f)))
                    } else {
                        emitLineLike("lineTo", x, currentY)
                    }
                    currentX = x
                }
                prev = 'H'
                lastQY = null
                lastQX = lastQY
                lastC2Y = lastQX
                lastC2X = lastC2Y
            }

            'V', 'v' -> {
                val rel = cmd == 'v'
                while (cursor.hasNumberAhead()) {
                    var y = cursor.nextNumber()
                    if (rel) y += currentY
                    if (transform.isIdentity()) {
                        lines.add(String.format("verticalLineTo(%s)", fmt(y, f)))
                    } else {
                        emitLineLike("lineTo", currentX, y)
                    }
                    currentY = y
                }
                prev = 'V'
                lastQY = null
                lastQX = lastQY
                lastC2Y = lastQX
                lastC2X = lastC2Y
            }

            'C', 'c' -> {
                val rel = cmd == 'c'
                while (cursor.hasNumberAhead()) {
                    var x1 = cursor.nextNumber()
                    var y1 = cursor.nextNumber()
                    var x2 = cursor.nextNumber()
                    var y2 = cursor.nextNumber()
                    var x = cursor.nextNumber()
                    var y = cursor.nextNumber()
                    if (rel) {
                        x1 += currentX
                        y1 += currentY
                        x2 += currentX
                        y2 += currentY
                        x += currentX
                        y += currentY
                    }
                    emitCurve(x1, y1, x2, y2, x, y)
                    currentX = x
                    currentY = y
                    lastC2X = x2
                    lastC2Y = y2
                    lastQY = null
                    lastQX = lastQY
                }
                prev = 'C'
            }

            'S', 's' -> {
                val rel = cmd == 's'
                while (cursor.hasNumberAhead()) {
                    val x1 =
                        if (prev != null && (prev == 'C' || prev == 'S') && lastC2X != null) 2 * currentX - lastC2X else currentX
                    val y1 =
                        if (prev != null && (prev == 'C' || prev == 'S') && lastC2Y != null) 2 * currentY - lastC2Y else currentY
                    var x2 = cursor.nextNumber()
                    var y2 = cursor.nextNumber()
                    var x = cursor.nextNumber()
                    var y = cursor.nextNumber()
                    if (rel) {
                        x2 += currentX
                        y2 += currentY
                        x += currentX
                        y += currentY
                    }
                    emitCurve(x1, y1, x2, y2, x, y)
                    currentX = x
                    currentY = y
                    lastC2X = x2
                    lastC2Y = y2
                    lastQY = null
                    lastQX = lastQY
                }
                prev = 'S'
            }

            'Q', 'q' -> {
                val rel = cmd == 'q'
                while (cursor.hasNumberAhead()) {
                    var x1 = cursor.nextNumber()
                    var y1 = cursor.nextNumber()
                    var x = cursor.nextNumber()
                    var y = cursor.nextNumber()
                    if (rel) {
                        x1 += currentX
                        y1 += currentY
                        x += currentX
                        y += currentY
                    }
                    emitQuad(x1, y1, x, y)
                    currentX = x
                    currentY = y
                    lastQX = x1
                    lastQY = y1
                    lastC2Y = null
                    lastC2X = lastC2Y
                }
                prev = 'Q'
            }

            'T', 't' -> {
                val rel = cmd == 't'
                while (cursor.hasNumberAhead()) {
                    val x1 =
                        if (prev != null && (prev == 'Q' || prev == 'T') && lastQX != null) 2 * currentX - lastQX else currentX
                    val y1 =
                        if (prev != null && (prev == 'Q' || prev == 'T') && lastQY != null) 2 * currentY - lastQY else currentY
                    var x = cursor.nextNumber()
                    var y = cursor.nextNumber()
                    if (rel) {
                        x += currentX
                        y += currentY
                    }
                    emitQuad(x1, y1, x, y)
                    currentX = x
                    currentY = y
                    lastQX = x1
                    lastQY = y1
                    lastC2Y = null
                    lastC2X = lastC2Y
                }
                prev = 'T'
            }

            'A', 'a' -> {
                val rel = cmd == 'a'
                while (cursor.hasNumberAhead()) {
                    val rx = cursor.nextNumber()
                    val ry = cursor.nextNumber()
                    val rot = cursor.nextNumber()
                    val large = cursor.nextNumber().toInt()
                    val sweep = cursor.nextNumber().toInt()
                    var x = cursor.nextNumber()
                    var y = cursor.nextNumber()
                    if (rel) {
                        x += currentX
                        y += currentY
                    }
                    val t = transform.transform(x, y)
                    lines.add(
                        String.format(
                            "arcTo(%s, %s, %s, %s, %s, %s, %s)",
                            fmt(rx, f),
                            fmt(ry, f),
                            fmt(rot, f),
                            large != 0,
                            sweep != 0,
                            fmt(t[0], f),
                            fmt(t[1], f)
                        )
                    )
                    currentX = x
                    currentY = y
                    lastQY = null
                    lastQX = lastQY
                    lastC2Y = lastQX
                    lastC2X = lastC2Y
                }
                prev = 'A'
            }

            'Z', 'z' -> {
                lines.add("close()")
                currentX = subStartX
                currentY = subStartY
                lastQY = null
                lastQX = lastQY
                lastC2Y = lastQX
                lastC2X = lastC2Y
                prev = 'Z'
            }

            else -> throw IllegalStateException("Unsupported command: " + cmd)
        }
    }
    return lines
}

private class PathCursor(private val s: String) {
    private var i = 0

    fun hasMore(): Boolean {
        skipWs()
        return i < s.length
    }

    fun hasNumberAhead(): Boolean {
        skipWs()
        if (i >= s.length) return false
        val c = s.get(i)
        return c == '+' || c == '-' || c == '.' || Character.isDigit(c)
    }

    fun nextCommand(prev: Char?): Char {
        skipWs()
        check(i < s.length) { "Unexpected end" }
        val c = s.get(i)
        if (isCommand(c)) {
            i++
            return c
        }
        if (prev != null) return prev
        throw IllegalStateException("Expected command, got: " + c)
    }

    fun nextNumber(): Double {
        skipWs()
        val start = i
        var dot = false
        var exp = false
        if (i < s.length && (s.get(i) == '+' || s.get(i) == '-')) i++
        while (i < s.length) {
            val c = s.get(i)
            if (Character.isDigit(c)) {
                i++
                continue
            }
            if (c == '.' && !dot) {
                dot = true
                i++
                continue
            }
            if ((c == 'e' || c == 'E') && !exp) {
                exp = true
                i++
                if (i < s.length && (s.get(i) == '+' || s.get(i) == '-')) i++
                continue
            }
            break
        }
        check(start != i) { "Number expected at " + i }
        return s.substring(start, i).toDouble()
    }

    fun skipWs() {
        while (i < s.length) {
            val c = s.get(i)
            if (Character.isWhitespace(c) || c == ',') i++
            else break
        }
    }

    fun isCommand(c: Char): Boolean {
        return when (c) {
            'M', 'm', 'L', 'l', 'H', 'h', 'V', 'v', 'C', 'c', 'S', 's', 'Q', 'q', 'T', 't', 'A', 'a', 'Z', 'z' -> true
            else -> false
        }
    }
}

/**
 * Flattens a list of path commands (as produced by [convertPathDataToJava])
 * into closed polygon loops of (x, y) points. Curves are subdivided until
 * flat; arcs are converted via their cubic approximation.
 *
 * [viewBoxSize] is the longest viewBox side; the flatness tolerance is
 * derived from it (0.2% of the viewBox) so icons of any size get curves
 * with similar visual smoothness.
 *
 * Returns a list of loops; each loop is a list of DoubleArray(x, y).
 * A loop is closed if it ends with "close()".
 *
 * @author Enaium
 */
fun flattenCommands(commands: List<String>, viewBoxSize: Double = 24.0): List<List<DoubleArray>> {
    val flatTolerance = viewBoxSize * 0.004
    val loops = mutableListOf<List<DoubleArray>>()
    var current = mutableListOf<DoubleArray>()
    var cx = 0.0
    var cy = 0.0
    var sx = 0.0
    var sy = 0.0
    var lastC2X: Double? = null
    var lastC2Y: Double? = null
    var lastQX: Double? = null
    var lastQY: Double? = null
    var prev: String? = null

    fun add(x: Double, y: Double) {
        current.add(doubleArrayOf(x, y))
    }

    fun closeLoop() {
        if (current.size >= 2) loops.add(current)
        current = mutableListOf()
        cx = sx; cy = sy
        lastC2X = null; lastC2Y = null; lastQX = null; lastQY = null
    }

    val numRe = Regex("[-+]?\\d+(?:\\.\\d+)?")
    for (cmd in commands) {
        val name = cmd.substringBefore('(')
        val args = numRe.findAll(cmd.substringAfter('(')).map { it.value.toDouble() }.toList()
        when (name) {
            "moveTo" -> {
                if (current.size >= 2) loops.add(current)
                current = mutableListOf()
                cx = args[0]; cy = args[1]
                sx = cx; sy = cy
                add(cx, cy)
                prev = "M"
            }
            "lineTo" -> {
                cx = args[0]; cy = args[1]
                add(cx, cy)
                prev = "L"
            }
            "horizontalLineTo" -> {
                cx = args[0]
                add(cx, cy)
                prev = "L"
            }
            "verticalLineTo" -> {
                cy = args[0]
                add(cx, cy)
                prev = "L"
            }
            "curveTo" -> {
                val x1 = args[0]; val y1 = args[1]
                val x2 = args[2]; val y2 = args[3]
                val x = args[4]; val y = args[5]
                flattenCubic(cx, cy, x1, y1, x2, y2, x, y, flatTolerance) { px, py -> add(px, py) }
                cx = x; cy = y
                lastC2X = x2; lastC2Y = y2
                lastQX = null; lastQY = null
                prev = "C"
            }
            "quadTo" -> {
                val x1 = args[0]; val y1 = args[1]
                val x = args[2]; val y = args[3]
                flattenQuad(cx, cy, x1, y1, x, y, flatTolerance) { px, py -> add(px, py) }
                cx = x; cy = y
                lastQX = x1; lastQY = y1
                lastC2X = null; lastC2Y = null
                prev = "Q"
            }
            "arcTo" -> {
                // arcTo(rx, ry, rot, large, sweep, x, y) — true/false flags are
                // not captured by numRe, so args are [rx, ry, rot, x, y].
                val rx = args[0]; val ry = args[1]; val rot = args[2]
                val flags = Regex("(true|false)").findAll(cmd).map { it.value }.toList()
                val large = flags.getOrElse(0) { "false" } == "true"
                val sweep = flags.getOrElse(1) { "false" } == "true"
                val x = args[3]; val y = args[4]
                flattenArc(cx, cy, rx, ry, rot, large, sweep, x, y, flatTolerance) { px, py -> add(px, py) }
                cx = x; cy = y
                lastC2X = null; lastC2Y = null; lastQX = null; lastQY = null
                prev = "A"
            }
            "close" -> {
                closeLoop()
                prev = "Z"
            }
        }
    }
    if (current.size >= 2) loops.add(current)
    return loops.map { simplifyLoop(it) }
}

/** Removes points that are nearly collinear with their neighbours. */
private fun simplifyLoop(loop: List<DoubleArray>): List<DoubleArray> {
    if (loop.size < 4) return loop
    val out = ArrayList<DoubleArray>(loop.size)
    var prev = loop[0]
    out.add(prev)
    for (i in 1 until loop.size - 1) {
        val p = loop[i]
        val next = loop[i + 1]
        // Area of triangle (prev, p, next); skip p if below threshold.
        val area = Math.abs((p[0] - prev[0]) * (next[1] - prev[1]) - (p[1] - prev[1]) * (next[0] - prev[0]))
        if (area > 0.01) {
            out.add(p)
            prev = p
        }
    }
    out.add(loop[loop.size - 1])
    return out
}

private fun flattenCubic(
    x0: Double, y0: Double, x1: Double, y1: Double,
    x2: Double, y2: Double, x3: Double, y3: Double,
    flatTolerance: Double,
    emit: (Double, Double) -> Unit,
) {
    // Explicit-parameter subdivision (no local-function capture; the JVM
    // backend misplaces captured doubles, making flatEnough always true).
    val stack = ArrayDeque<DoubleArray>()
    stack.add(doubleArrayOf(x0, y0, x1, y1, x2, y2, x3, y3, 0.0))
    while (stack.isNotEmpty()) {
        val s = stack.removeLast()
        val ax = s[0]; val ay = s[1]
        val bx = s[2]; val by = s[3]
        val cx = s[4]; val cy = s[5]
        val dx = s[6]; val dy = s[7]
        val depth = s[8].toInt()

        val len = Math.hypot(dx - ax, dy - ay)
        val flat = len < 1e-4 ||
            (Math.abs((bx - dx) * (dy - ay) - (by - dy) * (dx - ax)) +
                Math.abs((cx - dx) * (dy - ay) - (cy - dy) * (dx - ax))) / len < flatTolerance
        if (depth > 12 || flat) {
            emit(dx, dy)
            continue
        }
        val mx01 = (ax + bx) / 2; val my01 = (ay + by) / 2
        val mx12 = (bx + cx) / 2; val my12 = (by + cy) / 2
        val mx23 = (cx + dx) / 2; val my23 = (cy + dy) / 2
        val mx012 = (mx01 + mx12) / 2; val my012 = (my01 + my12) / 2
        val mx123 = (mx12 + mx23) / 2; val my123 = (my12 + my23) / 2
        val mx = (mx012 + mx123) / 2; val my = (my012 + my123) / 2
        stack.add(doubleArrayOf(mx, my, mx123, my123, mx23, my23, dx, dy, (depth + 1).toDouble()))
        stack.add(doubleArrayOf(ax, ay, mx01, my01, mx012, my012, mx, my, (depth + 1).toDouble()))
    }
}

private fun flattenQuad(
    x0: Double, y0: Double, x1: Double, y1: Double,
    x2: Double, y2: Double,
    flatTolerance: Double,
    emit: (Double, Double) -> Unit,
) {
    val stack = ArrayDeque<DoubleArray>()
    stack.add(doubleArrayOf(x0, y0, x1, y1, x2, y2, 0.0))
    while (stack.isNotEmpty()) {
        val s = stack.removeLast()
        val ax = s[0]; val ay = s[1]
        val bx = s[2]; val by = s[3]
        val cx = s[4]; val cy = s[5]
        val depth = s[6].toInt()

        val len = Math.hypot(cx - ax, cy - ay)
        val flat = len < 1e-4 ||
            Math.abs((bx - cx) * (cy - ay) - (by - cy) * (cx - ax)) / len < flatTolerance
        if (depth > 12 || flat) {
            emit(cx, cy)
            continue
        }
        val mx01 = (ax + bx) / 2; val my01 = (ay + by) / 2
        val mx12 = (bx + cx) / 2; val my12 = (by + cy) / 2
        val mx = (mx01 + mx12) / 2; val my = (my01 + my12) / 2
        stack.add(doubleArrayOf(mx, my, mx12, my12, cx, cy, (depth + 1).toDouble()))
        stack.add(doubleArrayOf(ax, ay, mx01, my01, mx, my, (depth + 1).toDouble()))
    }
}

private fun flattenArc(
    x0: Double, y0: Double,
    rxIn: Double, ryIn: Double, rotDeg: Double,
    largeArc: Boolean, sweep: Boolean,
    x: Double, y: Double,
    flatTolerance: Double,
    emit: (Double, Double) -> Unit,
) {
    var rx = Math.abs(rxIn)
    var ry = Math.abs(ryIn)
    if (rx == 0.0 || ry == 0.0 || (x0 == x && y0 == y)) {
        emit(x, y)
        return
    }
    val phi = Math.toRadians(rotDeg)
    val cosPhi = Math.cos(phi)
    val sinPhi = Math.sin(phi)

    val dx2 = (x0 - x) / 2.0
    val dy2 = (y0 - y) / 2.0
    val x1p = cosPhi * dx2 + sinPhi * dy2
    val y1p = -sinPhi * dx2 + cosPhi * dy2

    val lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry)
    if (lambda > 1.0) {
        val s = Math.sqrt(lambda)
        rx *= s
        ry *= s
    }

    val rx2 = rx * rx
    val ry2 = ry * ry
    val x1p2 = x1p * x1p
    val y1p2 = y1p * y1p
    val numerator = rx2 * ry2 - rx2 * y1p2 - ry2 * x1p2
    val denom = rx2 * y1p2 + ry2 * x1p2
    val radicand = if (denom == 0.0) 0.0 else numerator / denom
    val coef = if (radicand < 0.0) 0.0 else Math.sqrt(radicand)
    val sign = if (largeArc == sweep) -1.0 else 1.0
    val cxp = sign * coef * (rx * y1p) / ry
    val cyp = sign * coef * (-ry * x1p) / rx

    val cx = cosPhi * cxp - sinPhi * cyp + (x0 + x) / 2.0
    val cy = sinPhi * cxp + cosPhi * cyp + (y0 + y) / 2.0

    val ux = (x1p - cxp) / rx
    val uy = (y1p - cyp) / ry
    val vx = (-x1p - cxp) / rx
    val vy = (-y1p - cyp) / ry

    var theta1 = Math.atan2(uy, ux)
    var dTheta = Math.atan2(vy, vx) - theta1
    if (!sweep && dTheta > 0) dTheta -= 2 * Math.PI
    if (sweep && dTheta < 0) dTheta += 2 * Math.PI

    val segments = Math.max(1, Math.ceil(Math.abs(dTheta) / (Math.PI / 2.0)).toInt())
    val delta = dTheta / segments
    val t = (4.0 / 3.0) * Math.tan(delta / 4.0)

    var start = theta1
    for (i in 0 until segments) {
        val end = start + delta
        val sinStart = Math.sin(start); val cosStart = Math.cos(start)
        val sinEnd = Math.sin(end); val cosEnd = Math.cos(end)

        val p0x = cx + (cosPhi * rx * cosStart - sinPhi * ry * sinStart)
        val p0y = cy + (sinPhi * rx * cosStart + cosPhi * ry * sinStart)
        val p3x = cx + (cosPhi * rx * cosEnd - sinPhi * ry * sinEnd)
        val p3y = cy + (sinPhi * rx * cosEnd + cosPhi * ry * sinEnd)

        val dp0x = -cosPhi * rx * sinStart - sinPhi * ry * cosStart
        val dp0y = -sinPhi * rx * sinStart + cosPhi * ry * cosStart
        val dp3x = -cosPhi * rx * sinEnd - sinPhi * ry * cosEnd
        val dp3y = -sinPhi * rx * sinEnd + cosPhi * ry * cosEnd

        val c1x = p0x + t * dp0x
        val c1y = p0y + t * dp0y
        val c2x = p3x - t * dp3x
        val c2y = p3y - t * dp3y

        flattenCubic(p0x, p0y, c1x, c1y, c2x, c2y, p3x, p3y, flatTolerance, emit)
        start = end
    }
}

// ---------------------------------------------------------------------------
// earcut-based tessellation for filled contours.
// The JVM build logic runs the reference earcut triangulation (mapbox/earcut)
// on each outer contour plus its holes, producing clean contour-aligned
// triangles: no pixel grid, no scanline banding, far fewer vertices than
// rasterization (important: ImGui draw lists still use 16-bit indices unless
// the renderer advertises VtxOffset support).
// ---------------------------------------------------------------------------

/**
 * Tessellates filled contour loops into triangles with earcut. Contours are
 * grouped by containment (outer ring + its direct holes; nested islands are
 * their own groups), matching the SVG even-odd fill rule. Returns a flat
 * list x0,y0, x1,y1, x2,y2, ... in viewBox coordinates.
 *
 * @param loops each contour as flat x,y pairs (closed or open)
 * @param bounds unused, kept for API compatibility
 */
fun tessellateContours(loops: List<List<DoubleArray>>, bounds: DoubleArray, width: Int, height: Int): List<DoubleArray> {
    if (loops.isEmpty()) return emptyList()

    // Remove degenerate contours and closing duplicates.
    val cleaned = ArrayList<List<DoubleArray>>(loops.size)
    for (loop in loops) {
        if (loop.size < 3) continue
        val pts = if (loop.size > 1 && loop[0][0] == loop[loop.size - 1][0] &&
            loop[0][1] == loop[loop.size - 1][1]
        ) {
            loop.subList(0, loop.size - 1)
        } else loop
        if (pts.size >= 3) cleaned.add(pts)
    }
    if (cleaned.isEmpty()) return emptyList()

    // Group contours by containment depth with the SVG nonzero rule: a
    // contour is a hole only when its winding is opposite the ring that
    // contains it. Same-winding inner contours (e.g. a separate filled
    // symbol inside an outer frame) are independent solids and must be
    // triangulated on their own, not carved out.
    val groups = ArrayList<List<List<DoubleArray>>>()
    val used = BooleanArray(cleaned.size)
    for (i in cleaned.indices) {
        if (used[i]) continue
        val outer = cleaned[i]
        val outerWinding = contourWinding(outer)
        val group = ArrayList<List<DoubleArray>>()
        group.add(outer)
        for (j in cleaned.indices) {
            if (i == j || used[j]) continue
            val c = cleaned[j]
            // j is a direct hole of i if it is fully inside i, winds the
            // opposite way (signed area product negative), and no other
            // unused contour sits between them.
            if (contourInside(c, outer) && contourWinding(c) * outerWinding < 0 &&
                !hasContourBetween(j, i, cleaned, used)
            ) {
                group.add(c)
                used[j] = true
            }
        }
        used[i] = true
        groups.add(group)
    }

    val tris = ArrayList<DoubleArray>()
    for (group in groups) {
        val outer = group[0]
        var data = DoubleArray(outer.size * 2)
        for (k in outer.indices) {
            data[k * 2] = outer[k][0]
            data[k * 2 + 1] = outer[k][1]
        }
        val holeIndices = IntArray(group.size - 1)
        var offset = outer.size
        for (h in 1 until group.size) {
            holeIndices[h - 1] = offset
            val hole = group[h]
            val extra = DoubleArray(hole.size * 2)
            for (k in hole.indices) {
                extra[k * 2] = hole[k][0]
                extra[k * 2 + 1] = hole[k][1]
            }
            val tmp = DoubleArray(data.size + extra.size)
            System.arraycopy(data, 0, tmp, 0, data.size)
            System.arraycopy(extra, 0, tmp, data.size, extra.size)
            data = tmp
            offset += hole.size
        }
        val indices = earcut.Earcut.earcut(data, holeIndices)
        for (k in 0 until indices.size step 3) {
            val i0 = indices[k] * 2
            val i1 = indices[k + 1] * 2
            val i2 = indices[k + 2] * 2
            tris.add(doubleArrayOf(data[i0], data[i0 + 1], data[i1], data[i1 + 1], data[i2], data[i2 + 1]))
        }
    }
    return tris
}

/** True if [inner] lies fully inside [outer] (point-in-polygon on all vertices). */
private fun contourInside(inner: List<DoubleArray>, outer: List<DoubleArray>): Boolean {
    for (p in inner) {
        if (!pointInPolygon(p[0], p[1], outer)) return false
    }
    return true
}

/** Signed area of a contour: positive = counter-clockwise, negative = clockwise. */
private fun contourWinding(loop: List<DoubleArray>): Double {
    var sum = 0.0
    for (i in loop.indices) {
        val p = loop[i]
        val q = loop[(i + 1) % loop.size]
        sum += (q[0] - p[0]) * (q[1] + p[1])
    }
    return sum
}

/** True if any unused contour strictly between [outerIdx] and [candidate] contains the candidate. */
private fun hasContourBetween(
    candidate: Int,
    outerIdx: Int,
    loops: List<List<DoubleArray>>,
    used: BooleanArray,
): Boolean {
    for (k in loops.indices) {
        if (k == candidate || k == outerIdx || used[k]) continue
        if (contourInside(loops[candidate], loops[k])) return true
    }
    return false
}

private fun pointInPolygon(x: Double, y: Double, poly: List<DoubleArray>): Boolean {
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val xi = poly[i][0]; val yi = poly[i][1]
        val xj = poly[j][0]; val yj = poly[j][1]
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
            inside = !inside
        }
        j = i
    }
    return inside
}
