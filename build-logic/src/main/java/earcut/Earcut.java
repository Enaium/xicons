package earcut;

import java.util.ArrayList;
import java.util.List;

/**
 * Java port of the earcut polygon triangulation algorithm
 * (https://github.com/mapbox/earcut v2.2.4, ISC license) — the reference
 * implementation for triangulating polygons with holes, concave outlines
 * and self-intersecting contours. Used at icon-generation time so the
 * runtime only emits clean contour-aligned triangles.
 *
 * Input: flat [data] (x0,y0,x1,y1,...), [holeIndices] marking the start
 * vertex index of each hole ring. Output: triangle vertex indices.
 */
public final class Earcut {

    private Earcut() {
    }

    public static int[] earcut(double[] data, int[] holeIndices) {
        int dim = 2;
        boolean hasHoles = holeIndices != null && holeIndices.length > 0;
        int outerLen = hasHoles ? holeIndices[0] * dim : data.length;
        Node outerNode = linkedList(data, 0, outerLen, dim, true);
        List<Integer> triangles = new ArrayList<>();

        if (outerNode == null || outerNode.next == outerNode.prev) return new int[0];

        if (hasHoles) outerNode = eliminateHoles(data, holeIndices, outerNode, dim);

        earcutLinked(outerNode, triangles, dim, 0);

        int[] out = new int[triangles.size()];
        for (int k = 0; k < out.length; k++) out[k] = triangles.get(k);
        return out;
    }

    private static Node linkedList(double[] data, int start, int end, int dim, boolean clockwise) {
        Node last = null;
        if (clockwise == (signedArea(data, start, end, dim) > 0)) {
            for (int i = start; i < end; i += dim) {
                last = insertNode(i, data[i], data[i + 1], last);
            }
        } else {
            for (int i = end - dim; i >= start; i -= dim) {
                last = insertNode(i, data[i], data[i + 1], last);
            }
        }
        if (last != null && equals(last, last.next)) {
            removeNode(last);
            last = last.next;
        }
        return last;
    }

    private static Node eliminateHoles(double[] data, int[] holeIndices, Node outerNode, int dim) {
        List<Node> queue = new ArrayList<>();
        int len = holeIndices.length;
        for (int i = 0; i < len; i++) {
            int start = holeIndices[i] * dim;
            int end = i < len - 1 ? holeIndices[i + 1] * dim : data.length;
            Node list = linkedList(data, start, end, dim, false);
            if (list == list.next) list.steiner = true;
            queue.add(getLeftmost(list));
        }
        queue.sort((a, b) -> Double.compare(a.x, b.x));

        for (Node hole : queue) {
            outerNode = eliminateHole(hole, outerNode);
        }
        return outerNode;
    }

    private static Node eliminateHole(Node hole, Node outerNode) {
        Node bridge = findHoleBridge(hole, outerNode);
        if (bridge == null) return outerNode;

        Node bridgeReverse = splitPolygon(bridge, hole);

        // Filter collinear points around the cuts.
        filterPoints(bridgeReverse, bridgeReverse.next);
        return filterPoints(bridge, bridge.next);
    }

    private static Node findHoleBridge(Node hole, Node outerNode) {
        Node p = outerNode;
        double hx = hole.x;
        double hy = hole.y;
        double qx = -Double.MAX_VALUE;
        Node m = null;

        // Find a segment intersected by a ray from the hole's leftmost point
        // to the left; the segment's endpoint with the lesser x is the
        // potential connection point.
        do {
            if (hy <= p.y && hy >= p.next.y && p.next.y != p.y) {
                double x = p.x + (hy - p.y) * (p.next.x - p.x) / (p.next.y - p.y);
                if (x <= hx && x > qx) {
                    qx = x;
                    m = p.x < p.next.x ? p : p.next;
                    if (x == hx) return m; // hole touches outer segment; pick leftmost endpoint
                }
            }
            p = p.next;
        } while (p != outerNode);

        if (m == null) return null;

        // Look for points inside the triangle of the hole point, segment
        // intersection and endpoint; if there are no points found, we have a
        // valid connection; otherwise choose the point with the minimum angle
        // with the ray.
        Node stop = m;
        double mx = m.x;
        double my = m.y;
        double tanMin = Double.MAX_VALUE;

        p = m;
        do {
            if (hx >= p.x && p.x >= mx && hx != p.x &&
                pointInTriangle(hy < my ? hx : qx, hy, mx, my, hy < my ? qx : hx, hy, p.x, p.y)) {
                double tan = Math.abs(hy - p.y) / (hx - p.x); // tangential
                if (locallyInside(p, hole) &&
                    (tan < tanMin || (tan == tanMin && (p.x > m.x || (p.x == m.x && sectorContainsSector(m, p)))))) {
                    m = p;
                    tanMin = tan;
                }
            }
            p = p.next;
        } while (p != stop);

        return m;
    }

