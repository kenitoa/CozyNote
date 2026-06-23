package com.cozynote.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.cozynote.domain.Track;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public final class MusicPlayerService {
    public enum RepeatMode {
        RANDOM_REPEAT,
        TRACK_REPEAT,
        INFINITE_REPEAT
    }

    private static final List<String> AUDIO_EXTENSIONS = List.of(".mp3", ".wav", ".m4a", ".aac");
    private static final List<String> IMAGE_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".webp");

    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final DoubleProperty volume = new SimpleDoubleProperty(0.45);
    private final IntegerProperty elapsedSeconds = new SimpleIntegerProperty(0);
    private final IntegerProperty durationSeconds = new SimpleIntegerProperty(0);
    private final ObjectProperty<Track> currentTrack = new SimpleObjectProperty<>();
    private final ObjectProperty<RepeatMode> repeatMode = new SimpleObjectProperty<>(RepeatMode.INFINITE_REPEAT);
    private final List<Track> tracks;
    private final Path mediaRoot;
    private final Path playlistDir;
    private final Path profileDir;
    private MediaPlayer player;
    private int currentIndex;

    public MusicPlayerService() {
        mediaRoot = findMediaRoot();
        playlistDir = mediaRoot.resolve("playlist");
        profileDir = mediaRoot.resolve("profile");
        tracks = loadTracks(playlistDir, profileDir);
        if (tracks.isEmpty()) {
            tracks.add(new Track("No Track", "CozyNote", "", "", 0));
        }
        select(0);
    }

    public void play() {
        if (player == null) {
            return;
        }
        player.play();
        playing.set(true);
    }

    public void pause() {
        if (player != null) {
            player.pause();
        }
        playing.set(false);
    }

    public void stop() {
        if (player != null) {
            player.stop();
        }
        progress.set(0);
        elapsedSeconds.set(0);
        playing.set(false);
    }

    public void dispose() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
        playing.set(false);
    }

    public void next() {
        select(resolveNextIndex());
        play();
    }

    public void previous() {
        select(resolvePreviousIndex());
        play();
    }

    public void seekToProgress(double targetProgress) {
        if (player == null) {
            return;
        }
        double safeProgress = Math.max(0, Math.min(1, targetProgress));
        double totalSeconds = player.getTotalDuration().toSeconds();
        if (totalSeconds > 0) {
            player.seek(Duration.seconds(totalSeconds * safeProgress));
        }
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public IntegerProperty elapsedSecondsProperty() {
        return elapsedSeconds;
    }

    public IntegerProperty durationSecondsProperty() {
        return durationSeconds;
    }

    public ObjectProperty<Track> currentTrackProperty() {
        return currentTrack;
    }

    public ObjectProperty<RepeatMode> repeatModeProperty() {
        return repeatMode;
    }

    public RepeatMode repeatMode() {
        return repeatMode.get();
    }

    public void setRepeatMode(RepeatMode mode) {
        repeatMode.set(mode == null ? RepeatMode.INFINITE_REPEAT : mode);
    }

    public Track currentTrack() {
        return currentTrack.get();
    }

    public List<Track> tracks() {
        return List.copyOf(tracks);
    }

    public int currentIndex() {
        return currentIndex;
    }

    public void playTrack(int index) {
        if (index < 0 || index >= tracks.size()) {
            return;
        }
        select(index);
        play();
    }

    public void addTrack(Path sourcePath) throws IOException {
        if (sourcePath == null || !Files.isRegularFile(sourcePath)) {
            return;
        }
        Files.createDirectories(playlistDir);
        Path targetPath = uniqueTargetPath(playlistDir, sourcePath.getFileName().toString());
        Files.copy(sourcePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
        reloadTracksKeepingCurrent();
    }

    public void deleteTrack(int index) throws IOException {
        if (index < 0 || index >= tracks.size()) {
            return;
        }
        Track track = tracks.get(index);
        if (track.mediaUri() == null || track.mediaUri().isBlank()) {
            return;
        }
        Path trackPath = Path.of(URI.create(track.mediaUri()));
        Files.deleteIfExists(trackPath);
        deleteMatchingArtwork(track.title());
        reloadTracksAfterDeletion(index);
    }

    private void select(int index) {
        currentIndex = index;
        Track track = tracks.get(index);
        currentTrack.set(track);
        progress.set(0);
        elapsedSeconds.set(0);
        durationSeconds.set(track.durationSeconds());
        boolean resume = playing.get();
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
        if (track.mediaUri() == null || track.mediaUri().isBlank()) {
            return;
        }
        Media media = new Media(track.mediaUri());
        player = new MediaPlayer(media);
        player.volumeProperty().bind(volume);
        player.setOnReady(() -> {
            int seconds = (int) Math.round(media.getDuration().toSeconds());
            durationSeconds.set(seconds);
            currentTrack.set(new Track(track.title(), track.artist(), track.albumArtUri(), track.mediaUri(), seconds));
            if (resume) {
                play();
            }
        });
        player.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            double total = player.getTotalDuration().toSeconds();
            if (total > 0) {
                progress.set(newValue.toSeconds() / total);
            }
            elapsedSeconds.set((int) Math.round(newValue.toSeconds()));
        });
        player.setOnEndOfMedia(this::handleEndOfMedia);
    }

    private void handleEndOfMedia() {
        switch (repeatMode()) {
            case TRACK_REPEAT -> replayCurrentTrack();
            case RANDOM_REPEAT, INFINITE_REPEAT -> next();
        }
    }

    private void replayCurrentTrack() {
        if (player == null) {
            return;
        }
        player.seek(Duration.ZERO);
        player.play();
        playing.set(true);
    }

    private int resolveNextIndex() {
        return switch (repeatMode()) {
            case RANDOM_REPEAT -> randomIndexExcludingCurrent();
            case TRACK_REPEAT, INFINITE_REPEAT -> (currentIndex + 1) % tracks.size();
        };
    }

    private int resolvePreviousIndex() {
        return switch (repeatMode()) {
            case RANDOM_REPEAT -> randomIndexExcludingCurrent();
            case TRACK_REPEAT, INFINITE_REPEAT -> (currentIndex - 1 + tracks.size()) % tracks.size();
        };
    }

    private int randomIndexExcludingCurrent() {
        if (tracks.size() <= 1) {
            return currentIndex;
        }
        int randomIndex;
        do {
            randomIndex = ThreadLocalRandom.current().nextInt(tracks.size());
        } while (randomIndex == currentIndex);
        return randomIndex;
    }

    private void reloadTracksKeepingCurrent() {
        String currentUri = currentTrack() == null ? "" : currentTrack().mediaUri();
        reloadTracks(currentUri, currentIndex);
    }

    private void reloadTracksAfterDeletion(int deletedIndex) {
        int preferredIndex = Math.max(0, Math.min(deletedIndex, tracks.size() - 1));
        reloadTracks("", preferredIndex);
    }

    private void reloadTracks(String currentUri, int fallbackIndex) {
        List<Track> loaded = loadTracks(playlistDir, profileDir);
        if (loaded.isEmpty()) {
            loaded = new ArrayList<>(List.of(new Track("No Track", "CozyNote", "", "", 0)));
        }
        tracks.clear();
        tracks.addAll(loaded);

        int selectedIndex = Math.max(0, Math.min(fallbackIndex, tracks.size() - 1));
        if (currentUri != null && !currentUri.isBlank()) {
            for (int i = 0; i < tracks.size(); i++) {
                if (currentUri.equals(tracks.get(i).mediaUri())) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        select(selectedIndex);
    }

    private Path uniqueTargetPath(Path directory, String fileName) {
        Path candidate = directory.resolve(fileName);
        if (!Files.exists(candidate)) {
            return candidate;
        }
        String baseName = stripExtension(fileName);
        String extension = fileName.substring(baseName.length());
        int suffix = 2;
        while (true) {
            Path nextCandidate = directory.resolve(baseName + " (" + suffix + ")" + extension);
            if (!Files.exists(nextCandidate)) {
                return nextCandidate;
            }
            suffix++;
        }
    }

    private void deleteMatchingArtwork(String title) throws IOException {
        Files.createDirectories(profileDir);
        try (var paths = Files.list(profileDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> hasExtension(path, IMAGE_EXTENSIONS))
                    .filter(path -> stripExtension(path.getFileName().toString()).equalsIgnoreCase(title))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Ignore artwork deletion failures when the main track delete succeeded.
                        }
                    });
        }
    }

    private List<Track> loadTracks(Path playlistDir, Path profileDir) {
        try {
            Files.createDirectories(playlistDir);
            Files.createDirectories(profileDir);
            List<Path> audioFiles;
            try (var paths = Files.list(playlistDir)) {
                audioFiles = paths.filter(Files::isRegularFile)
                        .filter(path -> hasExtension(path, AUDIO_EXTENSIONS))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .toList();
            }
            List<Track> loaded = new ArrayList<>();
            for (Path audio : audioFiles) {
                String title = stripExtension(audio.getFileName().toString());
                Optional<Path> image = findMatchingImage(profileDir, title);
                loaded.add(new Track(title, "Local playlist", image.map(this::toUri).orElse(""), toUri(audio), 0));
            }
            return loaded;
        } catch (IOException exception) {
            return new ArrayList<>();
        }
    }

    private Path findMediaRoot() {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = new ArrayList<>();
        candidates.add(userDir);
        candidates.add(userDir.resolve("CozyNote"));
        candidates.add(userDir.resolve("src").resolve("main").resolve("resources"));
        if (userDir.getParent() != null) {
            candidates.add(userDir.getParent());
            candidates.add(userDir.getParent().resolve("CozyNote"));
            candidates.add(userDir.getParent().resolve("CozyNote").resolve("src").resolve("main").resolve("resources"));
        }
        addCodeSourceCandidates(candidates);
        for (Path candidate : candidates) {
            if (containsMedia(candidate)) {
                return candidate;
            }
        }
        return userDir;
    }

    private void addCodeSourceCandidates(List<Path> candidates) {
        try {
            Path location = Path.of(MusicPlayerService.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()).toAbsolutePath().normalize();
            candidates.add(location);
            candidates.add(location.resolve("src").resolve("main").resolve("resources"));
            Path cursor = location;
            for (int index = 0; index < 4 && cursor.getParent() != null; index++) {
                cursor = cursor.getParent();
                candidates.add(cursor);
                candidates.add(cursor.resolve("CozyNote"));
                candidates.add(cursor.resolve("src").resolve("main").resolve("resources"));
                candidates.add(cursor.resolve("CozyNote").resolve("src").resolve("main").resolve("resources"));
            }
        } catch (URISyntaxException | NullPointerException exception) {
            // Fallback candidates from user.dir are still enough for normal script and IDE launches.
        }
    }

    private boolean containsMedia(Path candidate) {
        return hasSupportedFile(candidate.resolve("playlist"), AUDIO_EXTENSIONS)
                || hasSupportedFile(candidate.resolve("profile"), IMAGE_EXTENSIONS);
    }

    private boolean hasSupportedFile(Path directory, List<String> extensions) {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (var paths = Files.list(directory)) {
            return paths.anyMatch(path -> Files.isRegularFile(path) && hasExtension(path, extensions));
        } catch (IOException exception) {
            return false;
        }
    }

    private Optional<Path> findMatchingImage(Path profileDir, String title) throws IOException {
        try (var paths = Files.list(profileDir)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> hasExtension(path, IMAGE_EXTENSIONS))
                    .filter(path -> stripExtension(path.getFileName().toString()).equalsIgnoreCase(title))
                    .findFirst();
        }
    }

    private boolean hasExtension(Path path, List<String> extensions) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return extensions.stream().anyMatch(name::endsWith);
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    private String toUri(Path path) {
        return path.toAbsolutePath().toUri().toString();
    }
}
