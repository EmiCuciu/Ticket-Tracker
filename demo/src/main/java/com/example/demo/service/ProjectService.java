package com.example.demo.service;

import com.example.demo.model.ProjectCreateRequest;
import com.example.demo.model.ProjectDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProjectService {

    /**
     * Retrieves a paginated list of projects.
     *
     * @param pageable pagination information
     * @return a page of ProjectDto objects
     */
    Page<ProjectDto> getProjects(Pageable pageable);

    /**
     * Retrieves a specific project by its ID.
     *
     * @param id the ID of the project
     * @return the ProjectDto object representing the project
     */
    ProjectDto getProject(UUID id);

    /**
     * Creates a new project.
     *
     * @param request the request containing project details
     * @return the created ProjectDto object
     */
    ProjectDto createProject(ProjectCreateRequest request);

    /**
     * Deletes a specific project by its ID.
     *
     * @param id the ID of the project to delete
     */
    void deleteProject(UUID id);
}
