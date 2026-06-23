package com.cozynote.service;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.cozynote.domain.Note;
import com.cozynote.domain.NoteBlock;

public final class SearchService {
    public List<Note> search(List<Note> notes, String keyword) {
        Predicate<Note> predicate = predicate(keyword);
        return notes.stream().filter(predicate).toList();
    }

    public Predicate<Note> predicate(String keyword) {
        String normalized = normalize(keyword);
        if (normalized.isEmpty()) {
            return note -> true;
        }
        return note -> matches(note, normalized);
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
    }

    private boolean matches(Note note, String keyword) {
        return contains(note.title(), keyword)
                || contains(note.category().name(), keyword)
                || Boolean.toString(note.favorite()).contains(keyword)
                || Boolean.toString(note.pinned()).contains(keyword)
                || note.blocks().stream().map(NoteBlock::text).anyMatch(text -> contains(text, keyword));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
