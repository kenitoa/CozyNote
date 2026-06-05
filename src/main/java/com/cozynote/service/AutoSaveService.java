package com.cozynote.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import com.cozynote.domain.Note;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

public final class AutoSaveService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("a h:mm");

    private final PauseTransition delay = new PauseTransition(Duration.millis(800));
    private final NoteService noteService;
    private final Consumer<String> statusConsumer;
    private Note currentNote;

    public AutoSaveService(NoteService noteService, Consumer<String> statusConsumer) {
        this.noteService = noteService;
        this.statusConsumer = statusConsumer;
        delay.setOnFinished(event -> saveNow());
    }

    public void requestSave(Note note) {
        currentNote = note;
        statusConsumer.accept("입력 중: 저장 대기...");
        delay.playFromStart();
    }

    public void saveImmediately(Note note) {
        currentNote = note;
        delay.stop();
        saveNow();
    }

    private void saveNow() {
        if (currentNote == null) {
            return;
        }
        statusConsumer.accept("저장 중...");
        try {
            noteService.save(currentNote);
            statusConsumer.accept("저장 완료: " + LocalDateTime.now().format(TIME_FORMAT));
        } catch (IOException exception) {
            statusConsumer.accept("오류 발생: 저장 실패");
        }
    }
}