    private static boolean sectorContainsSector(Node m, Node p) {
        return area(m.prev, m, p.prev) < 0 && area(p.next, m, m.next) < 0;
    }

    private static void earcutLinked(Node earIn, List<Integer> triangles, int dim, int pass) {
        Node ear = earIn;
        if (ear == null) return;

        Node stop = ear;
        Node prev;
        Node next;

        // Iteratively trim ears.
        while (ear.prev != ear.next) {
            prev = ear.prev;
            next = ear.next;

            if (isEar(ear)) {
                // Cut off the triangle.
                triangles.add(prev.i / dim);
                triangles.add(ear.i / dim);
                triangles.add(next.i / dim);

                removeNode(ear);

                // Skipping the next vertex leads to less sliver triangles.
                ear = next.next;
                stop = next.next;
                continue;
            }

            ear = next;

            // If we looped through the whole remaining polygon and can't find
            // any more ears:
            if (ear == stop) {
                if (pass == 0) {
                    // Try filtering points and slicing again.
                    earcutLinked(filterPoints(ear), triangles, dim, 1);
                } else if (pass == 1) {
                    // Try curing all small local self-intersections.
                    ear = cureLocalIntersections(filterPoints(ear), triangles, dim);
                    earcutLinked(ear, triangles, dim, 2);
                } else if (pass == 2) {
                    // As a last resort, try splitting the remaining polygon
                    // into two.
                    splitEarcut(ear, triangles, dim);
                }
                break;
            }
        }
    }

    private static boolean isEar(Node ear) {
        Node a = ear.prev;
        Node b = ear;
        Node c = ear.next;

        if (area(a, b, c) >= 0) return false; // reflex, can't be an ear

        // Triangle bbox; min & max are calculated like this for speed.
        double ax = a.x, bx = b.x, cx = c.x, ay = a.y, by = b.y, cy = c.y;
        double x0 = ax < bx ? (Math.min(ax, cx)) : (Math.min(bx, cx));
        double y0 = ay < by ? (Math.min(ay, cy)) : (Math.min(by, cy));
        double x1 = ax > bx ? (Math.max(ax, cx)) : (Math.max(bx, cx));
        double y1 = ay > by ? (Math.max(ay, cy)) : (Math.max(by, cy));

        Node p = c.next;
        while (p != a) {
            if (p.x >= x0 && p.x <= x1 && p.y >= y0 && p.y <= y1 &&
                pointInTriangle(ax, ay, bx, by, cx, cy, p.x, p.y) &&
                area(p.prev, p, p.next) >= 0) return false;
            p = p.next;
        }
        return true;
    }

    private static Node cureLocalIntersections(Node startIn, List<Integer> triangles, int dim) {
        Node start = startIn;
        Node p = start;
        do {
            Node a = p.prev;
            Node b = p.next.next;

            if (!equals(a, b) && intersects(a, p, p.next, b) && locallyInside(a, b) && locallyInside(b, a)) {
                triangles.add(a.i / dim);
                triangles.add(p.i / dim);
                triangles.add(b.i / dim);

                // Remove two nodes involved.
                removeNode(p);
                removeNode(p.next);

                p = start = b;
            }
            p = p.next;
        } while (p != start);

        return filterPoints(p);
    }

    private static void splitEarcut(Node start, List<Integer> triangles, int dim) {
        Node a = start;
        do {
            Node b = a.next.next;
            while (b != a.prev) {
                if (a.i != b.i && isValidDiagonal(a, b)) {
                    // Split the polygon in two by the diagonal.
                    Node c = splitPolygon(a, b);

                    // Filter collinear points around the cuts.
                    a = filterPoints(a, a.next);
                    c = filterPoints(c, c.next);

                    // Run earcut on each half.
                    earcutLinked(a, triangles, dim, 0);
                    earcutLinked(c, triangles, dim, 0);
                    return;
                }
                b = b.next;
            }
            a = a.next;
        } while (a != start);
    }

    private static boolean isValidDiagonal(Node a, Node b) {
        return a.next.i != b.i && a.prev.i != b.i &&
            !intersectsPolygon(a, b) &&
            (locallyInside(a, b) && locallyInside(b, a) && middleInside(a, b) &&
                (area(a.prev, a, b.prev) != 0 || area(a, b.prev, b) != 0) ||
                equals(a, b) && area(a.prev, a, a.next) > 0 && area(b.prev, b, b.next) > 0);
    }

