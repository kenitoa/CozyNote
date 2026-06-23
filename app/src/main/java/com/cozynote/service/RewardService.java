package com.cozynote.service;

import com.cozynote.domain.BlockType;
import com.cozynote.domain.Note;
import com.cozynote.domain.NoteBlock;

public final class RewardService {
    public record Summary(int totalPoints, int completedTodos, int totalTodos) {
    }

    public int calculatePointsForTodoCompletion(NoteBlock block) {
        if (block.type() == BlockType.TODO && block.checked()) {
            return 1;
        }
        return 0;
    }

    public int calculateTotalPoints(Note note) {
        return summarize(note).totalPoints();
    }

    public int countCompletedTodos(Note note) {
        return summarize(note).completedTodos();
    }

    public Summary summarize(Note note) {
        if (note == null) {
            return new Summary(0, 0, 0);
        }
        int total = 0;
        int completed = 0;
        for (NoteBlock block : note.blocks()) {
            if (block.type() == BlockType.TODO) {
                total++;
                if (block.checked()) {
                    completed++;
                }
            }
        }
        int allDoneBonus = completed > 0 && completed == total ? 3 : 0;
        return new Summary(completed + allDoneBonus, completed, total);
    }
}
