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

/**
 * Compile-time generated icon geometry: flattened polygon loops per shape,
 * with fill/stroke metadata. Produced by the `generateImgui` Gradle task.
 *
 * @author Enaium
 */
public class IconData(
    public val viewBoxWidth: Float,
    public val viewBoxHeight: Float,
    public val shapes: List<Shape>,
) {
    /**
     * One drawable unit: closed polygon loops (each a list of (x, y) pairs
     * in viewBox coordinates) plus styling. Filled shapes carry
     * [triangles] (a pre-tessellated mesh: flat x0,y0,x1,y1,x2,y2, ...);
     * [loops] are kept for stroke rendering.
     */
    public class Shape(
        public val fill: Boolean,
        public val stroke: Boolean,
        public val strokeWidth: Float,
        public val fillOpacity: Float = 1f,
        public val triangles: FloatArray = FloatArray(0),
        public val loops: List<List<Pair<Float, Float>>> = emptyList(),
    )
}
