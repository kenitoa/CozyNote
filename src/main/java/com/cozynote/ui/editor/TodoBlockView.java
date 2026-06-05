package com.cozynote.ui.editor;

import com.cozynote.domain.NoteBlock;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class TodoBlockView extends HBox {
    public TodoBlockView(NoteBlock block, Runnable onChanged) {
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(block.checked());
        checkBox.selectedProperty().addListener((observable, oldValue, checked) -> {
            block.setChecked(checked);
            onChanged.run();
        });

        TextField textField = new TextField(block.text());
        textField.setPromptText("할 일 입력");
        textField.getStyleClass().add("block-field");
        BlockStyle.apply(textField, block);
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            block.setText(newValue);
            onChanged.run();
        });

        getChildren().addAll(checkBox, textField);
        getStyleClass().addAll("block-row", "todo-block");
        setSpacing(8);
        BlockStyle.applyIndent(this, block);
        HBox.setHgrow(textField, Priority.ALWAYS);
    }
}
