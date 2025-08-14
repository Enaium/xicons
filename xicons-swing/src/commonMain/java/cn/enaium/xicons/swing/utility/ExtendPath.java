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

package cn.enaium.xicons.swing.utility;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * @author Enaium
 */
public class ExtendPath extends Path2D.Double {
    public void horizontalLineTo(double x) {
        Point2D currentPoint = getCurrentPoint();
        if (currentPoint != null) {
            lineTo(x, currentPoint.getY());
        } else {
            throw new IllegalStateException("No current point in the path.");
        }
    }

    public void verticalLineTo(double y) {
        Point2D currentPoint = getCurrentPoint();
        if (currentPoint != null) {
            lineTo(currentPoint.getX(), y);
        } else {
            throw new IllegalStateException("No current point in the path.");
        }
    }


    public void arcTo(double rx, double ry, double xAxisRotation, boolean largeArcFlag, boolean sweepFlag, double x, double y) {
        Point2D currentPoint = getCurrentPoint();
        if (currentPoint == null) {
            moveTo(x, y);
            return;
        }

        double x0 = currentPoint.getX();
        double y0 = currentPoint.getY();

        if (x0 == x && y0 == y) {
            return;
        }
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        if (rx == 0 || ry == 0) {
            lineTo(x, y);
            return;
        }

        double phi = Math.toRadians(xAxisRotation % 360.0);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);

        double dx2 = (x0 - x) / 2.0;
        double dy2 = (y0 - y) / 2.0;
        double x1p = cosPhi * dx2 + sinPhi * dy2;
        double y1p = -sinPhi * dx2 + cosPhi * dy2;

        double rx2 = rx * rx;
        double ry2 = ry * ry;
        double x1p2 = x1p * x1p;
        double y1p2 = y1p * y1p;
        double lambda = x1p2 / rx2 + y1p2 / ry2;
        if (lambda > 1) {
            double scale = Math.sqrt(lambda);
            rx *= scale;
            ry *= scale;
            rx2 = rx * rx;
            ry2 = ry * ry;
        }

        double sign = (largeArcFlag == sweepFlag) ? -1.0 : 1.0;
        double numerator = rx2 * ry2 - rx2 * y1p2 - ry2 * x1p2;
        double denom = rx2 * y1p2 + ry2 * x1p2;
        double cFactor = (denom == 0) ? 0 : sign * Math.sqrt(Math.max(0, numerator / denom));
        double cxp = cFactor * (rx * y1p) / ry;
        double cyp = cFactor * (-ry * x1p) / rx;

        double cx = cosPhi * cxp - sinPhi * cyp + (x0 + x) / 2.0;
        double cy = sinPhi * cxp + cosPhi * cyp + (y0 + y) / 2.0;

        double ux = (x1p - cxp) / rx;
        double uy = (y1p - cyp) / ry;
        double vx = (-x1p - cxp) / rx;
        double vy = (-y1p - cyp) / ry;

        double n = Math.hypot(ux, uy);
        double p = Math.hypot(vx, vy);
        if (n == 0 || p == 0) {
            lineTo(x, y);
            return;
        }

        double dot = ux * vx + uy * vy;
        double det = ux * vy - uy * vx;
        double theta1 = angle(1, 0, ux, uy);
        double deltaTheta = Math.atan2(det, dot);

        // Ensure sweep
        if (!sweepFlag && deltaTheta > 0) {
            deltaTheta -= Math.PI * 2;
        } else if (sweepFlag && deltaTheta < 0) {
            deltaTheta += Math.PI * 2;
        }

        int segments = (int) Math.ceil(Math.abs(deltaTheta) / (Math.PI / 2.0));
        double delta = deltaTheta / segments;
        double t = (4.0 / 3.0) * Math.tan(delta / 4.0);

        double startAngle = theta1;
        for (int i = 0; i < segments; i++) {
            double endAngle = startAngle + delta;

            double sinStart = Math.sin(startAngle);
            double cosStart = Math.cos(startAngle);
            double sinEnd = Math.sin(endAngle);
            double cosEnd = Math.cos(endAngle);

            double p0x = cx + (cosPhi * rx * cosStart - sinPhi * ry * sinStart);
            double p0y = cy + (sinPhi * rx * cosStart + cosPhi * ry * sinStart);
            double p3x = cx + (cosPhi * rx * cosEnd - sinPhi * ry * sinEnd);
            double p3y = cy + (sinPhi * rx * cosEnd + cosPhi * ry * sinEnd);

            double dp0x = -cosPhi * rx * sinStart - sinPhi * ry * cosStart;
            double dp0y = -sinPhi * rx * sinStart + cosPhi * ry * cosStart;
            double dp3x = -cosPhi * rx * sinEnd - sinPhi * ry * cosEnd;
            double dp3y = -sinPhi * rx * sinEnd + cosPhi * ry * cosEnd;

            double c1x = p0x + t * dp0x;
            double c1y = p0y + t * dp0y;
            double c2x = p3x - t * dp3x;
            double c2y = p3y - t * dp3y;

            curveTo(c1x, c1y, c2x, c2y, p3x, p3y);
            startAngle = endAngle;
        }
    }

    private static double angle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.hypot(ux, uy) * Math.hypot(vx, vy);
        if (len == 0) return 0;
        double cos = Math.max(-1, Math.min(1, dot / len));
        double ang = Math.acos(cos);
        return (ux * vy - uy * vx) < 0 ? -ang : ang;
    }

    public void scale(double x, double y) {
        AffineTransform transform = new AffineTransform();
        transform.scale(x, y);
        transform(transform);
    }

    public void close() {
        closePath();
    }
}
