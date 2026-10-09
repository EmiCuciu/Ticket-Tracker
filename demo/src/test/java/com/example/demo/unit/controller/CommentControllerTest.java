package com.example.demo.unit.controller;

import com.example.demo.config.TestSecurityConfig;
import com.example.demo.controller.CommentController;
import com.example.demo.exception.TicketNotFoundException;
import com.example.demo.model.CommentCreateRequest;
import com.example.demo.model.CommentDto;
import com.example.demo.model.UserSummaryDto;
import com.example.demo.security.JwtService;
import com.example.demo.service.CommentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import({TestSecurityConfig.class, EntityPageableResolver.class})
@ActiveProfiles("test")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsServiceImpl;

    @MockitoBean
    private JwtService jwtService;

    private UUID ticketId;
    private UUID commentId;
    private UserSummaryDto author;

    @BeforeEach
    void setup() {
        ticketId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        author = new UserSummaryDto();
        author.setId(UUID.randomUUID());
        author.setFullName("Cuciurean Emilian");
        author.setEmail("test@email.com");
    }

    private CommentDto buildCommentDto(String body) {
        CommentDto dto = new CommentDto();
        dto.setId(commentId);
        dto.setTicketId(ticketId);
        dto.setBody(body);
        dto.setAuthor(author);
        return dto;
    }

    @Test
    void whenGetComments_thenReturns200AndPage() throws Exception {
        CommentDto dto = buildCommentDto("First comment");

        given(commentService.getComments(eq(ticketId), eq(PageRequest.of(0, 20))))
                .willReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticketId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].body").value("First comment"))
                .andExpect(jsonPath("$.content[0].author.fullName").value("Cuciurean Emilian"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(commentService).getComments(eq(ticketId), eq(PageRequest.of(0, 20)));
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenGetCommentsTicketNotFound_thenReturns404() throws Exception {
        given(commentService.getComments(eq(ticketId), eq(PageRequest.of(0, 20))))
                .willThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticketId))
                .andExpect(status().isNotFound());

        verify(commentService).getComments(eq(ticketId), eq(PageRequest.of(0, 20)));
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenCreateComment_thenReturns201AndComment() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBody("New comment");

        CommentDto response = buildCommentDto("New comment");

        given(commentService.createComment(eq(ticketId), eq(request))).willReturn(response);

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("New comment"))
                .andExpect(jsonPath("$.ticketId").value(ticketId.toString()));

        verify(commentService).createComment(eq(ticketId), eq(request));
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenCreateCommentBlankBody_thenReturns400() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBody("");

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(commentService);
    }

    @Test
    void whenCreateCommentTicketNotFound_thenReturns404() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBody("New comment");

        given(commentService.createComment(eq(ticketId), eq(request)))
                .willThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(commentService).createComment(eq(ticketId), eq(request));
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenGetComments_customPageAndSize_thenReturnsCorrectPage() throws Exception {
        CommentDto dto = buildCommentDto("Comment page 2");

        given(commentService.getComments(eq(ticketId), eq(PageRequest.of(2, 5))))
                .willReturn(new PageImpl<>(List.of(dto), PageRequest.of(2, 5), 11));

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticketId)
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(commentService).getComments(eq(ticketId), eq(PageRequest.of(2, 5)));
    }

    @Test
    void whenGetComments_negativePage_thenReturns400() throws Exception {
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticketId)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    void whenGetComments_over1000Page_thenReturns400() throws Exception {
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticketId)
                        .param("page", "99999"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }
}