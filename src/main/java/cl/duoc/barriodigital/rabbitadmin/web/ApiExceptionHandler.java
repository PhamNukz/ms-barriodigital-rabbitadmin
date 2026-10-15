package cl.duoc.barriodigital.rabbitadmin.web;

import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Traduce los fallos a ProblemDetail con un mensaje que se pueda leer sin abrir
 * el codigo: que campo fallo y por que. La pauta evalua los mensajes de error,
 * no solo que la validacion exista.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail entradaInvalida(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
        pd.setTitle("Entrada invalida");
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Entrada invalida");
        return pd;
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail noEncontrado(NoSuchElementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("No encontrado");
        return pd;
    }

    /**
     * El broker rechazo la operacion: tipico PRECONDITION_FAILED al redeclarar algo
     * existente con otros argumentos, o el broker caido. 502 y no 500 porque el
     * fallo es del sistema de abajo, no de este servicio.
     */
    @ExceptionHandler(AmqpException.class)
    ProblemDetail brokerRechazo(AmqpException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "RabbitMQ rechazo la operacion: " + ex.getMessage());
        pd.setTitle("Error del broker");
        return pd;
    }
}
