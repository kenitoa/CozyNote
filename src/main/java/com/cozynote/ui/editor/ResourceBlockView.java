package com.cozynote.ui.editor;

import com.cozynote.domain.BlockType;
import com.cozynote.domain.NoteBlock;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class ResourceBlockView extends HBox {
    public ResourceBlockView(NoteBlock block, Runnable onChanged) {
        Label icon = new Label(iconFor(block.type()));
        icon.getStyleClass().add("block-icon");

        TextField textField = new TextField(block.text());
        textField.setPromptText(promptFor(block.type()));
        textField.getStyleClass().add("block-field");
        BlockStyle.apply(textField, block);
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            block.setText(newValue);
            onChanged.run();
        });

        getChildren().addAll(icon, textField);
        getStyleClass().addAll("block-row", "resource-block");
        setSpacing(8);
        BlockStyle.applyIndent(this, block);
        HBox.setHgrow(textField, Priority.ALWAYS);
    }

    private String iconFor(BlockType type) {
        return switch (type) {
            case IMAGE -> "▧";
            case LINK -> "🔗";
            case ATTACHMENT -> "⌕";
            case AUDIO -> "◉";
            default -> "□";
        };
    }

    private String promptFor(BlockType type) {
        return switch (type) {
            case IMAGE -> "이미지 파일 경로";
            case LINK -> "링크 URL";
            case ATTACHMENT -> "첨부 파일 경로";
            case AUDIO -> "음성 메모 또는 오디오 파일";
            default -> "내용";
        };
    }
}
