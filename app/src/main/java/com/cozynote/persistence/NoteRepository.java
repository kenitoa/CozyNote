package com.cozynote.persistence;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.cozynote.domain.Note;

public interface NoteRepository {
    List<Note> findAll() throws IOException;

    Optional<Note> findById(String id) throws IOException;

    void save(Note note) throws IOException;

    void deleteById(String id) throws IOException;
}
