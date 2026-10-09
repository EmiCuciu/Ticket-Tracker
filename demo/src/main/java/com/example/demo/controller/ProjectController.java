package com.example.demo.controller;

import com.example.demo.api.ProjectsApi;
import com.example.demo.model.ProjectCreateRequest;
import com.example.demo.model.ProjectDto;
import com.example.demo.model.ProjectPage;
import com.example.demo.service.ProjectService;
import com.example.demo.web.EntityPageableResolver;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ProjectController implements ProjectsApi {

    private final ProjectService projectService;
    private final EntityPageableResolver pageableResolver;

    public ProjectController(ProjectService projectService, EntityPageableResolver pageableResolver) {
        this.projectService = projectService;
        this.pageableResolver = pageableResolver;
    }

    @Override
    public ResponseEntity<ProjectPage> getProjects(Integer page, Integer size) {
        Page<ProjectDto> result = projectService.getProjects(pageableResolver.resolve(page, size));
        ProjectPage response = new ProjectPage();
        response.setContent(result.getContent());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ProjectDto> getProjectById(UUID id) {
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @Override
    public ResponseEntity<ProjectDto> createProject(ProjectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @Override
    public ResponseEntity<Void> deleteProject(UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}