package com.cozynote.app;

import com.cozynote.domain.Note;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class AppState {
    private final ObservableList<Note> notes = FXCollections.observableArrayList();
    private final ObjectProperty<Note> currentNote = new SimpleObjectProperty<>();
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private final BooleanProperty saving = new SimpleBooleanProperty(false);
    private final BooleanProperty leftSidebarVisible = new SimpleBooleanProperty(true);
    private final BooleanProperty rightPanelVisible = new SimpleBooleanProperty(true);
    private final StringProperty searchKeyword = new SimpleStringProperty("");
    private final StringProperty selectedCategory = new SimpleStringProperty("모든 메모");
    private final DoubleProperty editorZoom = new SimpleDoubleProperty(1.0);

    public ObservableList<Note> notes() {
        return notes;
    }

    public ObjectProperty<Note> currentNoteProperty() {
        return currentNote;
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public BooleanProperty savingProperty() {
        return saving;
    }

    public BooleanProperty leftSidebarVisibleProperty() {
        return leftSidebarVisible;
    }

    public BooleanProperty rightPanelVisibleProperty() {
        return rightPanelVisible;
    }

    public StringProperty searchKeywordProperty() {
        return searchKeyword;
    }

    public StringProperty selectedCategoryProperty() {
        return selectedCategory;
    }

    public DoubleProperty editorZoomProperty() {
        return editorZoom;
    }
}
