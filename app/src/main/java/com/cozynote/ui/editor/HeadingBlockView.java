package com.cozynote.ui.editor;

import com.cozynote.domain.NoteBlock;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class HeadingBlockView extends HBox {
    public HeadingBlockView(NoteBlock block, Runnable onChanged) {
        Label icon = new Label("☀");
        icon.getStyleClass().add("block-icon");

        TextField textField = new TextField(block.text());
        textField.setPromptText("제목");
        textField.getStyleClass().add("heading-field");
        BlockStyle.apply(textField, block);
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            block.setText(newValue);
            onChanged.run();
        });

        getChildren().addAll(icon, textField);
        getStyleClass().addAll("block-row", "heading-block");
        setSpacing(8);
        BlockStyle.applyIndent(this, block);
        HBox.setHgrow(textField, Priority.ALWAYS);
    }
}
