package com.project.bookngo.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PRECONDITION_REQUIRED)
public class VerificationRequiredException extends RuntimeException{
    public VerificationRequiredException(String message){
        super(message);
    }
}
