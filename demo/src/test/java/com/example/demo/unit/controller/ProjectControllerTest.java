package com.example.demo.unit.controller;

import com.example.demo.config.TestSecurityConfig;
import com.example.demo.controller.ProjectController;
import com.example.demo.exception.ProjectNotFoundException;
import com.example.demo.model.ProjectCreateRequest;
import com.example.demo.model.ProjectDto;
import com.example.demo.model.UserSummaryDto;
import com.example.demo.security.JwtService;
import com.example.demo.service.ProjectService;
import com.example.demo.service.implementation.UserDetailsServiceImpl;
import com.example.demo.web.EntityPageableResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import({TestSecurityConfig.class, EntityPageableResolver.class})
@ActiveProfiles("test")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsServiceImpl;

    @MockitoBean
    private JwtService jwtService;

    private UUID projectId;
    private UserSummaryDto createdBy;

    @BeforeEach
    void setup() {
        projectId = UUID.randomUUID();
        createdBy = new UserSummaryDto();
        createdBy.setId(UUID.randomUUID());
        createdBy.setFullName("Cuciurean Emilian");
        createdBy.setEmail("test@email.com");
    }

    private ProjectDto buildProjectDto(String name) {
        ProjectDto dto = new ProjectDto();
        dto.setId(projectId);
        dto.setName(name);
        dto.setDescription("desc");
        dto.setCreatedBy(createdBy);
        return dto;
    }

    @Test
    void whenProjectExists_thenReturns200AndProject() throws Exception {
        ProjectDto response = buildProjectDto("Test Project");

        given(projectService.getProject(projectId)).willReturn(response);

        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.createdBy.fullName").value("Cuciurean Emilian"));

        verify(projectService).getProject(projectId);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenProjectDoesNotExist_thenReturns404() throws Exception {
        given(projectService.getProject(projectId))
                .willThrow(new ProjectNotFoundException(projectId));

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isNotFound());

        verify(projectService).getProject(projectId);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenGetProjects_thenReturns200AndPage() throws Exception {
        ProjectDto dto = buildProjectDto("Project1");

        given(projectService.getProjects(eq(PageRequest.of(0, 20))))
                .willReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/projects")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Project1"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(projectService).getProjects(eq(PageRequest.of(0, 20)));
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenCreateProject_thenReturns201AndProject() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("New Project");
        request.setDescription("desc");

        ProjectDto response = buildProjectDto("New Project");

        given(projectService.createProject(eq(request))).willReturn(response);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Project"));

        verify(projectService).createProject(eq(request));
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenCreateProjectMissingName_thenReturns400() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setDescription("desc only, no name");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(projectService);
    }

    @Test
    void whenDeleteProject_thenReturns204() throws Exception {
        mockMvc.perform(delete("/api/projects/{id}", projectId))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(projectId);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenDeleteProjectNotFound_thenReturns404() throws Exception {
        doThrow(new ProjectNotFoundException(projectId))
                .when(projectService).deleteProject(projectId);

        mockMvc.perform(delete("/api/projects/{id}", projectId))
                .andExpect(status().isNotFound());

        verify(projectService).deleteProject(projectId);
        verifyNoMoreInteractions(projectService);
    }
}