package ai.sc2coach.portal.api;

import ai.sc2coach.portal.analysis.ReplayDecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidReplay(IllegalArgumentException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        detail.setTitle("Invalid replay upload");
        return detail;
    }

    @ExceptionHandler(ReplayDecodingException.class)
    ProblemDetail decodingFailed(ReplayDecodingException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        detail.setTitle("Replay decoding failed");
        return detail;
    }
}
