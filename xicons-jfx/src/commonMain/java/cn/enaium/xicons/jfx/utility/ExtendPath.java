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

package cn.enaium.xicons.jfx.utility;

import javafx.collections.ObservableList;
import javafx.scene.shape.*;
import javafx.scene.transform.Affine;

public class ExtendPath extends Path {
    public void moveTo(double x, double y) {
        getElements().add(new MoveTo(x, y));
    }

    public void lineTo(double x, double y) {
        getElements().add(new LineTo(x, y));
    }

    public void quadTo(double controlX, double controlY, double x, double y) {
        getElements().add(new QuadCurveTo(controlX, controlY, x, y));
    }

    public void curveTo(double controlX1, double controlY1, double controlX2, double controlY2, double x, double y) {
        getElements().add(new CubicCurveTo(controlX1, controlY1, controlX2, controlY2, x, y));
    }

    public void arcTo(double radiusX, double radiusY, double xAxisRotation, boolean largeArcFlag, boolean sweepFlag, double x, double y) {
        getElements().add(new ArcTo(radiusX, radiusY, xAxisRotation, x, y, largeArcFlag, sweepFlag));
    }

    public void close() {
        getElements().add(new ClosePath());
    }

    public void horizontalLineTo(double x) {
        getElements().add(new LineTo(x, getCurrentY()));
    }

    public void verticalLineTo(double y) {
        getElements().add(new LineTo(getCurrentX(), y));
    }

    public void scale(double x, double y) {
        Affine affine = new Affine();
        affine.appendScale(x, y);
        getTransforms().add(affine);
    }

    private double getCurrentX() {
        ObservableList<PathElement> elements = getElements();
        if (elements.isEmpty()) {
            return 0.0;
        }
        PathElement last = elements.get(elements.size() - 1);
        if (last instanceof MoveTo) {
            return ((MoveTo) last).getX();
        } else if (last instanceof LineTo) {
            return ((LineTo) last).getX();
        } else if (last instanceof QuadCurveTo) {
            return ((QuadCurveTo) last).getX();
        } else if (last instanceof CubicCurveTo) {
            return ((CubicCurveTo) last).getX();
        } else if (last instanceof ArcTo) {
            return ((ArcTo) last).getX();
        }
        return 0.0;
    }

    private double getCurrentY() {
        ObservableList<PathElement> elements = getElements();
        if (elements.isEmpty()) {
            return 0.0;
        }
        PathElement last = elements.get(elements.size() - 1);
        if (last instanceof MoveTo) {
            return ((MoveTo) last).getY();
        } else if (last instanceof LineTo) {
            return ((LineTo) last).getY();
        } else if (last instanceof QuadCurveTo) {
            return ((QuadCurveTo) last).getY();
        } else if (last instanceof CubicCurveTo) {
            return ((CubicCurveTo) last).getY();
        } else if (last instanceof ArcTo) {
            return ((ArcTo) last).getY();
        }
        return 0.0;
    }
}