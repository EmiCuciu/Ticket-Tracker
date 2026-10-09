package com.example.demo.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotFoundException extends RuntimeException {

    private final String resourceName;
    private final Object resourceId;

    public NotFoundException(String resourceName, Object resourceId) {
        super(String.format("%s with id %s was not found", resourceName, resourceId));
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

}