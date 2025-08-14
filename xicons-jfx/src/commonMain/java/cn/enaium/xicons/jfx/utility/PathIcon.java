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

/**
 * @author Enaium
 */
public abstract class PathIcon extends Group {
    public int width = 24;
    public int height = 24;
    public Color color = Color.BLACK;

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public ObservableList<Node> getChildren() {
        ExtendPath path = new ExtendPath();
        path.setStroke(null);
        path.setFill(color);
        path.setScaleX(width / 24.0);
        path.setScaleY(height / 24.0);
        final ExtendPath extendPath = path();
        path.getElements().addAll(extendPath.getElements());
        path.getTransforms().addAll(extendPath.getTransforms());
        final ObservableList<Node> children = super.getChildren();
        if (children.stream().noneMatch(node -> node instanceof ExtendPath)) {
            children.add(path);
        }
        return children;
    }

    public abstract ExtendPath path();
}
