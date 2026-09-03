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
    val evenOdd: Boolean
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

    val primary = shapes.filter { it.opacity >= tintThreshold }
    val chosen = if (primary.isNotEmpty()) primary else shapes
    return chosen.map { it.asPublic }
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
    get() = SvgShape(commands, fill, stroke, strokeWidth, strokeLineCap, strokeLineJoin, evenOdd)

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
