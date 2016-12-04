package com.jautumn.parser.core.exceptions;

public class BadDownloadServiceURLException extends Exception {
    private static final String MESSAGE = "Bad download service url: %s";

    public BadDownloadServiceURLException(String url) {
        this(MESSAGE, url);
    }

    public BadDownloadServiceURLException(String message, String url) {
        super(String.format(message, url));
    }
}
