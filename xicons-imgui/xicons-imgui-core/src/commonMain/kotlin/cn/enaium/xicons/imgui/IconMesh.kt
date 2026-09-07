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
import kotlin.math.abs
import cn.enaium.imgui.ImVec2
import cn.enaium.imgui.imguiTexUvWhitePixel

/**
 * A single point in a path.
 */
internal data class PathPoint(val x: Float, val y: Float)

/**
 * A cached mesh of an [Icon]: fill triangles plus stroke polylines, both in
 * viewBox space. Draw with [draw].
 *
 * @author Enaium
 */
public class IconMesh internal constructor(data: IconData) {
    /** Fill vertices: x0,y0, x1,y1, ... in viewBox space (deduplicated). */
    public val vertices: FloatArray

    /** Fill triangle indices into [vertices]. */
    internal val indices: IntArray

    /**
     * Low-opacity tint layers (e.g. antd/material twotone backgrounds):
     * each entry is a (vertices, indices, alpha) batch drawn beneath the
     * main color batch.
     */
    internal val tintGroups: List<TintGroup>

    internal class TintGroup(val vertices: FloatArray, val indices: IntArray, val alpha: Float)

    /** Normalized bounds (0..1 relative to the viewBox). */
    public val bounds: FloatArray

    /** Stroke polylines, each a flat x0,y0,x1,y1,... list plus width. */
    internal val strokes: List<Stroke>

    /** Loops that could not be ear-clipped; drawn with ImGui's native concave fill. */
    internal val concavePolys: List<FloatArray>

    /** Raw (viewBox-space) extent of the icon, used to scale in [draw]. */
    private val pathSize: Float

    /** Longest viewBox side, used to scale stroke widths (SVG widths are viewBox-relative). */
    private val viewBoxSize: Float

    internal class Stroke(val points: FloatArray, val width: Float)

