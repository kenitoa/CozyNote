package com.cozynote.domain;

public record Track(String title, String artist, String albumArtUri, String mediaUri, int durationSeconds) {
}
