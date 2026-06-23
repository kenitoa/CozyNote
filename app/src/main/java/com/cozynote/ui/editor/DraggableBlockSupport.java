package com.cozynote.ui.editor;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

final class DraggableBlockSupport {
    private static final double EDGE_SIZE = 16;

    private DraggableBlockSupport() {
    }

    static void install(Node node, Runnable onMoved) {
        final double[] start = new double[4];
        final boolean[] dragging = new boolean[1];
        node.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (canDrag(node, event)) {
                node.setCursor(Cursor.MOVE);
            } else if (!isResizeEdge(node, event)) {
                node.setCursor(Cursor.DEFAULT);
            }
        });
        node.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!canDrag(node, event)) {
                return;
            }
            start[0] = event.getScreenX();
            start[1] = event.getScreenY();
            start[2] = node.getLayoutX();
            start[3] = node.getLayoutY();
            dragging[0] = true;
            event.consume();
        });
        node.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!dragging[0]) {
                return;
            }
            node.setLayoutX(clamp(start[2] + event.getScreenX() - start[0], 0, maxLayoutX(node)));
            node.setLayoutY(clamp(start[3] + event.getScreenY() - start[1], 0, maxLayoutY(node)));
            event.consume();
        });
        node.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (!dragging[0]) {
                return;
            }
            dragging[0] = false;
            onMoved.run();
            event.consume();
        });
    }

    private static boolean canDrag(Node node, MouseEvent event) {
        if (event.getTarget() instanceof Control) {
            return false;
        }
        return !isResizeEdge(node, event);
    }

    private static boolean isResizeEdge(Node node, MouseEvent event) {
        double width = node.getBoundsInLocal().getWidth();
        double height = node.getBoundsInLocal().getHeight();
        return event.getX() <= EDGE_SIZE
                || event.getY() <= EDGE_SIZE
                || event.getX() >= width - EDGE_SIZE
                || event.getY() >= height - EDGE_SIZE;
    }

    private static double maxLayoutX(Node node) {
        if (node.getParent() instanceof Region parent) {
            return Math.max(0, parent.getWidth() - node.getBoundsInLocal().getWidth());
        }
        return Double.MAX_VALUE;
    }

    private static double maxLayoutY(Node node) {
        if (node.getParent() instanceof Region parent) {
            return Math.max(0, parent.getHeight() - node.getBoundsInLocal().getHeight());
        }
        return Double.MAX_VALUE;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
