package com.github.dillin.musicplayer.domain.models.exceptions;

public class FileWriteNotAllowedException extends RuntimeException {

    public FileWriteNotAllowedException(String message) {
        super(message);
    }
}
