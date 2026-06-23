package com.cozynote.ui.editor;

import java.util.ArrayList;
import java.util.List;

import com.cozynote.domain.NoteBlock;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public final class ChartBlockView extends VBox {
    private static final double CHART_ASPECT_RATIO = 16.0 / 9.0;
    private static final double MIN_BLOCK_WIDTH = 560;
    private static final double MIN_BLOCK_HEIGHT = 360;
    private static final double MIN_CANVAS_WIDTH = 420;
    private static final double MIN_CANVAS_HEIGHT = 280;

    private final NoteBlock block;
    private final Runnable onChanged;
    private final GridPane dataGrid = new GridPane();
    private final Canvas preview = new Canvas(720, 405);
    private final ComboBox<String> typeSelector = new ComboBox<>();
    private final VBox side;
    private final HBox body;
    private String chartType = "막대";

    public ChartBlockView(NoteBlock block, Runnable onChanged) {
        this.block = block;
        this.onChanged = onChanged;

        typeSelector.getItems().setAll("막대", "선", "원형");
        typeSelector.setValue(chartType);
        typeSelector.setOnAction(event -> {
            chartType = typeSelector.getValue();
            renderChart();
        });

        Button addRow = new Button("+행");
        addRow.getStyleClass().add("toolbar-button");
        addRow.setOnAction(event -> {
            List<DataPoint> points = points();
            points.add(new DataPoint("Category " + (char) ('A' + points.size()), 100));
            save(points);
        });

        Button removeRow = new Button("-행");
        removeRow.getStyleClass().add("toolbar-button");
        removeRow.setOnAction(event -> {
            List<DataPoint> points = points();
            if (points.size() > 1) {
                points.remove(points.size() - 1);
                save(points);
            }
        });

        Button apply = new Button("적용");
        apply.getStyleClass().add("primary-button");
        apply.setOnAction(event -> showResultOnly());

        Button edit = new Button("편집");
        edit.getStyleClass().add("toolbar-button");
        edit.setOnAction(event -> showEditor());

        side = new VBox(10,
                new Label("차트 편집기"),
                new HBox(8, new Label("유형"), typeSelector),
                new Label("데이터"),
                dataGrid,
                new HBox(6, addRow, removeRow, apply));
        side.getStyleClass().add("chart-editor-side");

        VBox result = new VBox(8, edit, preview);
        result.getStyleClass().add("chart-result-pane");
        result.setMinWidth(MIN_CANVAS_WIDTH);
        result.setMaxWidth(Double.MAX_VALUE);
        result.widthProperty().addListener((observable, oldValue, width) -> resizePreview(result));

        body = new HBox(16, side, result);
        body.getStyleClass().add("chart-editor-body");
        HBox.setHgrow(result, Priority.ALWAYS);

        getChildren().add(body);
        getStyleClass().addAll("block-row", "chart-block", "chart-editor-block");
        setPadding(new Insets(12));
        double[] savedSize = savedSize(980, 620);
        setPrefWidth(savedSize[0]);
        setPrefHeight(savedSize[1]);
        setMinWidth(MIN_BLOCK_WIDTH);
        setMinHeight(MIN_BLOCK_HEIGHT);
        setMaxWidth(Double.MAX_VALUE);
        ResizableBlockSupport.install(this, MIN_BLOCK_WIDTH, MIN_BLOCK_HEIGHT, () -> {
            resizePreview(result);
            saveLayout();
            onChanged.run();
        });
        DraggableBlockSupport.install(this, () -> {
            saveLayout();
            onChanged.run();
        });
        widthProperty().addListener((observable, oldValue, width) -> resizePreview(result));
        heightProperty().addListener((observable, oldValue, height) -> resizePreview(result));
        renderDataGrid();
        resizePreview(result);
        renderChart();
    }

    private void showResultOnly() {
        side.setVisible(false);
        side.setManaged(false);
        resizePreview((Region) preview.getParent());
        renderChart();
    }

    private void showEditor() {
        side.setVisible(true);
        side.setManaged(true);
        renderChart();
    }

    private void resizePreview(Region result) {
        if (result == null) {
            return;
        }
        double sideWidth = side.isManaged() ? Math.max(side.getWidth(), side.getPrefWidth()) + body.getSpacing() : 0;
        double availableWidth = Math.max(MIN_CANVAS_WIDTH,
                getWidth() - sideWidth - getInsets().getLeft() - getInsets().getRight() - 28);
        double availableHeight = Math.max(MIN_CANVAS_HEIGHT,
                getHeight() - getInsets().getTop() - getInsets().getBottom() - 58);
        double widthFromHeight = availableHeight * CHART_ASPECT_RATIO;
        double canvasWidth = Math.min(availableWidth, widthFromHeight);
        double canvasHeight = canvasWidth / CHART_ASPECT_RATIO;
        if (canvasHeight > availableHeight) {
            canvasHeight = availableHeight;
            canvasWidth = canvasHeight * CHART_ASPECT_RATIO;
        }
        preview.setWidth(Math.max(MIN_CANVAS_WIDTH, canvasWidth));
        preview.setHeight(Math.max(MIN_CANVAS_HEIGHT, canvasHeight));
        renderChart();
    }

    private void saveLayout() {
        block.setLayout(getLayoutX(), getLayoutY(), Math.round(getWidth()), Math.round(getHeight()));
    }

    private double[] savedSize(double fallbackWidth, double fallbackHeight) {
        if (block.layoutWidth() > 0 && block.layoutHeight() > 0) {
            return new double[] {
                    Math.max(MIN_BLOCK_WIDTH, block.layoutWidth()),
                    Math.max(MIN_BLOCK_HEIGHT, block.layoutHeight())
            };
        }
        String layout = block.richTextHtml();
        if (layout == null || !layout.startsWith("size=") || !layout.contains("x")) {
            return new double[] {fallbackWidth, fallbackHeight};
        }
        String[] values = layout.substring("size=".length()).split("x", 2);
        try {
            double width = Math.max(MIN_BLOCK_WIDTH, Double.parseDouble(values[0]));
            double height = Math.max(MIN_BLOCK_HEIGHT, Double.parseDouble(values[1]));
            return new double[] {width, height};
        } catch (RuntimeException ignored) {
            return new double[] {fallbackWidth, fallbackHeight};
        }
    }

    private void renderDataGrid() {
        dataGrid.getChildren().clear();
        dataGrid.setHgap(0);
        dataGrid.setVgap(0);
        addHeader("Category", 0);
        addHeader("Value", 1);
        List<DataPoint> points = points();
        for (int i = 0; i < points.size(); i++) {
            DataPoint point = points.get(i);
            TextField label = cell(point.label());
            TextField value = cell(String.valueOf((int) point.value()));
            final int row = i;
            label.textProperty().addListener((observable, oldValue, newValue) -> updatePoint(row, newValue, value.getText()));
            value.textProperty().addListener((observable, oldValue, newValue) -> updatePoint(row, label.getText(), newValue));
            dataGrid.add(label, 0, i + 1);
            dataGrid.add(value, 1, i + 1);
        }
    }

    private void addHeader(String text, int column) {
        Label label = new Label(text);
        label.getStyleClass().add("chart-table-header");
        label.setMinWidth(120);
        dataGrid.add(label, column, 0);
    }

    private TextField cell(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("chart-table-cell");
        field.setPrefWidth(120);
        return field;
    }

    private void updatePoint(int index, String label, String value) {
        List<DataPoint> points = points();
        if (index >= points.size()) {
            return;
        }
        double number;
        try {
            number = Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            number = points.get(index).value();
        }
        points.set(index, new DataPoint(label, number));
        saveWithoutRender(points);
        renderChart();
    }

    private void save(List<DataPoint> points) {
        saveWithoutRender(points);
        renderDataGrid();
        renderChart();
    }

    private void saveWithoutRender(List<DataPoint> points) {
        List<String> lines = new ArrayList<>();
        for (DataPoint point : points) {
            lines.add(point.label() + "=" + point.value());
        }
        block.setText(String.join(System.lineSeparator(), lines));
        onChanged.run();
    }

    private void renderChart() {
        GraphicsContext graphics = preview.getGraphicsContext2D();
        graphics.clearRect(0, 0, preview.getWidth(), preview.getHeight());
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, preview.getWidth(), preview.getHeight());
        graphics.setStroke(Color.web("#d7dee6"));
        graphics.strokeRect(0.5, 0.5, preview.getWidth() - 1, preview.getHeight() - 1);
        List<DataPoint> points = points();
        if ("선".equals(chartType)) {
            renderLine(graphics, points);
        } else if ("원형".equals(chartType)) {
            renderPie(graphics, points);
        } else {
            renderBars(graphics, points);
        }
    }

    private void renderBars(GraphicsContext graphics, List<DataPoint> points) {
        double max = points.stream().mapToDouble(DataPoint::value).max().orElse(1);
        double canvasWidth = preview.getWidth();
        double canvasHeight = preview.getHeight();
        double left = Math.max(58, canvasWidth * 0.10);
        double right = Math.max(120, canvasWidth * 0.18);
        double top = Math.max(42, canvasHeight * 0.12);
        double bottom = canvasHeight - Math.max(54, canvasHeight * 0.12);
        double width = Math.max(120, canvasWidth - left - right);
        double height = Math.max(120, bottom - top);
        drawGrid(graphics, left, bottom, width, height);
        double slot = width / points.size();
        for (int i = 0; i < points.size(); i++) {
            double barHeight = height * points.get(i).value() / max;
            graphics.setFill(color(i));
            graphics.fillRect(left + i * slot + slot * 0.25, bottom - barHeight, slot * 0.5, barHeight);
            graphics.setFill(Color.web("#26323f"));
            graphics.fillText(points.get(i).label(), left + i * slot + 4, bottom + 22);
        }
        drawLegend(graphics, points, left + width + 28, top + 8);
    }

    private void renderLine(GraphicsContext graphics, List<DataPoint> points) {
        double max = points.stream().mapToDouble(DataPoint::value).max().orElse(1);
        double canvasWidth = preview.getWidth();
        double canvasHeight = preview.getHeight();
        double left = Math.max(58, canvasWidth * 0.10);
        double right = Math.max(120, canvasWidth * 0.18);
        double top = Math.max(42, canvasHeight * 0.12);
        double bottom = canvasHeight - Math.max(54, canvasHeight * 0.12);
        double width = Math.max(120, canvasWidth - left - right);
        double height = Math.max(120, bottom - top);
        drawGrid(graphics, left, bottom, width, height);
        graphics.setStroke(Color.web("#2496e8"));
        graphics.setLineWidth(3);
        for (int i = 1; i < points.size(); i++) {
            double x1 = left + (i - 1) * width / Math.max(1, points.size() - 1);
            double y1 = bottom - height * points.get(i - 1).value() / max;
            double x2 = left + i * width / Math.max(1, points.size() - 1);
            double y2 = bottom - height * points.get(i).value() / max;
            graphics.strokeLine(x1, y1, x2, y2);
        }
        drawLegend(graphics, points, left + width + 28, top + 8);
    }

    private void renderPie(GraphicsContext graphics, List<DataPoint> points) {
        double total = points.stream().mapToDouble(DataPoint::value).sum();
        double canvasWidth = preview.getWidth();
        double canvasHeight = preview.getHeight();
        double legendWidth = Math.max(130, canvasWidth * 0.18);
        double diameter = Math.max(120, Math.min(canvasHeight * 0.72, canvasWidth - legendWidth - 80));
        double x = Math.max(36, (canvasWidth - legendWidth - diameter) / 2);
        double y = Math.max(32, (canvasHeight - diameter) / 2);
        double start = 90;
        for (int i = 0; i < points.size(); i++) {
            double angle = 360 * points.get(i).value() / Math.max(1, total);
            graphics.setFill(color(i));
            graphics.fillArc(x, y, diameter, diameter, start, -angle, javafx.scene.shape.ArcType.ROUND);
            start -= angle;
        }
        drawLegend(graphics, points, x + diameter + 34, y + 20);
    }

    private void drawGrid(GraphicsContext graphics, double left, double bottom, double width, double height) {
        graphics.setStroke(Color.web("#d7dee6"));
        for (int i = 0; i <= 4; i++) {
            double y = bottom - height * i / 4;
            graphics.strokeLine(left, y, left + width, y);
        }
        graphics.setStroke(Color.web("#111827"));
        graphics.strokeLine(left, bottom, left + width, bottom);
    }

    private void drawLegend(GraphicsContext graphics, List<DataPoint> points, double x, double y) {
        for (int i = 0; i < points.size(); i++) {
            graphics.setFill(color(i));
            graphics.fillRect(x, y + i * 24, 14, 14);
            graphics.setFill(Color.web("#26323f"));
            graphics.fillText(points.get(i).label(), x + 22, y + 12 + i * 24);
        }
    }

    private Color color(int index) {
        Color[] colors = {Color.web("#2496e8"), Color.web("#f7b500"), Color.web("#9bdc22"), Color.web("#7f67d8")};
        return colors[index % colors.length];
    }

    private List<DataPoint> points() {
        List<DataPoint> points = new ArrayList<>();
        for (String line : (block.text() == null ? "" : block.text()).split("\\R")) {
            String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            try {
                points.add(new DataPoint(parts[0].trim(), Double.parseDouble(parts[1].trim())));
            } catch (NumberFormatException ignored) {
                // Skip malformed rows.
            }
        }
        if (points.isEmpty()) {
            points.add(new DataPoint("Category A", 500));
            points.add(new DataPoint("Category B", 520));
            points.add(new DataPoint("Category C", 540));
        }
        return points;
    }

    private record DataPoint(String label, double value) {
    }
}
