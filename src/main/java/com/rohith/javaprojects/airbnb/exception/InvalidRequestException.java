package com.rohith.javaprojects.airbnb.exception;

public class InvalidRequestException extends RuntimeException{

    public InvalidRequestException(String message) {

        super(message);
    }

}
