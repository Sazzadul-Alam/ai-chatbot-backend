package com.ds.tracks.commons.exception;

public class TokenExpireException extends RuntimeException {
    public TokenExpireException(String msg) {
        super(msg);
    }
}