    init {
        val tris = ArrayList<Float>(64)
        val strokeList = ArrayList<Stroke>(4)
        val concavePolys = ArrayList<FloatArray>(2)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        fun track(loop: List<Pair<Float, Float>>) {
            for ((x, y) in loop) {
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
        }

        val tintGroupList = ArrayList<TintGroup>(2)
        for (shape in data.shapes) {
            if (shape.fill) {
                if (shape.triangles.isNotEmpty()) {
                    // Pre-tessellated at generation time (even-odd rule,
                    // handles holes, concave and nested contours exactly).
                    val shapeTris = ArrayList<Float>(shape.triangles.size)
                    for (v in shape.triangles) shapeTris.add(v)
                    // Bounds tracking: fill-only shapes carry no loops, so
                    // track the triangle vertices directly.
                    for (i in shape.triangles.indices step 2) {
                        val x = shape.triangles[i]
                        val y = shape.triangles[i + 1]
                        if (x < minX) minX = x
                        if (y < minY) minY = y
                        if (x > maxX) maxX = x
                        if (y > maxY) maxY = y
                    }
                    if (shape.fillOpacity < 1f) {
                        // Tint layer: own batch, rendered with reduced alpha
                        // underneath the main color batch.
                        val (sv, si) = buildMesh(shapeTris)
                        tintGroupList.add(TintGroup(sv, si, shape.fillOpacity))
                    } else {
                        tris.addAll(shapeTris)
                    }
                } else {
                    // Drop sub-3-point loops; dedupe closing points happens in
                    // bridgeContours.
                    val loops = shape.loops.filter { loop -> loop.size >= 3 }
                        .map { loop -> loop.map { PathPoint(it.first, it.second) } }
                    if (loops.isNotEmpty()) {
                        val cleanedLoops = dedupeLoops(loops)
                        if (cleanedLoops.size == 1) {
                            // Single contour: reliable ear clipping (rendered
                            // with DrawTriangleFilled — no seam lines).
                            triangulate(cleanedLoops[0], tris)
                        } else {
                            // Multiple contours: merge via earcut-style bridging
                            // and let ImGui's native concave fill triangulate.
                            val merged = bridgeContours(cleanedLoops)
                            for (ring in merged) {
                                if (ring.size < 3) continue
                                val flat = FloatArray(ring.size * 2)
                                for (i in ring.indices) {
                                    flat[i * 2] = ring[i].x
                                    flat[i * 2 + 1] = ring[i].y
                                }
                                concavePolys.add(flat)
                            }
                        }
                    }
                }
            }
            for (loop in shape.loops) {
                if (loop.size < 2) continue
                track(loop)
                if (shape.stroke && shape.strokeWidth > 0f && loop.size >= 2) {
                    val flat = FloatArray(loop.size * 2)
                    for (i in loop.indices) {
                        flat[i * 2] = loop[i].first
                        flat[i * 2 + 1] = loop[i].second
                    }
                    strokeList.add(Stroke(flat, shape.strokeWidth))
                }
            }
        }

        val (v, i) = buildMesh(tris)
        vertices = v
        indices = i
        strokes = strokeList
        tintGroups = tintGroupList
        this.concavePolys = concavePolys
        bounds = floatArrayOf(
            if (data.viewBoxWidth > 0f) minX / data.viewBoxWidth else 0f,
            if (data.viewBoxHeight > 0f) minY / data.viewBoxHeight else 0f,
            if (data.viewBoxWidth > 0f) maxX / data.viewBoxWidth else 1f,
            if (data.viewBoxHeight > 0f) maxY / data.viewBoxHeight else 1f,
        )
        // Raw (viewBox-space) extent, used to scale vertices to draw-list
        // coordinates in [draw]. bounds are normalized, so they cannot be
        // used to scale raw vertices.
        pathSize = maxOf(maxX - minX, maxY - minY, 1e-4f)
        viewBoxSize = maxOf(data.viewBoxWidth, data.viewBoxHeight, 1e-4f)
    }

    private fun buildMesh(trisIn: ArrayList<Float>): Pair<FloatArray, IntArray> {
        // Deduplicate triangle vertices into a shared vertex pool so the
        // draw call can batch-write through PrimReserve/PrimWriteVtx/PrimWriteIdx
        // instead of one JNI call per triangle.
        val flat = trisIn.toFloatArray()
        val vtxMap = HashMap<Long, Int>(flat.size / 2)
        val vtx = ArrayList<Float>(flat.size)
        val idx = ArrayList<Int>(flat.size / 6 * 3)
        for (t in 0 until flat.size / 6) {
            for (k in 0 until 3) {
                val xi = (t * 6 + k * 2)
                val yi = xi + 1
                val key = (flat[xi].toBits().toLong() shl 32) or (flat[yi].toBits().toLong() and 0xFFFFFFFFL)
                val existing = vtxMap[key]
                if (existing != null) {
                    idx.add(existing)
                } else {
                    val newIdx = vtx.size / 2
                    vtxMap[key] = newIdx
                    vtx.add(flat[xi])
                    vtx.add(flat[yi])
                    idx.add(newIdx)
                }
            }
        }
        return vtx.toFloatArray() to idx.toIntArray()
    }

    private fun emitBatch(drawList: ImDrawList, verts: FloatArray, idxs: IntArray, ox: Float, oy: Float, scale: Float, color: Int) {
        if (idxs.isEmpty() || verts.isEmpty()) return
        val uv = cachedWhiteUv
        drawList.primReserve(idxs.size, verts.size / 2)
        val base = drawList.vtxCurrentIdx
        for (i in 0 until verts.size step 2) {
            drawList.primWriteVtx(
                ImVec2(ox + verts[i] * scale, oy + verts[i + 1] * scale),
                ImVec2(uv[0], uv[1]),
                color,
            )
        }
        for (i in idxs) {
            drawList.primWriteIdx(base + i)
        }
    }

    private companion object {
        val cachedWhiteUv: FloatArray by lazy { imguiTexUvWhitePixel() }
    }

    /**
     * Draws the mesh into [drawList].
     *
     * @param pos   top-left corner in draw-list coordinates
     * @param size  width and height in draw-list coordinates
     * @param color packed 0xRRGGBBAA
     */
    public fun draw(drawList: ImDrawList, pos: ImVec2, size: Float, color: Int) {
        // Scale relative to the larger of the viewBox and the content span:
        // icons whose geometry does not fill the viewBox keep a consistent
        // scale, while content that overflows the viewBox (some SVG files
        // have coordinates slightly outside their declared viewBox) never
        // draws outside the requested size.
        val scale = size / maxOf(viewBoxSize, pathSize)
        // Center the content's bounding box inside the size box.
        val contentW = bounds[2] - bounds[0]
        val contentH = bounds[3] - bounds[1]
        val ox = pos.x + (size - contentW * size) / 2f
        val oy = pos.y + (size - contentH * size) / 2f

        // Tint layers first (drawn beneath the main color).
        for (tint in tintGroups) {
            val alpha = ((color ushr 24) and 0xFF) * tint.alpha
            val tintColor = ((alpha.toInt() and 0xFF) shl 24) or (color and 0x00FFFFFF)
            emitBatch(drawList, tint.vertices, tint.indices, ox, oy, scale, tintColor)
        }
        // Main color batch.
        emitBatch(drawList, vertices, indices, ox, oy, scale, color)

        // Loops whose ear clipping failed: let ImGui's native concave fill
        // handle them (it tolerates degenerate/self-intersecting geometry).
        for (poly in concavePolys) {
            val nPts = poly.size / 2
            if (nPts < 3) continue
            val arr = Array(nPts) { ImVec2(ox + poly[it * 2] * scale, oy + poly[it * 2 + 1] * scale) }
            drawList.DrawConcavePolyFilled(arr, color)
        }

        // Stroke polylines. Stroke width is viewBox-relative in the SVG, so
        // scale it by the viewBox ratio (not the local path bounds, which
        // would make lines look too thick on icons that don't fill the box).
        for (stroke in strokes) {
            val pts = stroke.points
            val nPts = pts.size / 2
            if (nPts < 2) continue
            // A loop whose last point coincides with its first is a closed
            // contour (SVG Z); draw it closed and drop the duplicate point.
            val closed = nPts >= 3 &&
                abs(pts[0] - pts[(nPts - 1) * 2]) < 0.01f &&
                abs(pts[1] - pts[(nPts - 1) * 2 + 1]) < 0.01f
            val count = if (closed) nPts - 1 else nPts
            val arr = Array(count) { ImVec2(ox + pts[it * 2] * scale, oy + pts[it * 2 + 1] * scale) }
            drawList.DrawPolyline(
                arr,
                color,
                closed = closed,
                thickness = stroke.width * (size / viewBoxSize),
            )
        }
    }
}

/**
 * Deduplicates near-coincident consecutive points and drops degenerate loops.
 */
internal fun dedupeLoops(loops: List<List<PathPoint>>): List<List<PathPoint>> {
    val cleaned = ArrayList<List<PathPoint>>(loops.size)
    for (loop in loops) {
        val c = ArrayList<PathPoint>(loop.size)
        for (p in loop) {
            val last = c.lastOrNull()
            if (last != null && abs(p.x - last.x) < 0.01f && abs(p.y - last.y) < 0.01f) continue
            c.add(p)
        }
        if (c.size > 1) {
            val first = c[0]
            val last = c[c.size - 1]
            if (abs(first.x - last.x) < 0.01f && abs(first.y - last.y) < 0.01f) {
                c.removeAt(c.size - 1)
            }
        }
        if (c.size >= 3) cleaned.add(c)
    }
    return cleaned
}

/**
 * Merges a set of contours (outer + holes, nonzero fill rule) into simple
 * single rings via earcut-style bridging, so each ring can be filled by
 * ImGui's native concave tessellator (which handles thin borders, rounded
 * corners and concave outlines correctly).
 *
 * Returns the bridged rings; empty when no valid outer contour exists.
 */
internal fun bridgeContours(loops: List<List<PathPoint>>): List<List<PathPoint>> {
    // Deduplicate near-coincident consecutive points and drop degenerate loops.
    val cleaned = ArrayList<List<PathPoint>>(loops.size)
    for (loop in loops) {
        val c = ArrayList<PathPoint>(loop.size)
        for (p in loop) {
            val last = c.lastOrNull()
            if (last != null && abs(p.x - last.x) < 0.01f && abs(p.y - last.y) < 0.01f) continue
            c.add(p)
        }
        if (c.size > 1) {
            val first = c[0]
            val last = c[c.size - 1]
            if (abs(first.x - last.x) < 0.01f && abs(first.y - last.y) < 0.01f) {
                c.removeAt(c.size - 1)
            }
        }
        if (c.size >= 3) cleaned.add(c)
    }
    if (cleaned.isEmpty()) return emptyList()

    // Sort by |area| descending so outer contours come first.
    val sorted = cleaned.sortedByDescending { abs(loopArea(it)) }

    // Containment depth (nonzero rule): odd depth -> hole, even -> solid.
    data class L(val pts: List<PathPoint>, var depth: Int = 0)
    val items = sorted.map { L(it) }
    for (i in items.indices) {
        val p = items[i].pts[0]
        for (j in items.indices) {
            if (i == j) continue
            if (abs(loopArea(items[j].pts)) > abs(loopArea(items[i].pts)) &&
                pointInPolygon(p, items[j].pts)
            ) {
                items[i].depth++
            }
        }
    }
    val solids = items.filter { it.depth % 2 == 0 }
    val holes = items.filter { it.depth % 2 == 1 }

    val result = ArrayList<List<PathPoint>>()
    for (solid in solids) {
        // A hole belongs to the SMALLEST solid contour that contains it
        // (nested groups like camera body + lens each own their holes).
        val solidHoles = holes.filter { hole ->
            pointInPolygon(hole.pts[0], solid.pts) &&
                solids.none { other ->
                    other !== solid &&
                        pointInPolygon(hole.pts[0], other.pts) &&
                        abs(loopArea(other.pts)) < abs(loopArea(solid.pts))
                }
        }
        var ring = solid.pts.toMutableList()
        var ok = true
        for (hole in solidHoles) {
            val bridged = bridgeHole(ring, hole.pts)
            if (bridged == null) { ok = false; break }
            ring = bridged.toMutableList()
        }
        if (ok && ring.size >= 3) result.add(ring)
    }
    return result
}

/** True when segments (p1,p2) and (p3,p4) properly intersect. */
private fun segmentsIntersect(p1: PathPoint, p2: PathPoint, p3: PathPoint, p4: PathPoint): Boolean {
    fun cross(o: PathPoint, a: PathPoint, b: PathPoint): Float =
        (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    val d1 = cross(p3, p4, p1)
    val d2 = cross(p3, p4, p2)
    val d3 = cross(p1, p2, p3)
    val d4 = cross(p1, p2, p4)
    return ((d1 > 0f && d2 < 0f) || (d1 < 0f && d2 > 0f)) &&
        ((d3 > 0f && d4 < 0f) || (d3 < 0f && d4 > 0f))
}

/**
 * Merges [hole] into [ring] by bridging the closest pair of vertices
 * (outer ring point <-> hole point). Returns the merged ring, or null when
 * the hole does not fit (e.g. it is not inside the ring).
 */
private fun bridgeHole(ring: List<PathPoint>, hole: List<PathPoint>): List<PathPoint>? {
    if (hole.size < 3) return null
    // Find the farthest VISIBLE pair (oi, hi): bridging through the hole
    // interior (instead of across a thin border between hole and outer
    // ring) keeps the seam triangles inside the hole region, so filled
    // borders stay intact. The bridge segment must not cross any edge of
    // the ring or the hole.
    var bestI = -1
    var bestJ = -1
    var bestDist = -1f
    for (i in ring.indices) {
        val a = ring[i]
        val b = ring[(i + 1) % ring.size]
        for (j in hole.indices) {
            val c = hole[j]
            val d = hole[(j + 1) % hole.size]
            val dx = a.x - c.x
            val dy = a.y - c.y
            val dist = dx * dx + dy * dy
            if (dist <= bestDist) continue
            // visibility: segment a-c must not cross any ring edge
            var visible = true
            for (k in ring.indices) {
                if (k == i) continue
                val e = ring[k]
                val f = ring[(k + 1) % ring.size]
                if (segmentsIntersect(a, c, e, f)) { visible = false; break }
            }
            if (visible) {
                for (k in hole.indices) {
                    if (k == j) continue
                    val e = hole[k]
                    val f = hole[(k + 1) % hole.size]
                    if (segmentsIntersect(a, c, e, f)) { visible = false; break }
                }
            }
            if (visible) {
                bestDist = dist
                bestI = i
                bestJ = j
            }
        }
    }
    if (bestI < 0) {
        // No visible bridge found (e.g. a hole nested deep inside a busy
        // contour). Fall back to the closest pair without the visibility
        // requirement — earcut's visibility check is an optimization, not
        // a correctness requirement.
        var bi = -1; var bj = -1; var bd = -1f
        for (i in ring.indices) {
            for (j in hole.indices) {
                val dx = ring[i].x - hole[j].x
                val dy = ring[i].y - hole[j].y
                val d = dx * dx + dy * dy
                if (d > bd) { bd = d; bi = i; bj = j }
            }
        }
        bestI = bi; bestJ = bj
    }
    if (bestI < 0) return null
    // Earcut requires the hole to be wound OPPOSITE to the outer ring, so the
    // hole interior becomes exterior of the merged polygon. Insert the hole
    // in its original order when the outer is clockwise, reversed when the
    // outer is counter-clockwise.
    val outerCw = loopArea(ring) < 0f
    val holeCw = loopArea(hole) < 0f
    val reverse = outerCw == holeCw  // need opposite winding
    val out = ArrayList<PathPoint>(ring.size + hole.size + 2)
    for (k in 0..bestI) out.add(ring[k])
    if (reverse) {
        for (k in 0 until hole.size) {
            out.add(hole[(bestJ - k + hole.size) % hole.size])
        }
    } else {
        for (k in 0 until hole.size) {
            out.add(hole[(bestJ + k) % hole.size])
        }
    }
    out.add(hole[bestJ]) // duplicate bridge vertex back
    for (k in bestI + 1 until ring.size) out.add(ring[k])
    return out
}

private fun loopArea(loop: List<PathPoint>): Float {
    var area = 0f
    for (i in loop.indices) {
        val p = loop[i]
        val q = loop[(i + 1) % loop.size]
        area += p.x * q.y - q.x * p.y
    }
    return area / 2f
}

/** Ray-casting point-in-polygon test. */
private fun pointInPolygon(p: PathPoint, poly: List<PathPoint>): Boolean {
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[j]
        if ((a.y > p.y) != (b.y > p.y) &&
            p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

/**
 * Ear-clips [points] into [out]. Returns true when the polygon was fully
 * triangulated; false when ear clipping stalled (self-intersecting or
 * otherwise degenerate geometry) — in that case only a partial mesh may
 * have been appended and callers must roll it back.
 */
internal fun triangulate(points: List<PathPoint>, out: MutableList<Float>): Boolean {
    // Remove near-duplicate consecutive points (e.g. an almost-closed loop
    // whose first/last point differ by a rounding epsilon). Such degenerate
    // edges block ear clipping and leave unfilled gaps.
    val cleaned = ArrayList<PathPoint>(points.size)
    for (p in points) {
        val last = cleaned.lastOrNull()
        if (last != null && kotlin.math.abs(p.x - last.x) < 0.01f &&
            kotlin.math.abs(p.y - last.y) < 0.01f
        ) {
            continue
        }
        cleaned.add(p)
    }
    // Also merge first/last if they are near-coincident (closed loop).
    if (cleaned.size > 1) {
        val first = cleaned[0]
        val last = cleaned[cleaned.size - 1]
        if (kotlin.math.abs(first.x - last.x) < 0.01f &&
            kotlin.math.abs(first.y - last.y) < 0.01f
        ) {
            cleaned.removeAt(cleaned.size - 1)
        }
    }
    val pts = cleaned

    val n = pts.size
    if (n < 3) return false
    if (n == 3) {
        out.add(pts[0].x); out.add(pts[0].y)
        out.add(pts[1].x); out.add(pts[1].y)
        out.add(pts[2].x); out.add(pts[2].y)
        return true
    }

    // Work on an index list, removing ears as we go.
    val idx = ArrayList<Int>(n)
    for (i in 0 until n) idx.add(i)

    // Determine winding: positive area = CCW.
    var area = 0f
    for (i in 0 until n) {
        val p = pts[idx[i]]
        val q = pts[idx[(i + 1) % n]]
        area += p.x * q.y - q.x * p.y
    }
    val ccw = area >= 0f

    var guard = 0
    var i = 0
    while (idx.size > 3 && guard < n * n) {
        guard++
        val i0 = idx[i]
        val i1 = idx[(i + 1) % idx.size]
        val i2 = idx[(i + 2) % idx.size]
        val a = pts[i0]
        val b = pts[i1]
        val c = pts[i2]

        if (isEar(a, b, c, ccw, pts, idx)) {
            out.add(a.x); out.add(a.y)
            out.add(b.x); out.add(b.y)
            out.add(c.x); out.add(c.y)
            idx.removeAt((i + 1) % idx.size)
            i %= idx.size
        } else {
            i = (i + 1) % idx.size
        }
    }

    if (idx.size == 3) {
        val a = pts[idx[0]]
        val b = pts[idx[1]]
        val c = pts[idx[2]]
        out.add(a.x); out.add(a.y)
        out.add(b.x); out.add(b.y)
        out.add(c.x); out.add(c.y)
        return true
    }
    // Stalled: leftover points could not be clipped (self-intersection).
    return false
}

private fun isEar(
    a: PathPoint,
    b: PathPoint,
    c: PathPoint,
    ccw: Boolean,
    points: List<PathPoint>,
    idx: List<Int>,
): Boolean {
    // Reflex test: the candidate ear tip b must be convex (same side as the
    // polygon winding).
    val cross = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
    if (ccw && cross <= 0f) return false
    if (!ccw && cross >= 0f) return false

    // Containment test: no other polygon vertex may lie inside the ear
    // triangle (a, b, c).
    for (i in idx) {
        val p = points[i]
        if (p === a || p === b || p === c) continue
        if (pointInTriangle(p, a, b, c)) return false
    }
    return true
}

private fun pointInTriangle(p: PathPoint, a: PathPoint, b: PathPoint, c: PathPoint): Boolean {
    val d1 = sign(p, a, b)
    val d2 = sign(p, b, c)
    val d3 = sign(p, c, a)
    val hasNeg = d1 < 0f || d2 < 0f || d3 < 0f
    val hasPos = d1 > 0f || d2 > 0f || d3 > 0f
    return !(hasNeg && hasPos)
}

private fun sign(p1: PathPoint, p2: PathPoint, p3: PathPoint): Float =
    (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
