package com.project.bookngo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentials extends RuntimeException{
    public InvalidCredentials(String message){
        super(message);
    }

}
