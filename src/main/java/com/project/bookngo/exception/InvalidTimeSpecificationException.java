package com.project.bookngo.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTimeSpecificationException extends RuntimeException {
    public InvalidTimeSpecificationException(String message) {
        super(message);
    }
}
