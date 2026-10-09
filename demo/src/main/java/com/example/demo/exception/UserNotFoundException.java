package com.example.demo.exception;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(Object resourceId) {
        super("User", resourceId);
    }
}
