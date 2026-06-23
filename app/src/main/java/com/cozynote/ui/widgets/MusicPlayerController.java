package com.cozynote.ui.widgets;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.cozynote.domain.Track;
import com.cozynote.service.MusicPlayerService;
import com.cozynote.service.SettingsService;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class MusicPlayerController {
    private static final String FAVORITE_TRACKS_KEY = "music.favoriteTracks";

    @FXML private VBox widgetRoot;
    @FXML private ImageView albumImageView;
    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label elapsedLabel;
    @FXML private Label durationLabel;
    @FXML private Button playButton;
    @FXML private Button trackListButton;
    @FXML private Button repeatModeButton;
    @FXML private Slider progressSlider;
    @FXML private Slider volumeSlider;
    @FXML private StackPane miniGameContainer;

    private final MusicPlayerService service = new MusicPlayerService();
    private final CafeGamePanel cafeGamePanel = new CafeGamePanel();
    private SettingsService settingsService;

    @FXML
    private void initialize() {
        miniGameContainer.getChildren().setAll(cafeGamePanel);
        volumeSlider.valueProperty().bindBidirectional(service.volumeProperty());
        volumeSlider.valueProperty().addListener((observable, oldValue, value) -> {
            if (settingsService != null) {
                settingsService.setDouble("music.volume", value.doubleValue());
            }
        });
        service.currentTrackProperty().addListener((observable, oldValue, track) -> renderTrack(track));
        service.playingProperty().addListener((observable, oldValue, playing) ->
                playButton.setText(playing ? "\u23F8" : "\u25B6"));
        service.elapsedSecondsProperty().addListener((observable, oldValue, value) ->
                elapsedLabel.setText(format(value.intValue())));
        service.durationSecondsProperty().addListener((observable, oldValue, value) ->
                durationLabel.setText(format(value.intValue())));
        service.repeatModeProperty().addListener((observable, oldValue, mode) -> renderRepeatMode(mode));
        service.progressProperty().addListener((observable, oldValue, value) -> {
            if (!progressSlider.isValueChanging()) {
                progressSlider.setValue(value.doubleValue());
            }
        });
        progressSlider.valueChangingProperty().addListener((observable, wasChanging, changing) -> {
            if (!changing) {
                service.seekToProgress(progressSlider.getValue());
            }
        });
        trackListButton.setTooltip(new Tooltip("\uC804\uCCB4 \uACE1 \uB9AC\uC2A4\uD2B8"));
        renderRepeatMode(service.repeatMode());
        renderTrack(service.currentTrack());
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
        volumeSlider.setValue(settingsService.getDouble("music.volume", volumeSlider.getValue()));
        service.setRepeatMode(parseRepeatMode(settingsService.getString("music.repeatMode", "INFINITE_REPEAT")));
        applySettings();
    }

    public void applySettings() {
        if (settingsService == null) {
            return;
        }
        if (settingsService.getBoolean("music.autoPlay", false)) {
            service.play();
        } else {
            service.pause();
        }
    }

    public void dispose() {
        cafeGamePanel.stop();
        service.dispose();
    }

    @FXML
    private void togglePlay() {
        if (service.playingProperty().get()) {
            service.pause();
        } else {
            service.play();
        }
    }

    @FXML
    private void previousTrack() {
        service.previous();
    }

    @FXML
    private void openTrackList() {
        final boolean[] favoritesOnly = {true};
        final boolean[] sortAscending = {true};
        final Runnable[] refreshAction = new Runnable[1];

        TextField searchField = new TextField();
        searchField.setPromptText("\uAC80\uC0C9");
        searchField.getStyleClass().add("track-library-search-field");

        Button favoriteButton = new Button("\uC790\uC8FC \uB4E3\uB294 \uACE1");
        favoriteButton.getStyleClass().addAll("track-library-chip", "track-library-filter-button");

        Button recentButton = new Button("\uCD5C\uADFC");
        recentButton.getStyleClass().addAll("track-library-chip", "track-library-filter-button");

        Button addTrackButton = new Button("\uFF0B \uACE1 \uCD94\uAC00\uD558\uAE30");
        addTrackButton.getStyleClass().add("track-library-add-button");

        Button deleteTrackButton = new Button("\uC0AD\uC81C");
        deleteTrackButton.getStyleClass().addAll("track-library-text-button", "track-library-danger-button");
        deleteTrackButton.setManaged(false);
        deleteTrackButton.setVisible(false);

        Button sortButton = new Button();
        sortButton.getStyleClass().add("track-library-sort-button");

        ListView<Track> trackListView = new ListView<>();
        trackListView.getStyleClass().add("track-library-list");
        trackListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Track track, boolean empty) {
                super.updateItem(track, empty);
                if (empty || track == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setGraphic(createTrackRow(track, trackListView));
            }
        });

        Label statusLabel = new Label("\uC120\uD0DD\uD55C \uACE1\uC744 \uBC14\uB85C \uC7AC\uC0DD\uD558\uAC70\uB098 \uAD00\uB9AC\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.");
        statusLabel.getStyleClass().add("track-library-status");

        addTrackButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("\uACE1 \uCD94\uAC00\uD558\uAE30");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Audio", "*.mp3", "*.wav", "*.m4a", "*.aac"));
            File source = chooser.showOpenDialog(currentWindow());
            if (source == null) {
                return;
            }
            try {
                service.addTrack(source.toPath());
                if (refreshAction[0] != null) {
                    refreshAction[0].run();
                }
                statusLabel.setText("\uACE1\uC744 \uCD94\uAC00\uD588\uC2B5\uB2C8\uB2E4: " + source.getName());
            } catch (IOException exception) {
                showError("\uACE1 \uCD94\uAC00 \uC2E4\uD328", exception.getMessage());
            }
        });

        deleteTrackButton.setOnAction(event -> {
            Track selectedTrack = trackListView.getSelectionModel().getSelectedItem();
            if (selectedTrack == null) {
                statusLabel.setText("\uC0AD\uC81C\uD560 \uACE1\uC744 \uC120\uD0DD\uD574 \uC8FC\uC138\uC694.");
                return;
            }
            if (!confirm("\uACE1 \uC0AD\uC81C",
                    "\"" + selectedTrack.title() + "\" \uACE1\uC744 \uC7AC\uC0DD\uBAA9\uB85D\uC5D0\uC11C \uC0AD\uC81C\uD560\uAE4C\uC694?")) {
                return;
            }
            try {
                removeFavoriteTrack(selectedTrack);
                int serviceIndex = indexOfTrack(selectedTrack);
                if (serviceIndex >= 0) {
                    service.deleteTrack(serviceIndex);
                }
                if (refreshAction[0] != null) {
                    refreshAction[0].run();
                }
                statusLabel.setText("\uACE1\uC744 \uC0AD\uC81C\uD588\uC2B5\uB2C8\uB2E4: " + selectedTrack.title());
            } catch (IOException exception) {
                showError("\uACE1 \uC0AD\uC81C \uC2E4\uD328", exception.getMessage());
            }
        });

        Button playSelectedButton = new Button("\u25B6 \uC7AC\uC0DD");
        playSelectedButton.getStyleClass().addAll("track-library-footer-button", "track-library-primary-button");
        playSelectedButton.setOnAction(event -> {
            Track selectedTrack = selectedOrCurrentTrack(trackListView);
            int serviceIndex = indexOfTrack(selectedTrack);
            if (serviceIndex >= 0) {
                service.playTrack(serviceIndex);
                ((Stage) playSelectedButton.getScene().getWindow()).close();
            }
        });

        Button closeButton = new Button("\uB2EB\uAE30");
        closeButton.getStyleClass().addAll("track-library-footer-button", "track-library-secondary-button");
        closeButton.setOnAction(event -> ((Stage) closeButton.getScene().getWindow()).close());

        favoriteButton.setOnAction(event -> {
            favoritesOnly[0] = true;
            if (refreshAction[0] != null) {
                refreshAction[0].run();
            }
            statusLabel.setText("\uC790\uC8FC \uB4E3\uB294 \uACE1\uB9CC \uD45C\uC2DC\uD569\uB2C8\uB2E4.");
        });

        recentButton.setOnAction(event -> {
            favoritesOnly[0] = false;
            if (refreshAction[0] != null) {
                refreshAction[0].run();
            }
            statusLabel.setText("\uC804\uCCB4 \uACE1 \uB9AC\uC2A4\uD2B8\uB97C \uD45C\uC2DC\uD569\uB2C8\uB2E4.");
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (refreshAction[0] != null) {
                refreshAction[0].run();
            }
        });

        trackListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedTrack) -> {
            boolean hasSelection = selectedTrack != null;
            deleteTrackButton.setManaged(hasSelection);
            deleteTrackButton.setVisible(hasSelection);
        });

        trackListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Track selectedTrack = trackListView.getSelectionModel().getSelectedItem();
                int serviceIndex = indexOfTrack(selectedTrack);
                if (serviceIndex >= 0) {
                    service.playTrack(serviceIndex);
                    ((Stage) trackListView.getScene().getWindow()).close();
                }
            }
        });

        Label modalTitleLabel = new Label("\uB0B4 \uB77C\uC774\uBE0C\uB7EC\uB9AC");
        modalTitleLabel.getStyleClass().add("track-library-title");
        Label titleIconLabel = new Label("\u266B");
        titleIconLabel.getStyleClass().add("track-library-title-icon");
        Label subtitleLabel = new Label("\uC804\uCCB4 \uACE1 \uB9AC\uC2A4\uD2B8");
        subtitleLabel.getStyleClass().add("track-library-subtitle");

        HBox titleRow = new HBox(12, modalTitleLabel, titleIconLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(6, titleRow, subtitleLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        HBox topBar = new HBox(titleBox);
        topBar.getStyleClass().add("track-library-topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label searchIcon = new Label("\u2315");
        searchIcon.getStyleClass().add("track-library-search-icon");
        HBox searchBox = new HBox(12, searchIcon, searchField);
        searchBox.getStyleClass().add("track-library-search-box");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPrefWidth(360);
        searchBox.setMaxWidth(360);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox toolbarRow = new HBox(12, favoriteButton, addTrackButton, recentButton, searchBox);
        toolbarRow.getStyleClass().add("track-library-toolbar");
        toolbarRow.setAlignment(Pos.CENTER_LEFT);

        sortButton.setOnAction(event -> {
            sortAscending[0] = !sortAscending[0];
            if (refreshAction[0] != null) {
                refreshAction[0].run();
            }
            statusLabel.setText(sortAscending[0]
                    ? "\uACE1 \uC81C\uBAA9 \uC624\uB984\uCC28\uC21C\uC73C\uB85C \uC815\uB82C\uD588\uC2B5\uB2C8\uB2E4."
                    : "\uACE1 \uC81C\uBAA9 \uB0B4\uB9BC\uCC28\uC21C\uC73C\uB85C \uC815\uB82C\uD588\uC2B5\uB2C8\uB2E4.");
        });

        HBox utilityRow = new HBox(10, deleteTrackButton);
        utilityRow.getStyleClass().add("track-library-utility-row");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        utilityRow.getChildren().addAll(spacer, sortButton);

        VBox listPanel = new VBox(10, trackListView);
        listPanel.getStyleClass().add("track-library-panel");
        VBox.setVgrow(trackListView, Priority.ALWAYS);

        HBox footerRow = new HBox(12);
        footerRow.getStyleClass().add("track-library-footer");
        Label infoIcon = new Label("\u24D8");
        infoIcon.getStyleClass().add("track-library-info-icon");
        footerRow.getChildren().addAll(infoIcon, statusLabel);
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        footerRow.getChildren().addAll(footerSpacer, closeButton, playSelectedButton);
        footerRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(30, topBar, toolbarRow, utilityRow, listPanel, footerRow);
        content.getStyleClass().add("track-library-content");
        VBox.setVgrow(listPanel, Priority.ALWAYS);

        VBox root = new VBox(content);
        root.getStyleClass().add("track-library-window");

        refreshAction[0] = () -> refreshTrackListView(
                trackListView,
                service.currentTrack(),
                favoriteButton,
                recentButton,
                searchField.getText(),
                favoritesOnly[0],
                sortAscending[0],
                sortButton);

        Scene scene = new Scene(root, 1320, 1060);
        if (widgetRoot.getScene() != null) {
            scene.getStylesheets().setAll(widgetRoot.getScene().getStylesheets());
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (currentWindow() != null) {
            dialog.initOwner(currentWindow());
        }
        dialog.setTitle("\uC804\uCCB4 \uACE1 \uB9AC\uC2A4\uD2B8");
        dialog.setScene(scene);

        refreshAction[0].run();
        trackListView.getSelectionModel().clearSelection();
        dialog.showAndWait();
    }

    @FXML
    private void nextTrack() {
        service.next();
    }

    @FXML
    private void cycleRepeatMode() {
        MusicPlayerService.RepeatMode nextMode = switch (service.repeatMode()) {
            case RANDOM_REPEAT -> MusicPlayerService.RepeatMode.TRACK_REPEAT;
            case TRACK_REPEAT -> MusicPlayerService.RepeatMode.INFINITE_REPEAT;
            case INFINITE_REPEAT -> MusicPlayerService.RepeatMode.RANDOM_REPEAT;
        };
        updateRepeatMode(nextMode);
    }

    private void renderTrack(Track track) {
        if (track == null) {
            return;
        }
        titleLabel.setText(track.title());
        artistLabel.setText(track.artist());
        elapsedLabel.setText(format(service.elapsedSecondsProperty().get()));
        durationLabel.setText(format(service.durationSecondsProperty().get()));
        if (track.albumArtUri() == null || track.albumArtUri().isBlank()) {
            albumImageView.setImage(null);
        } else {
            albumImageView.setImage(new Image(track.albumArtUri(), true));
        }
    }

    private void refreshTrackListView(ListView<Track> trackListView, Track preferredTrack,
                                      Button favoriteButton, Button recentButton,
                                      String query, boolean favoritesOnly,
                                      boolean sortAscending, Button sortButton) {
        List<Track> filteredTracks = service.tracks().stream()
                .filter(track -> !favoritesOnly || isFavoriteTrack(track))
                .filter(track -> matchesTrackQuery(track, query))
                .sorted(trackComparator(sortAscending))
                .toList();
        if (favoritesOnly && filteredTracks.isEmpty()) {
            filteredTracks = service.tracks().stream()
                    .filter(track -> matchesTrackQuery(track, query))
                    .sorted(trackComparator(sortAscending))
                    .toList();
        }
        trackListView.getItems().setAll(filteredTracks);
        int indexToSelect = Math.max(0, Math.min(service.currentIndex(), trackListView.getItems().size() - 1));
        if (preferredTrack != null && preferredTrack.mediaUri() != null) {
            for (int index = 0; index < trackListView.getItems().size(); index++) {
                Track candidate = trackListView.getItems().get(index);
                if (preferredTrack.mediaUri().equals(candidate.mediaUri())) {
                    indexToSelect = index;
                    break;
                }
            }
        }
        if (!trackListView.getItems().isEmpty()) {
            trackListView.getSelectionModel().select(indexToSelect);
            trackListView.scrollTo(indexToSelect);
        }
        updateLibraryFilterButtons(favoriteButton, recentButton, favoritesOnly);
        updateSortButton(sortButton, sortAscending);
        trackListView.refresh();
    }

    private Comparator<Track> trackComparator(boolean ascending) {
        Comparator<Track> comparator = Comparator
                .comparing((Track track) -> track.title().toLowerCase())
                .thenComparing(track -> track.artist().toLowerCase());
        return ascending ? comparator : comparator.reversed();
    }

    private void updateSortButton(Button sortButton, boolean ascending) {
        sortButton.setText(ascending ? "\u2637  \uC815\uB82C \u2191" : "\u2637  \uC815\uB82C \u2193");
        sortButton.setTooltip(new Tooltip(ascending
                ? "\uACE1 \uC81C\uBAA9 \uC624\uB984\uCC28\uC21C"
                : "\uACE1 \uC81C\uBAA9 \uB0B4\uB9BC\uCC28\uC21C"));
    }

    private void updateLibraryFilterButtons(Button favoriteButton, Button recentButton, boolean favoritesOnly) {
        favoriteButton.getStyleClass().remove("track-library-chip-active");
        recentButton.getStyleClass().remove("track-library-chip-active");
        if (favoritesOnly) {
            favoriteButton.getStyleClass().add("track-library-chip-active");
        } else {
            recentButton.getStyleClass().add("track-library-chip-active");
        }
    }

    private boolean isFavoriteTrack(Track track) {
        return track != null && favoriteTrackUris().contains(track.mediaUri());
    }

    private void toggleFavoriteTrack(Track track) {
        if (track == null || track.mediaUri() == null || track.mediaUri().isBlank()) {
            return;
        }
        Set<String> favoriteUris = favoriteTrackUris();
        if (!favoriteUris.add(track.mediaUri())) {
            favoriteUris.remove(track.mediaUri());
        }
        saveFavoriteTrackUris(favoriteUris);
    }

    private void removeFavoriteTrack(Track track) {
        if (track == null || track.mediaUri() == null || track.mediaUri().isBlank()) {
            return;
        }
        Set<String> favoriteUris = favoriteTrackUris();
        if (favoriteUris.remove(track.mediaUri())) {
            saveFavoriteTrackUris(favoriteUris);
        }
    }

    private Set<String> favoriteTrackUris() {
        Set<String> favoriteUris = new LinkedHashSet<>();
        if (settingsService == null) {
            return favoriteUris;
        }
        String raw = settingsService.getString(FAVORITE_TRACKS_KEY, "");
        for (String line : raw.split("\\R")) {
            String value = line.trim();
            if (!value.isEmpty()) {
                favoriteUris.add(value);
            }
        }
        return favoriteUris;
    }

    private void saveFavoriteTrackUris(Set<String> favoriteUris) {
        if (settingsService == null) {
            return;
        }
        settingsService.setString(FAVORITE_TRACKS_KEY, String.join(System.lineSeparator(), favoriteUris));
    }

    private Stage currentWindow() {
        return widgetRoot != null && widgetRoot.getScene() != null && widgetRoot.getScene().getWindow() instanceof Stage stage
                ? stage
                : null;
    }

    private StackPane createArtwork(Track track) {
        StackPane artwork = new StackPane();
        artwork.getStyleClass().add("track-library-artwork");
        if (track.albumArtUri() != null && !track.albumArtUri().isBlank()) {
            ImageView coverImageView = new ImageView(new Image(track.albumArtUri(), true));
            coverImageView.setFitWidth(70);
            coverImageView.setFitHeight(70);
            coverImageView.setPreserveRatio(false);
            coverImageView.setSmooth(true);
            artwork.getChildren().add(coverImageView);
            return artwork;
        }
        Label placeholder = new Label(track.title().isBlank()
                ? "\u266B"
                : track.title().substring(0, 1).toUpperCase());
        placeholder.getStyleClass().add("track-library-artwork-placeholder");
        artwork.getChildren().add(placeholder);
        return artwork;
    }

    private HBox createTrackRow(Track track, ListView<Track> trackListView) {
        Label title = new Label(track.title());
        title.getStyleClass().add("track-library-item-title");
        Label subtitle = new Label(track.artist());
        subtitle.getStyleClass().add("track-library-item-subtitle");
        VBox textBox = new VBox(8, title, subtitle);
        textBox.setAlignment(Pos.CENTER_LEFT);

        Label duration = new Label(format(track.durationSeconds()));
        duration.getStyleClass().add("track-library-item-duration");

        Button rowPlayButton = new Button("\u25B6");
        rowPlayButton.getStyleClass().add("track-library-row-play-button");
        rowPlayButton.setOnAction(event -> {
            int serviceIndex = indexOfTrack(track);
            if (serviceIndex >= 0) {
                service.playTrack(serviceIndex);
                trackListView.getSelectionModel().select(track);
            }
        });

        HBox actions = new HBox(28, duration, rowPlayButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(28, createArtwork(track), textBox, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        row.getStyleClass().add("track-library-row");
        return row;
    }

    private boolean matchesTrackQuery(Track track, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalizedQuery = query.trim().toLowerCase();
        return track.title().toLowerCase().contains(normalizedQuery)
                || track.artist().toLowerCase().contains(normalizedQuery);
    }

    private int indexOfTrack(Track track) {
        if (track == null || track.mediaUri() == null || track.mediaUri().isBlank()) {
            return -1;
        }
        List<Track> tracks = service.tracks();
        for (int index = 0; index < tracks.size(); index++) {
            if (track.mediaUri().equals(tracks.get(index).mediaUri())) {
                return index;
            }
        }
        return -1;
    }

    private Track selectedOrCurrentTrack(ListView<Track> trackListView) {
        Track selectedTrack = trackListView.getSelectionModel().getSelectedItem();
        return selectedTrack != null ? selectedTrack : service.currentTrack();
    }

    private String format(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    private void updateRepeatMode(MusicPlayerService.RepeatMode mode) {
        service.setRepeatMode(mode);
        if (settingsService != null) {
            settingsService.setString("music.repeatMode", mode.name());
        }
    }

    private void renderRepeatMode(MusicPlayerService.RepeatMode mode) {
        MusicPlayerService.RepeatMode safeMode =
                mode == null ? MusicPlayerService.RepeatMode.INFINITE_REPEAT : mode;
        repeatModeButton.setText(repeatModeSymbol(safeMode));
        repeatModeButton.setTooltip(new Tooltip(repeatModeDescription(safeMode)));
    }

    private MusicPlayerService.RepeatMode parseRepeatMode(String value) {
        try {
            return MusicPlayerService.RepeatMode.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return MusicPlayerService.RepeatMode.INFINITE_REPEAT;
        }
    }

    private String repeatModeSymbol(MusicPlayerService.RepeatMode mode) {
        return switch (mode) {
            case RANDOM_REPEAT -> "\uD83D\uDD00";
            case TRACK_REPEAT -> "\u27F21";
            case INFINITE_REPEAT -> "\u221E";
        };
    }

    private String repeatModeDescription(MusicPlayerService.RepeatMode mode) {
        return switch (mode) {
            case RANDOM_REPEAT -> "\uB79C\uB364 \uBC18\uBCF5";
            case TRACK_REPEAT -> "\uD2B8\uB799 \uBC18\uBCF5";
            case INFINITE_REPEAT -> "\uBB34\uD55C \uBC18\uBCF5";
        };
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "\uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4." : message);
        alert.showAndWait();
    }
}
