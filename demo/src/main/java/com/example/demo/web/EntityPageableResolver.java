package com.example.demo.web;

import com.example.demo.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class EntityPageableResolver {

    public Pageable resolve(Integer page, Integer size) {
        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 20;

        if (resolvedPage < 0) {
            throw new BadRequestException("page must be >= 0");
        }
        if (resolvedSize < 1) {
            throw new BadRequestException("size must be >= 1");
        }

        return PageRequest.of(resolvedPage, resolvedSize);
    }
}