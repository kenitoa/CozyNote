package com.cozynote.service;

import java.io.IOException;
import java.util.List;

import com.cozynote.domain.BlockType;
import com.cozynote.domain.Category;
import com.cozynote.domain.Note;
import com.cozynote.domain.NoteBlock;
import com.cozynote.persistence.NoteRepository;

public final class NoteService {
    private final NoteRepository repository;

    public NoteService(NoteRepository repository) {
        this.repository = repository;
    }

    public List<Note> listNotes() throws IOException {
        return repository.findAll();
    }

    public Note createNote() {
        return new Note("새 메모", "", new Category("모든 메모", 0), false);
    }

    public Note createNewNote() throws IOException {
        Note note = new Note("새 메모", "", new Category("모든 메모", 0), false);
        note.replaceBlocks(List.of(new NoteBlock(BlockType.TEXT, "", 0)));
        repository.save(note);
        return note;
    }

    public void save(Note note) throws IOException {
        repository.save(note);
    }

    public void saveAll(List<Note> notes) throws IOException {
        for (Note note : notes) {
            repository.save(note);
        }
    }

    public void delete(Note note) throws IOException {
        repository.deleteById(note.id());
    }
}
