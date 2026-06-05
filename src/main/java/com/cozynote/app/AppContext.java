package com.cozynote.app;

import java.nio.file.Path;

import com.cozynote.persistence.SqliteNoteRepository;
import com.cozynote.service.BackupService;
import com.cozynote.service.CategoryService;
import com.cozynote.service.NoteService;
import com.cozynote.service.ProgressService;
import com.cozynote.service.RewardService;
import com.cozynote.service.SettingsService;

public final class AppContext {
    private final AppState state = new AppState();
    private final CommandRegistry commands = new CommandRegistry();
    private final NoteService noteService;
    private final CategoryService categoryService;
    private final ProgressService progressService;
    private final SettingsService settingsService;
    private final RewardService rewardService = new RewardService();
    private final BackupService backupService = new BackupService();

    public AppContext() {
        Path database = Path.of(System.getProperty("user.home"), ".cozynote", "cozynote.db");
        SqliteNoteRepository repository = new SqliteNoteRepository(database);
        noteService = new NoteService(repository);
        categoryService = new CategoryService(repository);
        progressService = new ProgressService(repository);
        settingsService = new SettingsService(repository);
    }

    public AppState state() {
        return state;
    }

    public CommandRegistry commands() {
        return commands;
    }

    public NoteService noteService() {
        return noteService;
    }

    public CategoryService categoryService() {
        return categoryService;
    }

    public ProgressService progressService() {
        return progressService;
    }

    public SettingsService settingsService() {
        return settingsService;
    }

    public RewardService rewardService() {
        return rewardService;
    }

    public BackupService backupService() {
        return backupService;
    }
}
