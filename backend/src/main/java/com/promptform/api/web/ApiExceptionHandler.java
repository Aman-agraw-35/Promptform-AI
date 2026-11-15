package com.promptform.api.web;

import com.promptform.api.auth.AuthException;
import com.promptform.api.eval.EvalException;
import com.promptform.api.generation.GenerationException;
import com.promptform.api.project.ProjectException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(EvalException.class)
    public ProblemDetail handleEval(EvalException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Evaluation request failed");
        return problem;
    }

    @ExceptionHandler(ProjectException.class)
    public ProblemDetail handleProject(ProjectException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Project request failed");
        return problem;
    }

    @ExceptionHandler(AuthException.class)
    public ProblemDetail handleAuthConflict(AuthException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Authentication request failed");
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("Authentication failed");
        return problem;
    }

    @ExceptionHandler(GenerationException.class)
    public ProblemDetail handleGeneration(GenerationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problem.setTitle("Form generation failed");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Prompt must be between 1 and 2,000 characters.");
        problem.setTitle("Invalid request");
        return problem;
    }
}
