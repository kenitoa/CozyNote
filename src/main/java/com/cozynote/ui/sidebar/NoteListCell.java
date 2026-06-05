package com.cozynote.ui.sidebar;

import java.time.format.DateTimeFormatter;

import com.cozynote.domain.Note;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class NoteListCell extends ListCell<Note> {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private final Label titleLabel = new Label();
    private final Label updatedAtLabel = new Label();
    private final Label pinLabel = new Label();
    private final Label favoriteLabel = new Label();
    private final VBox textBox = new VBox(2, titleLabel, updatedAtLabel);
    private final HBox badgesBox = new HBox(6, pinLabel, favoriteLabel);
    private final HBox row = new HBox(10, textBox, badgesBox);

    public NoteListCell() {
        titleLabel.getStyleClass().add("note-name");
        updatedAtLabel.getStyleClass().add("note-time");
        pinLabel.getStyleClass().add("pin-label");
        favoriteLabel.getStyleClass().add("favorite-label");
        badgesBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("note-list-row");
    }

    @Override
    protected void updateItem(Note note, boolean empty) {
        super.updateItem(note, empty);
        if (empty || note == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        titleLabel.setText(note.title());
        updatedAtLabel.setText(note.updatedAt().format(TIME_FORMAT));
        pinLabel.setText(note.pinned() ? "고정" : "");
        favoriteLabel.setText(note.favorite() ? "★" : "");
        setGraphic(row);
    }
}
