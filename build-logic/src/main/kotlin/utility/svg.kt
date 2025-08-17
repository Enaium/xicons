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

import java.util.*
import java.util.regex.Pattern

/**
 * @author Enaium
 */
fun svg(content: String, f: Boolean = false): List<String> {
    val lines = mutableListOf<String>()
    val ds = extractPathDAttributes(content)
    for (d in ds) {
        lines.addAll(convertPathDataToJava(d, f))
    }
    return lines
}

fun extractSvgViewBoxAttributes(svg: String): List<String> {
    val result = mutableListOf<String>()
    val p = Pattern.compile("<svg[^>]*\\sviewBox=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
    val m = p.matcher(svg)
    while (m.find()) result.add(m.group(1))
    return result
}

private fun extractPathDAttributes(svg: String): List<String> {
    val result = mutableListOf<String>()
    val p = Pattern.compile("<path[^>]*\\sd=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
    val m = p.matcher(svg)
    while (m.find()) result.add(m.group(1))
    return result
}

private fun convertPathDataToJava(d: String, f: Boolean = false): List<String> {
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

    fun f(v: Double): String {
        return String.format(Locale.US, "%.2f", v).let {
            if (f) {
                "${it}f"
            } else {
                it
            }
        }
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
                lines.add(String.format("moveTo(%s, %s)", f(x), f(y)))
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
                    lines.add(String.format("lineTo(%s, %s)", f(x), f(y)))
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
                    lines.add(String.format("lineTo(%s, %s)", f(x), f(y)))
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
                    lines.add(String.format("horizontalLineTo(%s)", f(x)))
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
                    lines.add(String.format("verticalLineTo(%s)", f(y)))
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
                    lines.add(
                        String.format(
                            "curveTo(%s, %s, %s, %s, %s, %s)",
                            f(x1),
                            f(y1),
                            f(x2),
                            f(y2),
                            f(x),
                            f(y)
                        )
                    )
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
                    lines.add(
                        String.format(
                            "curveTo(%s, %s, %s, %s, %s, %s)",
                            f(x1),
                            f(y1),
                            f(x2),
                            f(y2),
                            f(x),
                            f(y)
                        )
                    )
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
                    lines.add(String.format("quadTo(%s, %s, %s, %s)", f(x1), f(y1), f(x), f(y)))
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
                    lines.add(String.format("quadTo(%s, %s, %s, %s)", f(x1), f(y1), f(x), f(y)))
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
                    lines.add(
                        String.format(
                            "arcTo(%s, %s, %s, %s, %s, %s, %s)",
                            f(rx),
                            f(ry),
                            f(rot),
                            large != 0,
                            sweep != 0,
                            f(x),
                            f(y)
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