package cl.duoc.barriodigital.rabbitadmin.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.amqp.AmqpException;
import org.springframework.core.NestedExceptionUtils;
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

    /** Cuerpo invalido (@Valid @RequestBody). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail entradaInvalida(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, "Entrada invalida", detalle);
    }

    /** Nombre invalido en la ruta o en ?names= (controladores con @Validated). */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail parametroInvalido(ConstraintViolationException ex) {
        String detalle = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, "Entrada invalida", detalle);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Entrada invalida", ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail noEncontrado(NoSuchElementException ex) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage());
    }

    /**
     * El broker rechazo la operacion. El motivo real viene anidado (AmqpIOException
     * → IOException → ShutdownSignalException), asi que se informa la causa mas
     * especifica y no el "java.io.IOException" de afuera.
     * <ul>
     *   <li>{@code PRECONDITION_FAILED}: se intento redeclarar algo que ya existe con
     *       otros argumentos (p. ej. crear como clasica una cola que es quorum).
     *       Es una configuracion invalida del cliente: 409.</li>
     *   <li>Cualquier otro (broker caido, etc.): 502, porque el fallo es del sistema
     *       de abajo y no de este servicio.</li>
     * </ul>
     */
    @ExceptionHandler(AmqpException.class)
    ProblemDetail brokerRechazo(AmqpException ex) {
        String causa = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        if (causa != null && causa.contains("PRECONDITION_FAILED")) {
            return problema(HttpStatus.CONFLICT, "Configuracion incompatible con la existente",
                    "RabbitMQ rechazo la operacion porque ya existe con otros argumentos: " + causa);
        }
        return problema(HttpStatus.BAD_GATEWAY, "Error del broker", "RabbitMQ rechazo la operacion: " + causa);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setTitle(titulo);
        return pd;
    }
}
