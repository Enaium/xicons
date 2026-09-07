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

package cn.enaium.xicons.imgui

import cn.enaium.imgui.ImDrawList
import cn.enaium.imgui.ImGui
import cn.enaium.imgui.ImVec2

/**
 * An icon backed by generated [IconData]. The geometry is triangulated once
 * on first use and cached; subsequent [draw] calls only emit triangles.
 *
 * @author Enaium
 */
public class Icon internal constructor(
    internal val data: IconData,
) {
    internal fun mesh(): IconMesh {
        val cached = meshCache
        if (cached != null) return cached
        val m = IconMesh(data)
        meshCache = m
        return m
    }

    private var meshCache: IconMesh? = null

    /**
     * Draws this icon into [drawList] at [pos] (top-left), scaled to [size]
     * (width = height), tinted with [color] (packed 0xRRGGBBAA).
     */
    public fun draw(drawList: ImDrawList, pos: ImVec2, size: Float, color: Int = DEFAULT_COLOR) {
        mesh().draw(drawList, pos, size, color)
    }

    /**
     * Draws this icon at the current ImGui cursor position (like
     * [ImGui.text]) with the given [size] and [color] (both optional).
     */
    public fun draw(size: Float = DEFAULT_SIZE, color: Int = DEFAULT_COLOR) {
        draw(ImGui.getWindowDrawList(), ImGui.getCursorScreenPos(), size, color)
    }

    /**
     * Draws this icon at [pos] (top-left) in the current window's draw list.
     */
    public fun draw(pos: ImVec2, size: Float = DEFAULT_SIZE, color: Int = DEFAULT_COLOR) {
        draw(ImGui.getWindowDrawList(), pos, size, color)
    }

    public companion object {
        /** Default icon size in pixels used by the cursor-position overloads. */
        public const val DEFAULT_SIZE: Float = 24f

        /** Default icon color: black (0xFF000000), matching swing/jfx/compose. */
        public const val DEFAULT_COLOR: Int = 0xFF000000.toInt()

        /** Builds an [Icon] from generated [IconData]. */
        public fun fromData(data: IconData): Icon = Icon(data)
    }
}

/**
 * Draws this icon centered in the given rectangle.
 */
public fun Icon.drawCentered(
    drawList: ImDrawList,
    center: ImVec2,
    size: Float,
    color: Int = Icon.DEFAULT_COLOR,
) {
    // [IconMesh.draw] always spans `size` pixels along its longest axis, so
    // the drawn box is exactly size x size regardless of the viewBox.
    val left = center.x - size / 2f
    val top = center.y - size / 2f
    draw(drawList, ImVec2(left, top), size, color)
}

/**
 * Draws this icon centered at [center] in the current window's draw list.
 */
public fun Icon.drawCentered(
    center: ImVec2,
    size: Float = Icon.DEFAULT_SIZE,
    color: Int = Icon.DEFAULT_COLOR,
) {
    drawCentered(ImGui.getWindowDrawList(), center, size, color)
}