    private static boolean intersectsPolygon(Node a, Node b) {
        Node p = a;
        do {
            if (p.i != a.i && p.next.i != a.i && p.i != b.i && p.next.i != b.i &&
                intersects(p, p.next, a, b)) {
                return true;
            }
            p = p.next;
        } while (p != a);
        return false;
    }

    private static boolean intersects(Node p1, Node q1, Node p2, Node q2) {
        double o1 = sign(area(p1, q1, p2));
        double o2 = sign(area(p1, q1, q2));
        double o3 = sign(area(p2, q2, p1));
        double o4 = sign(area(p2, q2, q1));

        if (o1 != o2 && o3 != o4) return true; // general case

        if (o1 == 0 && onSegment(p1, p2, q1)) return true;
        if (o2 == 0 && onSegment(p1, q2, q1)) return true;
        if (o3 == 0 && onSegment(p2, p1, q2)) return true;
        if (o4 == 0 && onSegment(p2, q1, q2)) return true;
        return false;
    }

    private static boolean onSegment(Node p, Node q, Node r) {
        return q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) &&
            q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y);
    }

    private static double sign(double num) {
        return num > 0 ? 1 : (num < 0 ? -1 : 0);
    }

    private static boolean middleInside(Node a, Node b) {
        Node p = a;
        boolean inside = false;
        double px = (a.x + b.x) / 2;
        double py = (a.y + b.y) / 2;
        do {
            if ((p.y > py) != (p.next.y > py) && p.next.y != p.y &&
                px < (p.next.x - p.x) * (py - p.y) / (p.next.y - p.y) + p.x) {
                inside = !inside;
            }
            p = p.next;
        } while (p != a);
        return inside;
    }

    private static Node filterPoints(Node start) {
        return filterPoints(start, null);
    }

    private static Node filterPoints(Node startIn, Node endIn) {
        Node start = startIn;
        Node end = endIn;
        if (start == null) return null;
        if (end == null) end = start;
        Node p = start;
        boolean again;
        do {
            again = false;
            if (!p.steiner && (equals(p, p.next) || area(p.prev, p, p.next) == 0)) {
                removeNode(p);
                p = end = p.prev;
                if (p == p.next) break;
                again = true;
            } else {
                p = p.next;
            }
        } while (again || p != end);
        return end;
    }

    private static Node splitPolygon(Node a, Node b) {
        Node a2 = new Node(a.i, a.x, a.y);
        Node b2 = new Node(b.i, b.x, b.y);
        Node an = a.next;
        Node bp = b.prev;

        a.next = b;
        b.prev = a;

        a2.next = an;
        an.prev = a2;

        b2.next = a2;
        a2.prev = b2;

        bp.next = b2;
        b2.prev = bp;

        return b2;
    }

    private static Node insertNode(int i, double x, double y, Node last) {
        Node p = new Node(i, x, y);
        if (last == null) {
            p.prev = p;
            p.next = p;
        } else {
            p.next = last.next;
            p.prev = last;
            last.next.prev = p;
            last.next = p;
        }
        return p;
    }

    private static void removeNode(Node p) {
        p.next.prev = p.prev;
        p.prev.next = p.next;
    }

    private static double signedArea(double[] data, int start, int end, int dim) {
        double sum = 0;
        int j = end - dim;
        for (int i = start; i < end; i += dim) {
            sum += (data[j] - data[i]) * (data[i + 1] + data[j + 1]);
            j = i;
        }
        return sum;
    }

    private static double area(Node p, Node q, Node r) {
        return (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
    }

    private static boolean equals(Node p1, Node p2) {
        return p1.x == p2.x && p1.y == p2.y;
    }

    private static Node getLeftmost(Node start) {
        Node p = start;
        Node leftmost = start;
        do {
            if (p.x < leftmost.x || (p.x == leftmost.x && p.y < leftmost.y)) leftmost = p;
            p = p.next;
        } while (p != start);
        return leftmost;
    }

    private static boolean pointInTriangle(
        double ax, double ay, double bx, double by,
        double cx, double cy, double px, double py) {
        return (cx - px) * (ay - py) >= (ax - px) * (cy - py) &&
            (ax - px) * (by - py) >= (bx - px) * (ay - py) &&
            (bx - px) * (cy - py) >= (cx - px) * (by - py);
    }

    private static boolean locallyInside(Node a, Node b) {
        return area(a.prev, a, a.next) < 0 ?
            area(a, b, a.next) >= 0 && area(a, a.prev, b) >= 0 :
            area(a, b, a.prev) < 0 || area(a, a.next, b) < 0;
    }

    private static final class Node {
        int i;
        double x;
        double y;
        Node prev;
        Node next;
        boolean steiner;

        Node(int i, double x, double y) {
            this.i = i;
            this.x = x;
            this.y = y;
        }
    }
}
