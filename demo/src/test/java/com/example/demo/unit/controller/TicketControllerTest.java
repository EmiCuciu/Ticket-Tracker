package com.example.demo.unit.controller;

import com.example.demo.config.TestSecurityConfig;
import com.example.demo.controller.TicketController;
import com.example.demo.exception.TicketNotFoundException;
import com.example.demo.model.*;
import com.example.demo.security.JwtService;
import com.example.demo.service.TicketService;
import com.example.demo.service.implementation.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsServiceImpl;

    @MockitoBean
    private JwtService jwtService;

    private UserSummaryDto userSummaryDto;
    private ProjectSummaryDto projectSummaryDto;
    private UUID ticketId;

    @BeforeEach
    void setup() {
        userSummaryDto = new UserSummaryDto();
        userSummaryDto.setId(UUID.randomUUID());
        userSummaryDto.setEmail("test@email.com");
        userSummaryDto.setFullName("Cuciurean Emilian");

        projectSummaryDto = new ProjectSummaryDto();
        projectSummaryDto.setId(UUID.randomUUID());
        projectSummaryDto.setName("Test Project");

        ticketId = UUID.randomUUID();
    }

    private TicketDto buildTicketDto(String title, Status status, Priority priority) {
        TicketDto dto = new TicketDto();
        dto.setId(ticketId);
        dto.setTitle(title);
        dto.setStatus(status);
        dto.setPriority(priority);
        dto.setAssignee(userSummaryDto);
        dto.setProject(projectSummaryDto);
        return dto;
    }

    @Test
    void whenTicketExists_thenReturns200AndTicket() throws Exception {
        TicketDto response = buildTicketDto("Test Ticket", Status.OPEN, Priority.HIGH);

        given(ticketService.getTicket(ticketId)).willReturn(response);

        mockMvc.perform(get("/api/tickets/{id}", ticketId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.title").value("Test Ticket"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignee.fullName").value("Cuciurean Emilian"))
                .andExpect(jsonPath("$.project.name").value("Test Project"));

        verify(ticketService).getTicket(ticketId);
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenTicketDoesNotExist_thenReturn404() throws Exception {
        given(ticketService.getTicket(ticketId))
                .willThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(get("/api/tickets/{id}", ticketId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(ticketService).getTicket(ticketId);
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenGetTickets_thenReturns200AndPageWithFullTicket() throws Exception {
        TicketDto dto = buildTicketDto("Ticket1", Status.IN_PROGRESS, Priority.MEDIUM);

        given(ticketService.searchTickets(
                eq(Status.IN_PROGRESS), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), isNull(), isNull()
        )).willReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/tickets")
                        .param("status", "IN_PROGRESS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Ticket1"))
                .andExpect(jsonPath("$.content[0].assignee.fullName").value("Cuciurean Emilian"))
                .andExpect(jsonPath("$.content[0].project.name").value("Test Project"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(ticketService).searchTickets(
                eq(Status.IN_PROGRESS), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), isNull(), isNull());
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenGetTicketsNoParams_thenReturns200AndEmptyPage() throws Exception {
        given(ticketService.searchTickets(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), isNull(), isNull()
        )).willReturn(new PageImpl<>(List.of(), PageRequest.of(500, 20), 0));

        mockMvc.perform(get("/api/tickets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(ticketService).searchTickets(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), isNull(), isNull());
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenCreateTicket_thenReturns201AndTicket() throws Exception {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("New ticket");
        request.setStatus(Status.OPEN);
        request.setPriority(Priority.LOW);
        request.setProjectId(projectSummaryDto.getId());
        request.setAssigneeId(JsonNullable.of(userSummaryDto.getId()));

        TicketDto response = buildTicketDto("New ticket", Status.OPEN, Priority.LOW);

        given(ticketService.createTicket(eq(request))).willReturn(response);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New ticket"))
                .andExpect(jsonPath("$.assignee.fullName").value("Cuciurean Emilian"))
                .andExpect(jsonPath("$.project.name").value("Test Project"));

        verify(ticketService).createTicket(eq(request));
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenCreateTicketMissingTitle_thenReturns400() throws Exception {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setStatus(Status.OPEN);
        request.setPriority(Priority.LOW);
        request.setProjectId(projectSummaryDto.getId());

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(ticketService);
    }

    @Test
    void whenUpdateTicket_thenReturns200AndTicket() throws Exception {
        TicketPutRequest request = new TicketPutRequest();
        request.setTitle("Updated title");
        request.setStatus(Status.IN_PROGRESS);
        request.setPriority(Priority.HIGH);

        TicketDto response = buildTicketDto("Updated title", Status.IN_PROGRESS, Priority.HIGH);

        given(ticketService.updateTicket(eq(ticketId), eq(request))).willReturn(response);

        mockMvc.perform(put("/api/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(ticketService).updateTicket(eq(ticketId), eq(request));
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenUpdateTicketNotFound_thenReturns404() throws Exception {
        TicketPutRequest request = new TicketPutRequest();
        request.setTitle("Updated title");
        request.setStatus(Status.IN_PROGRESS);
        request.setPriority(Priority.HIGH);

        given(ticketService.updateTicket(eq(ticketId), eq(request)))
                .willThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(put("/api/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(ticketService).updateTicket(eq(ticketId), eq(request));
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenUpdateTicketMissingTitle_thenReturns400() throws Exception {
        TicketPutRequest request = new TicketPutRequest();
        request.setStatus(Status.IN_PROGRESS);
        request.setPriority(Priority.HIGH);

        mockMvc.perform(put("/api/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(ticketService);
    }

    @Test
    void whenPatchTicket_thenReturns200AndTicket() throws Exception {
        TicketPatchRequest request = new TicketPatchRequest();
        request.setStatus(JsonNullable.of(Status.DONE));

        TicketDto response = buildTicketDto("Test Ticket", Status.DONE, Priority.HIGH);

        given(ticketService.patchTicket(eq(ticketId), eq(request))).willReturn(response);

        mockMvc.perform(patch("/api/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        verify(ticketService).patchTicket(eq(ticketId), eq(request));
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenPatchTicketInvalidStatus_thenReturns400() throws Exception {
        String invalidJson = """
                { "status": "NOT_A_REAL_STATUS" }
                """;

        mockMvc.perform(patch("/api/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(ticketService);
    }

    @Test
    void whenDeleteTicket_thenReturns204() throws Exception {
        mockMvc.perform(delete("/api/tickets/{id}", ticketId))
                .andExpect(status().isNoContent());

        verify(ticketService).deleteTicket(ticketId);
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenDeleteTicketNotFound_thenReturns404() throws Exception {
        doThrow(new TicketNotFoundException(ticketId))
                .when(ticketService).deleteTicket(ticketId);

        mockMvc.perform(delete("/api/tickets/{id}", ticketId))
                .andExpect(status().isNotFound());

        verify(ticketService).deleteTicket(ticketId);
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void getTickets_pageAbovemax_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(get("/api/tickets").param("page", "99999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("page"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be less than or equal to 1000"));
    }
}