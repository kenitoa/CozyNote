package com.cozynote.ui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cozynote.domain.NoteBlock;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class TableBlockView extends VBox {
    private static final double MIN_WIDTH = 260;
    private static final double MIN_HEIGHT = 180;

    private final NoteBlock block;
    private final Runnable onChanged;
    private final GridPane grid = new GridPane();
    private HBox controls;
    private double dragStartX;
    private double dragStartY;
    private double startWidth;
    private double startHeight;

    public TableBlockView(NoteBlock block, Runnable onChanged) {
        this.block = block;
        this.onChanged = onChanged;

        Button addRowButton = new Button("+행");
        addRowButton.getStyleClass().add("toolbar-button");
        addRowButton.setOnAction(event -> {
            List<List<String>> rows = rows();
            int columns = Math.max(2, columnCount(rows));
            rows.add(blankRow(columns));
            saveRows(rows);
        });

        Button addColumnButton = new Button("+열");
        addColumnButton.getStyleClass().add("toolbar-button");
        addColumnButton.setOnAction(event -> {
            List<List<String>> rows = rows();
            if (rows.isEmpty()) {
                rows.add(blankRow(1));
            }
            for (List<String> row : rows) {
                row.add("");
            }
            saveRows(rows);
        });

        Button removeRowButton = new Button("-행");
        removeRowButton.getStyleClass().add("toolbar-button");
        removeRowButton.setOnAction(event -> {
            List<List<String>> rows = rows();
            if (rows.size() > 1) {
                rows.remove(rows.size() - 1);
                saveRows(rows);
            }
        });

        Button removeColumnButton = new Button("-열");
        removeColumnButton.getStyleClass().add("toolbar-button");
        removeColumnButton.setOnAction(event -> {
            List<List<String>> rows = rows();
            int columns = columnCount(rows);
            if (columns > 1) {
                for (List<String> row : rows) {
                    if (!row.isEmpty()) {
                        row.remove(row.size() - 1);
                    }
                }
                saveRows(rows);
            }
        });

        controls = new HBox(6, addRowButton, addColumnButton, removeRowButton, removeColumnButton);
        getChildren().addAll(controls, grid);
        getStyleClass().addAll("block-row", "table-block");
        setSpacing(6);
        double[] savedSize = savedSize(560, 360);
        setPrefWidth(savedSize[0]);
        setPrefHeight(savedSize[1]);
        setMinWidth(MIN_WIDTH);
        setMinHeight(MIN_HEIGHT);
        ResizableBlockSupport.install(this, MIN_WIDTH, MIN_HEIGHT, () -> {
            resizeToFrame();
            saveLayout();
            onChanged.run();
        });
        DraggableBlockSupport.install(this, () -> {
            saveLayout();
            onChanged.run();
        });
        renderGrid();
        widthProperty().addListener((observable, oldValue, newValue) -> resizeToFrame());
        heightProperty().addListener((observable, oldValue, newValue) -> resizeToFrame());
    }

    private void renderGrid() {
        grid.getChildren().clear();
        grid.setHgap(0);
        grid.setVgap(0);
        List<List<String>> rows = rows();
        int columns = Math.max(2, columnCount(rows));
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            while (row.size() < columns) {
                row.add("");
            }
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                TextField cell = new TextField(row.get(columnIndex));
                cell.getStyleClass().add("table-cell-field");
                cell.setPrefWidth(140);
                cell.setPrefHeight(76);
                final int r = rowIndex;
                final int c = columnIndex;
                cell.textProperty().addListener((observable, oldValue, newValue) -> {
                    List<List<String>> current = rows();
                    current.get(r).set(c, newValue);
                    block.setText(joinRows(current));
                    onChanged.run();
                });
                installResizeHandlers(cell, r, c);
                grid.add(cell, columnIndex, rowIndex);
                GridPane.setHgrow(cell, Priority.ALWAYS);
            }
        }
        VBox.setMargin(grid, new Insets(2, 0, 0, 0));
        resizeToFrame();
    }

    private void resizeToFrame() {
        List<List<String>> rows = rows();
        int rowCount = Math.max(1, rows.size());
        int columnCount = Math.max(2, columnCount(rows));
        double availableWidth = Math.max(96, getWidth() - getInsets().getLeft() - getInsets().getRight());
        double controlsHeight = controls == null ? 0 : controls.getBoundsInParent().getHeight() + getSpacing() + 8;
        double availableHeight = Math.max(36, getHeight() - controlsHeight - getInsets().getTop() - getInsets().getBottom());
        double cellWidth = Math.max(48, availableWidth / columnCount);
        double cellHeight = Math.max(32, availableHeight / rowCount);
        for (javafx.scene.Node node : grid.getChildren()) {
            if (node instanceof TextField cell) {
                cell.setPrefWidth(cellWidth);
                cell.setPrefHeight(cellHeight);
            }
        }
    }

    private void saveLayout() {
        block.setLayout(getLayoutX(), getLayoutY(), Math.round(getWidth()), Math.round(getHeight()));
    }

    private double[] savedSize(double fallbackWidth, double fallbackHeight) {
        if (block.layoutWidth() > 0 && block.layoutHeight() > 0) {
            return new double[] {
                    Math.max(MIN_WIDTH, block.layoutWidth()),
                    Math.max(MIN_HEIGHT, block.layoutHeight())
            };
        }
        String layout = block.richTextHtml();
        if (layout == null || !layout.startsWith("size=") || !layout.contains("x")) {
            return new double[] {fallbackWidth, fallbackHeight};
        }
        String[] values = layout.substring("size=".length()).split("x", 2);
        try {
            double width = Math.max(MIN_WIDTH, Double.parseDouble(values[0]));
            double height = Math.max(MIN_HEIGHT, Double.parseDouble(values[1]));
            return new double[] {width, height};
        } catch (RuntimeException ignored) {
            return new double[] {fallbackWidth, fallbackHeight};
        }
    }

    private void installResizeHandlers(TextField cell, int rowIndex, int columnIndex) {
        cell.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            boolean rightEdge = event.getX() > cell.getWidth() - 6;
            boolean bottomEdge = event.getY() > cell.getHeight() - 6;
            if (rightEdge && bottomEdge) {
                cell.setCursor(Cursor.SE_RESIZE);
            } else if (rightEdge) {
                cell.setCursor(Cursor.E_RESIZE);
            } else if (bottomEdge) {
                cell.setCursor(Cursor.S_RESIZE);
            } else {
                cell.setCursor(Cursor.TEXT);
            }
        });
        cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (cell.getCursor() == Cursor.E_RESIZE || cell.getCursor() == Cursor.S_RESIZE || cell.getCursor() == Cursor.SE_RESIZE) {
                dragStartX = event.getScreenX();
                dragStartY = event.getScreenY();
                startWidth = cell.getWidth();
                startHeight = cell.getHeight();
                event.consume();
            }
        });
        cell.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            Cursor cursor = cell.getCursor();
            if (cursor == Cursor.E_RESIZE || cursor == Cursor.SE_RESIZE) {
                resizeColumn(columnIndex, Math.max(48, startWidth + event.getScreenX() - dragStartX));
            }
            if (cursor == Cursor.S_RESIZE || cursor == Cursor.SE_RESIZE) {
                resizeRow(rowIndex, Math.max(32, startHeight + event.getScreenY() - dragStartY));
            }
            if (cursor == Cursor.E_RESIZE || cursor == Cursor.S_RESIZE || cursor == Cursor.SE_RESIZE) {
                event.consume();
            }
        });
    }

    private void resizeColumn(int columnIndex, double width) {
        for (javafx.scene.Node node : grid.getChildren()) {
            Integer column = GridPane.getColumnIndex(node);
            if (node instanceof TextField cell && (column == null ? 0 : column) == columnIndex) {
                cell.setPrefWidth(width);
            }
        }
    }

    private void resizeRow(int rowIndex, double height) {
        for (javafx.scene.Node node : grid.getChildren()) {
            Integer row = GridPane.getRowIndex(node);
            if (node instanceof TextField cell && (row == null ? 0 : row) == rowIndex) {
                cell.setPrefHeight(height);
            }
        }
    }

    private List<List<String>> rows() {
        String text = block.text();
        if (text == null || text.isBlank()) {
            text = "항목\t값\n\t";
        }
        List<List<String>> rows = new ArrayList<>();
        for (String line : text.split("\\R", -1)) {
            rows.add(new ArrayList<>(Arrays.asList(line.split("\\t", -1))));
        }
        return rows;
    }

    private int columnCount(List<List<String>> rows) {
        return rows.stream().mapToInt(List::size).max().orElse(2);
    }

    private List<String> blankRow(int columns) {
        List<String> row = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            row.add("");
        }
        return row;
    }

    private void saveRows(List<List<String>> rows) {
        block.setText(joinRows(rows));
        renderGrid();
        onChanged.run();
    }

    private String joinRows(List<List<String>> rows) {
        List<String> lines = new ArrayList<>();
        for (List<String> row : rows) {
            lines.add(String.join("\t", row));
        }
        return String.join(System.lineSeparator(), lines);
    }
}
