package com.cozynote.ui.editor;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

final class ResizableBlockSupport {
    private static final double EDGE_SIZE = 14;

    private ResizableBlockSupport() {
    }

    static void install(Region region, double minWidth, double minHeight, Runnable onResized) {
        final double[] start = new double[6];
        final Cursor[] resizeCursor = new Cursor[1];

        region.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            Cursor cursor = cursorFor(region, event.getX(), event.getY());
            resizeCursor[0] = cursor;
            region.setCursor(cursor);
        });

        region.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            Cursor cursor = cursorFor(region, event.getX(), event.getY());
            if (cursor == Cursor.DEFAULT) {
                resizeCursor[0] = null;
                return;
            }
            resizeCursor[0] = cursor;
            start[0] = event.getScreenX();
            start[1] = event.getScreenY();
            start[2] = region.getWidth();
            start[3] = region.getHeight();
            start[4] = region.getLayoutX();
            start[5] = region.getLayoutY();
            event.consume();
        });

        region.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            Cursor cursor = resizeCursor[0];
            if (cursor == null || cursor == Cursor.DEFAULT || cursor == Cursor.MOVE) {
                return;
            }
            double deltaX = event.getScreenX() - start[0];
            double deltaY = event.getScreenY() - start[1];
            double width = start[2];
            double height = start[3];
            double layoutX = start[4];
            double layoutY = start[5];

            if (cursor == Cursor.E_RESIZE || cursor == Cursor.NE_RESIZE || cursor == Cursor.SE_RESIZE) {
                width = start[2] + deltaX;
            }
            if (cursor == Cursor.W_RESIZE || cursor == Cursor.NW_RESIZE || cursor == Cursor.SW_RESIZE) {
                width = start[2] - deltaX;
                if (width >= minWidth) {
                    layoutX = start[4] + deltaX;
                }
            }
            if (cursor == Cursor.S_RESIZE || cursor == Cursor.SE_RESIZE || cursor == Cursor.SW_RESIZE) {
                height = start[3] + deltaY;
            }
            if (cursor == Cursor.N_RESIZE || cursor == Cursor.NE_RESIZE || cursor == Cursor.NW_RESIZE) {
                height = start[3] - deltaY;
                if (height >= minHeight) {
                    layoutY = start[5] + deltaY;
                }
            }

            region.setPrefWidth(Math.max(minWidth, width));
            region.setMinWidth(Math.max(minWidth, width));
            region.setMaxWidth(Math.max(minWidth, width));
            region.setPrefHeight(Math.max(minHeight, height));
            region.setMinHeight(Math.max(minHeight, height));
            region.setLayoutX(Math.max(0, layoutX));
            region.setLayoutY(Math.max(0, layoutY));
            constrainToParent(region, minWidth, minHeight);
            onResized.run();
            event.consume();
        });
    }

    private static void constrainToParent(Region region, double minWidth, double minHeight) {
        if (!(region.getParent() instanceof Region parent) || parent.getWidth() <= 0 || parent.getHeight() <= 0) {
            return;
        }
        double maxWidth = Math.max(minWidth, parent.getWidth());
        double maxHeight = Math.max(minHeight, parent.getHeight());
        double width = Math.min(region.getWidth(), maxWidth);
        double height = Math.min(region.getHeight(), maxHeight);
        double x = Math.max(0, Math.min(region.getLayoutX(), Math.max(0, parent.getWidth() - width)));
        double y = Math.max(0, Math.min(region.getLayoutY(), Math.max(0, parent.getHeight() - height)));

        if (x + width > parent.getWidth()) {
            width = Math.max(minWidth, parent.getWidth() - x);
        }
        if (y + height > parent.getHeight()) {
            height = Math.max(minHeight, parent.getHeight() - y);
        }

        region.setLayoutX(x);
        region.setLayoutY(y);
        region.setPrefWidth(width);
        region.setMinWidth(width);
        region.setMaxWidth(width);
        region.setPrefHeight(height);
        region.setMinHeight(height);
    }

    private static Cursor cursorFor(Region region, double x, double y) {
        boolean left = x <= EDGE_SIZE;
        boolean right = x >= region.getWidth() - EDGE_SIZE;
        boolean top = y <= EDGE_SIZE;
        boolean bottom = y >= region.getHeight() - EDGE_SIZE;
        if (top && left) {
            return Cursor.NW_RESIZE;
        }
        if (top && right) {
            return Cursor.NE_RESIZE;
        }
        if (bottom && left) {
            return Cursor.SW_RESIZE;
        }
        if (bottom && right) {
            return Cursor.SE_RESIZE;
        }
        if (left) {
            return Cursor.W_RESIZE;
        }
        if (right) {
            return Cursor.E_RESIZE;
        }
        if (top) {
            return Cursor.N_RESIZE;
        }
        if (bottom) {
            return Cursor.S_RESIZE;
        }
        return Cursor.DEFAULT;
    }
}
