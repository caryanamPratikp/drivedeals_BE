package in.motorcircle.backend.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ResourceNotFoundException formats into RFC 7807 404 ProblemDetail")
    void handleResourceNotFoundFormatsProblemDetail() {
        ResourceNotFoundException ex = new ResourceNotFoundException("BuyerGroup", "id", "grp_123");
        ProblemDetail problem = exceptionHandler.handleResourceNotFound(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problem.getDetail()).contains("BuyerGroup not found with id: 'grp_123'");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("BusinessRuleException formats into RFC 7807 400 ProblemDetail with custom errorCode")
    void handleBusinessRuleFormatsProblemDetail() {
        BusinessRuleException ex = new BusinessRuleException("INSUFFICIENT_MEMBERS", "Minimum 3 members required to request quotes.");
        ProblemDetail problem = exceptionHandler.handleBusinessRuleViolation(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Business Rule Violation");
        assertThat(problem.getProperties().get("errorCode")).isEqualTo("INSUFFICIENT_MEMBERS");
    }
}
