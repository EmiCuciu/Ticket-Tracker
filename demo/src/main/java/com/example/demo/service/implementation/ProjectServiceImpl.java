package com.example.demo.service.implementation;

import com.example.demo.domain.Project;
import com.example.demo.domain.User;
import com.example.demo.exception.ProjectNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.mapper.ProjectMapper;
import com.example.demo.model.ProjectCreateRequest;
import com.example.demo.model.ProjectDto;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.CurrentUserProvider;
import com.example.demo.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectDto> getProjects(Pageable pageable) {
        Page<Project> result = projectRepository.findAll(pageable);
        return result.map(projectMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "projects", key = "#id")
    public ProjectDto getProject(UUID id) {
        return projectMapper.toDto(findOrThrow(id));
    }

    @Override
    @Transactional
    public ProjectDto createProject(ProjectCreateRequest request) {
        Project project = projectMapper.toEntity(request);

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        User createdBy = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        project.setCreatedBy(createdBy);
        return projectMapper.toDto(projectRepository.save(project));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "projects", key = "#id"),
            @CacheEvict(value = "tickets", allEntries = true)
    })
    public void deleteProject(UUID id) {
        if (!projectRepository.existsById(id))
            throw new ProjectNotFoundException(id);
        projectRepository.deleteById(id);
    }

    private Project findOrThrow(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }
}
