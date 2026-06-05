package com.cozynote.ui.editor;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public final class DividerBlockView extends HBox {
    public DividerBlockView() {
        Region line = new Region();
        line.getStyleClass().add("divider-line");
        getChildren().add(line);
        getStyleClass().addAll("block-row", "divider-block");
        HBox.setHgrow(line, Priority.ALWAYS);
    }
}
