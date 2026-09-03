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
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * @author Enaium
 */
public abstract class PathIcon extends Group {
    public int width = 24;
    public int height = 24;
    public Color color = Color.BLACK;

    private ObservableList<Node> unmodifiableChildren;

    {
        // Build eagerly so the children list is populated before the Parent
        // constructor caches its unmodifiableChildren view; otherwise
        // getChildrenUnmodifiable() would observe an empty group.
        build();
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    private void build() {
        ObservableList<Node> children = super.getChildren();
        children.clear();
        for (ExtendPath shape : paths()) {
            ExtendPath path = new ExtendPath();
            path.setScaleX(width / 24.0);
            path.setScaleY(height / 24.0);
            boolean stroked = shape.getStrokeWidth() > 0;
            path.setFill(shape.isFillEnabled() ? color : null);
            path.setStroke(stroked ? color : null);
            path.setStrokeWidth(shape.getStrokeWidth());
            path.setStrokeLineCap(shape.getStrokeLineCap());
            path.setStrokeLineJoin(shape.getStrokeLineJoin());
            path.setFillRule(shape.getFillRule());
            path.getElements().addAll(shape.getElements());
            path.getTransforms().addAll(shape.getTransforms());
            children.add(path);
        }
    }

    @Override
    public ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    @Override
    public ObservableList<Node> getChildrenUnmodifiable() {
        if (unmodifiableChildren == null) {
            unmodifiableChildren = javafx.collections.FXCollections.unmodifiableObservableList(getChildren());
        }
        return unmodifiableChildren;
    }

    public abstract List<ExtendPath> paths();

    public ExtendPath path() {
        return paths().get(0);
    }
}
