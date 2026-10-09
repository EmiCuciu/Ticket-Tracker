package com.example.demo.mapper;

import com.example.demo.domain.Project;
import com.example.demo.model.ProjectCreateRequest;
import com.example.demo.model.ProjectDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectDto toDto(Project project);

    Project toEntity(ProjectCreateRequest request);
}
