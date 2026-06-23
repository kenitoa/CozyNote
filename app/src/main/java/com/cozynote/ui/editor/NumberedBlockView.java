package com.cozynote.ui.editor;

import com.cozynote.domain.NoteBlock;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class NumberedBlockView extends HBox {
    public NumberedBlockView(NoteBlock block, Runnable onChanged) {
        Label number = new Label((block.orderIndex() + 1) + ".");
        number.getStyleClass().add("block-icon");

        TextField textField = new TextField(block.text());
        textField.setPromptText("번호 항목 입력");
        textField.getStyleClass().add("block-field");
        BlockStyle.apply(textField, block);
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            block.setText(newValue);
            onChanged.run();
        });

        getChildren().addAll(number, textField);
        getStyleClass().addAll("block-row", "numbered-block");
        setSpacing(8);
        BlockStyle.applyIndent(this, block);
        HBox.setHgrow(textField, Priority.ALWAYS);
    }
}
