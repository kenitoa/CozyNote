package com.cozynote.ui.sidebar;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.cozynote.app.AppState;
import com.cozynote.domain.Category;
import com.cozynote.domain.Note;
import com.cozynote.service.CategoryService;
import com.cozynote.service.NoteService;
import com.cozynote.service.SearchService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class SidebarController {
    private final ObservableList<Note> notes = FXCollections.observableArrayList();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private NoteService noteService;
    private CategoryService categoryService;
    private final SearchService searchService = new SearchService();
    private AppState appState;
    private FilteredList<Note> filteredNotes;
    private String keyword = "";
    private Consumer<Note> noteSelectionHandler;
    private Consumer<String> statusHandler;
    private Runnable newMemoCommand;

    @FXML private TextField searchField;
    @FXML private Button newMemoButton;
    @FXML private ListView<Note> recentNoteListView;
    @FXML private ListView<Note> favoriteNoteListView;
    @FXML private ListView<Category> categoryListView;
    @FXML private Label sidebarStatusLabel;
    @FXML private Button settingsButton;

    @FXML
    private void initialize() {
        recentNoteListView.setCellFactory(list -> createRecentNoteCell(list));
        favoriteNoteListView.setCellFactory(list -> createFavoriteNoteCell(list));
        categoryListView.setItems(categories);
        categoryListView.setCellFactory(this::createCategoryCell);
        categoryListView.setContextMenu(createCategoryContextMenu(
                () -> categoryListView.getSelectionModel().getSelectedItem()));

        newMemoButton.setOnAction(event -> {
            if (newMemoCommand != null) {
                newMemoCommand.run();
            } else {
                createNewMemo();
            }
        });
        settingsButton.setOnAction(event -> openSettings());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            keyword = newValue == null ? "" : newValue;
            if (appState != null && !keyword.equals(appState.searchKeywordProperty().get())) {
                appState.searchKeywordProperty().set(keyword);
            }
            applySearch();
        });
        recentNoteListView.getSelectionModel().selectedItemProperty().addListener((observable, oldNote, note) -> {
            if (note != null && appState != null && appState.currentNoteProperty().get() != note) {
                appState.currentNoteProperty().set(note);
            }
            if (note != null && noteSelectionHandler != null) {
                noteSelectionHandler.accept(note);
            }
        });
        categoryListView.getSelectionModel().selectedItemProperty().addListener((observable, oldCategory, category) -> {
            if (appState != null && category != null) {
                appState.selectedCategoryProperty().set(category.name());
            }
            applySearch();
        });
        bindNoteLists(notes);
    }

    public void setServices(NoteService noteService, CategoryService categoryService) {
        this.noteService = noteService;
        this.categoryService = categoryService;
        loadCategories();
        loadNotes();
    }

    public void setAppState(AppState appState) {
        this.appState = appState;
        bindNoteLists(appState.notes());
        appState.searchKeywordProperty().bindBidirectional(searchField.textProperty());
        appState.searchKeywordProperty().addListener((observable, oldValue, newValue) -> {
            keyword = newValue == null ? "" : newValue;
            applySearch();
        });
        appState.selectedCategoryProperty().addListener((observable, oldValue, newValue) -> applySearch());
        appState.currentNoteProperty().addListener((observable, oldNote, note) -> {
            if (note != null && recentNoteListView.getSelectionModel().getSelectedItem() != note) {
                recentNoteListView.getSelectionModel().select(note);
            }
        });
        if (!notes.isEmpty() && appState.notes().isEmpty()) {
            appState.notes().setAll(notes);
        }
        applySearch();
    }

    public void setNoteSelectionHandler(Consumer<Note> noteSelectionHandler) {
        this.noteSelectionHandler = noteSelectionHandler;
    }

    public void setStatusHandler(Consumer<String> statusHandler) {
        this.statusHandler = statusHandler;
    }

    public void setNewMemoCommand(Runnable newMemoCommand) {
        this.newMemoCommand = newMemoCommand;
    }

    public void showSavedNote(Note savedNote) {
        if (savedNote == null) {
            return;
        }
        noteList().removeIf(note -> note.id().equals(savedNote.id()));
        noteList().add(0, savedNote);
        if (appState != null) {
            appState.currentNoteProperty().set(savedNote);
            appState.dirtyProperty().set(false);
            appState.savingProperty().set(false);
        }
        applySearch();
        recentNoteListView.getSelectionModel().select(savedNote);
        setStatus("메모를 저장했습니다.");
    }

    private NoteListCell createRecentNoteCell(ListView<Note> list) {
        NoteListCell cell = new NoteListCell();
        cell.setContextMenu(createRecentNoteContextMenu(cell));
        cell.setOnContextMenuRequested(event -> {
            if (!cell.isEmpty()) {
                list.getSelectionModel().select(cell.getIndex());
            }
        });
        return cell;
    }

    private ListCell<Category> createCategoryCell(ListView<Category> list) {
        ListCell<Category> cell = new ListCell<>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                setText(empty || category == null ? null : "  ".repeat(category.level()) + category.name());
            }
        };
        cell.setContextMenu(createCategoryContextMenu(cell::getItem));
        cell.setOnContextMenuRequested(event -> {
            if (!cell.isEmpty()) {
                list.getSelectionModel().select(cell.getIndex());
            }
        });
        return cell;
    }

    private NoteListCell createFavoriteNoteCell(ListView<Note> list) {
        NoteListCell cell = new NoteListCell();
        cell.setContextMenu(createFavoriteNoteContextMenu(cell));
        cell.setOnContextMenuRequested(event -> {
            if (!cell.isEmpty()) {
                list.getSelectionModel().select(cell.getIndex());
            }
        });
        return cell;
    }

    private ContextMenu createRecentNoteContextMenu(NoteListCell cell) {
        MenuItem favoriteItem = new MenuItem();
        favoriteItem.setOnAction(event -> {
            Note note = cell.getItem();
            if (note != null) {
                setFavorite(note, !note.favorite());
            }
        });

        MenuItem deleteItem = new MenuItem("메모 삭제");
        deleteItem.setOnAction(event -> deleteNote(cell.getItem()));

        ContextMenu menu = new ContextMenu(favoriteItem, deleteItem);
        menu.setOnShowing(event -> {
            Note note = cell.getItem();
            boolean disabled = note == null;
            favoriteItem.setDisable(disabled);
            deleteItem.setDisable(disabled);
            favoriteItem.setText(note != null && note.favorite() ? "즐겨찾기 해제" : "즐겨찾기 추가");
        });
        return menu;
    }

    private ContextMenu createFavoriteNoteContextMenu(NoteListCell cell) {
        MenuItem removeFavoriteItem = new MenuItem("즐겨찾기 삭제");
        removeFavoriteItem.setOnAction(event -> {
            Note note = cell.getItem();
            if (note != null) {
                setFavorite(note, false);
            }
        });

        MenuItem deleteItem = new MenuItem("메모 삭제");
        deleteItem.setOnAction(event -> deleteNote(cell.getItem()));

        ContextMenu menu = new ContextMenu(removeFavoriteItem, deleteItem);
        menu.setOnShowing(event -> {
            boolean disabled = cell.getItem() == null;
            removeFavoriteItem.setDisable(disabled);
            deleteItem.setDisable(disabled);
        });
        return menu;
    }

    private ContextMenu createCategoryContextMenu(Supplier<Category> categorySupplier) {
        MenuItem createCategoryItem = new MenuItem("카테고리 생성");
        createCategoryItem.setOnAction(event -> createCategory(0));

        MenuItem createSubCategoryItem = new MenuItem("하위 카테고리 생성");
        createSubCategoryItem.setOnAction(event -> createCategory(1));

        MenuItem deleteCategoryItem = new MenuItem("카테고리 삭제");
        deleteCategoryItem.setOnAction(event -> deleteCategory(categorySupplier.get()));

        ContextMenu contextMenu = new ContextMenu(createCategoryItem, createSubCategoryItem, deleteCategoryItem);
        contextMenu.setOnShowing(event -> deleteCategoryItem.setDisable(categorySupplier.get() == null));
        return contextMenu;
    }

    private void createCategory(int level) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(level == 0 ? "카테고리 생성" : "하위 카테고리 생성");
        dialog.setHeaderText(null);
        dialog.setContentText("카테고리 이름");
        dialog.showAndWait()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .ifPresent(name -> {
                    try {
                        Category category = categoryService.createCategory(name, level, categories.size());
                        loadCategories();
                        categoryListView.getSelectionModel().select(category);
                        setStatus(level == 0 ? "카테고리를 만들었습니다." : "하위 카테고리를 만들었습니다.");
                    } catch (IOException exception) {
                        setStatus("카테고리 저장 실패: " + exception.getMessage());
                    }
                });
    }

    private void deleteCategory(Category category) {
        if (category == null) {
            return;
        }
        if (categoryService.isDefaultCategory(category)) {
            setStatus("기본 카테고리는 삭제할 수 없습니다.");
            return;
        }
        if (!confirm("카테고리 삭제", category.name() + " 카테고리를 삭제할까요? 메모는 모든 메모로 이동합니다.")) {
            return;
        }
        try {
            categoryService.deleteCategory(category);
            loadCategories();
            loadNotes();
            setStatus("카테고리를 삭제했습니다.");
        } catch (IllegalArgumentException exception) {
            setStatus(exception.getMessage());
        } catch (IOException exception) {
            setStatus("카테고리 삭제 실패: " + exception.getMessage());
        }
    }

    private void setFavorite(Note note, boolean favorite) {
        if (note == null) {
            return;
        }
        note.setFavorite(favorite);
        try {
            noteService.save(note);
            refreshNoteInState(note);
            applySearch();
            setStatus(favorite ? "즐겨찾기에 추가했습니다." : "즐겨찾기에서 삭제했습니다.");
        } catch (IOException exception) {
            setStatus("즐겨찾기 저장 실패: " + exception.getMessage());
        }
    }

    private void deleteNote(Note note) {
        if (note == null) {
            return;
        }
        if (!confirm("메모 삭제", note.title() + " 메모를 삭제할까요?")) {
            return;
        }
        try {
            noteService.delete(note);
            noteList().removeIf(item -> item.id().equals(note.id()));
            if (appState != null && appState.currentNoteProperty().get() == note) {
                appState.currentNoteProperty().set(noteList().isEmpty() ? null : noteList().get(0));
            }
            applySearch();
            loadCategories();
            setStatus("메모를 삭제했습니다.");
        } catch (IOException exception) {
            setStatus("메모 삭제 실패: " + exception.getMessage());
        }
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public void createNewMemo() {
        try {
            Note note = noteService.createNewNote();
            noteList().add(0, note);
            if (appState != null) {
                appState.currentNoteProperty().set(note);
                appState.dirtyProperty().set(false);
            }
            applySearch();
            recentNoteListView.getSelectionModel().select(note);
            setStatus("새 메모를 만들었습니다.");
        } catch (IOException exception) {
            setStatus("새 메모 저장 실패: " + exception.getMessage());
        }
    }

    public void reloadNotes() {
        loadNotes();
    }

    private void loadNotes() {
        try {
            noteList().setAll(noteService.listNotes());
            applySearch();
            if (appState != null && !noteList().isEmpty() && appState.currentNoteProperty().get() == null) {
                appState.currentNoteProperty().set(noteList().get(0));
            }
            setStatus(noteList().isEmpty() ? "저장된 메모가 없습니다." : "메모 " + noteList().size() + "개");
        } catch (IOException exception) {
            noteList().clear();
            applySearch();
            setStatus("DB를 읽지 못했습니다. 새 메모는 계속 작성할 수 있습니다.");
        }
    }

    private void loadCategories() {
        try {
            categories.setAll(categoryService.listCategories());
        } catch (IOException exception) {
            setStatus("카테고리를 읽지 못했습니다.");
        }
    }

    private void applySearch() {
        if (filteredNotes == null) {
            return;
        }
        Predicate<Note> textMatches = searchService.predicate(keyword);
        String selectedCategory = appState == null ? "모든 메모" : appState.selectedCategoryProperty().get();
        filteredNotes.setPredicate(note -> textMatches.test(note) && matchesCategory(note, selectedCategory));
    }

    private boolean matchesCategory(Note note, String selectedCategory) {
        if (selectedCategory == null || selectedCategory.isBlank() || "모든 메모".equals(selectedCategory)) {
            return true;
        }
        return note.category() != null && selectedCategory.equals(note.category().name());
    }

    private void bindNoteLists(ObservableList<Note> source) {
        filteredNotes = new FilteredList<>(source);
        recentNoteListView.setItems(filteredNotes);
        favoriteNoteListView.setItems(new FilteredList<>(filteredNotes, Note::isFavorite));
        applySearch();
    }

    private ObservableList<Note> noteList() {
        return appState == null ? notes : appState.notes();
    }

    private void refreshNoteInState(Note note) {
        ObservableList<Note> list = noteList();
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(note.id())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            list.set(index, note);
        }
        if (appState != null && appState.currentNoteProperty().get() == note) {
            appState.currentNoteProperty().set(note);
        }
    }

    private void setStatus(String text) {
        if (sidebarStatusLabel != null) {
            sidebarStatusLabel.setText(text);
        }
        if (statusHandler != null) {
            statusHandler.accept(text);
        }
    }

    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cozynote/views/settings.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("설정");
            dialog.setScene(new Scene(root, 560, 680));
            dialog.showAndWait();
        } catch (IOException exception) {
            setStatus("설정 화면을 열지 못했습니다.");
        }
    }
}
