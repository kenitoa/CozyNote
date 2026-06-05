package com.cozynote.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.cozynote.persistence.SqliteNoteRepository;

public final class ProgressService {
    private final SqliteNoteRepository repository;
    private final Map<String, String> lastSavedValues = new HashMap<>();

    public ProgressService(SqliteNoteRepository repository) {
        this.repository = repository;
    }

    public synchronized void saveProgress(String key, String value) throws IOException {
        String normalizedValue = value == null ? "" : value;
        if (normalizedValue.equals(lastSavedValues.get(key))) {
            return;
        }
        repository.saveProgress(key, normalizedValue);
        lastSavedValues.put(key, normalizedValue);
    }
}
