package com.cozynote.domain;

public record Category(String name, int noteCount, int level) {
    public Category(String name, int noteCount) {
        this(name, noteCount, 0);
    }
}
