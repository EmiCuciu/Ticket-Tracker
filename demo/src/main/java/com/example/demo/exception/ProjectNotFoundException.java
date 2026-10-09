package com.example.demo.exception;

public class ProjectNotFoundException extends NotFoundException {

    public ProjectNotFoundException(Object resourceId) {
        super("Project", resourceId);
    }
}
