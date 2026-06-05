package com.cozynote.ui.editor;

import com.cozynote.domain.NoteBlock;

import javafx.geometry.Pos;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

final class BlockStyle {
    private BlockStyle() {
    }

    static void apply(TextField textField, NoteBlock block) {
        textField.setAlignment(alignment(block.alignment()));
        textField.setStyle("""
                -fx-font-weight: %s;
                -fx-font-style: %s;
                -fx-font-size: %dpx;
                -fx-text-fill: %s;
                -fx-background-color: %s;
                -fx-strikethrough: %s;
                -fx-border-color: %s;
                -fx-border-width: %s;
                """.formatted(
                block.bold() ? "bold" : "normal",
                block.italic() ? "italic" : "normal",
                block.superscript() || block.subscript() ? 11 : 14,
                block.textColor(),
                block.highlightColor(),
                block.strike() ? "true" : "false",
                block.underline() ? "transparent transparent #5f5145 transparent" : "transparent",
                block.underline() ? "0 0 1 0" : "0"));
    }

    static void apply(TextArea textArea, NoteBlock block) {
        textArea.setStyle("""
                -fx-font-weight: %s;
                -fx-font-style: %s;
                -fx-font-size: %dpx;
                -fx-text-fill: %s;
                -fx-background-color: %s;
                -fx-strikethrough: %s;
                -fx-text-alignment: %s;
                """.formatted(
                block.bold() ? "bold" : "normal",
                block.italic() ? "italic" : "normal",
                block.superscript() || block.subscript() ? 11 : 14,
                block.textColor(),
                block.highlightColor(),
                block.strike() ? "true" : "false",
                textAlignment(block.alignment())));
    }

    static void applyIndent(HBox row, NoteBlock block) {
        row.setTranslateX(block.indentLevel() * 22.0);
    }

    private static Pos alignment(String alignment) {
        return switch (alignment) {
            case "CENTER" -> Pos.CENTER;
            case "RIGHT" -> Pos.CENTER_RIGHT;
            default -> Pos.CENTER_LEFT;
        };
    }

    private static String textAlignment(String alignment) {
        return switch (alignment) {
            case "CENTER" -> "center";
            case "RIGHT" -> "right";
            default -> "left";
        };
    }
}
