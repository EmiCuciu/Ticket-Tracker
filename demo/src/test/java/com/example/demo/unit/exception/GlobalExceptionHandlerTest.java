package com.example.demo.unit.exception;

import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.ErrorResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Profile("test")
class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        handler = new GlobalExceptionHandler(objectMapper);
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/tickets/123");
        when(request.getMethod()).thenReturn("GET");
    }

    @Test
    void handleNotFound_returns404() {
        NotFoundException ex = new NotFoundException("Ticket", "123");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).contains("Ticket");
        assertThat(response.getBody().getPath()).isEqualTo("/api/tickets/123");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleBadRequest_returns400() {
        BadRequestException ex = new BadRequestException("dueBefore must not be earlier than dueAfter");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("dueBefore must not be earlier than dueAfter");
    }

    @Test
    void handleValidation_returns400WithFieldErrors() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "ticketCreateRequest");
        bindingResult.addError(new FieldError("ticketCreateRequest", "title", "must not be blank"));
        bindingResult.addError(new FieldError("ticketCreateRequest", "status", "must not be null"));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getFieldErrors()).hasSize(2);
        assertThat(response.getBody().getFieldErrors())
                .extracting("field")
                .containsExactlyInAnyOrder("title", "status");
        assertThat(response.getBody().getFieldErrors())
                .extracting("message")
                .containsExactlyInAnyOrder("must not be blank", "must not be null");
    }

    @Test
    void handleTypeMismatch_returns400() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("status");
        when(ex.getValue()).thenReturn("NOT_A_STATUS");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Invalid value for parameter 'status' : 'NOT_A_STATUS'");
    }

    @Test
    void handleUnreadable_withInvalidFormatCause_returns400WithFieldDetail() {
        InvalidFormatException ife = mock(InvalidFormatException.class);
        when(ife.getValue()).thenReturn("BAD_ENUM");
        JsonMappingException.Reference ref = new JsonMappingException.Reference(new Object(), "status");
        when(ife.getPath()).thenReturn(List.of(ref));

        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getCause()).thenReturn(ife);

        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid value 'BAD_ENUM' for field 'status'");
    }

    @Test
    void handleUnreadable_withoutInvalidFormatCause_returnsGenericMessage() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getCause()).thenReturn(new RuntimeException("broken"));

        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Malformed JSON request");
    }

    @Test
    void handleUnreadable_invalidFormatWithEmptyPath() {
        InvalidFormatException ife = mock(InvalidFormatException.class);
        when(ife.getValue()).thenReturn("BAD_VALUE");
        when(ife.getPath()).thenReturn(List.of());

        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getCause()).thenReturn(ife);

        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex, request);

        assertThat(response.getBody().getMessage()).isEqualTo("Invalid value 'BAD_VALUE' for field '?'");
    }

    @Test
    void handleDataIntegrity_returns409() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Resource already exists or violates a database constraint");
    }

    @Test
    void handleGeneric_returns500WithGenericMessage() {
        Exception ex = new RuntimeException("sensitive internal detail");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("sensitive internal detail");
    }
}