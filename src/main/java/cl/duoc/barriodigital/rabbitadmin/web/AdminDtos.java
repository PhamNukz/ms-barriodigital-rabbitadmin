package cl.duoc.barriodigital.rabbitadmin.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entrada de la API. Los nombres se validan contra lo que RabbitMQ acepta
 * —letras, digitos, guion, guion bajo, punto y dos puntos, hasta 255 caracteres—
 * para que un nombre invalido se rechace con un 400 legible en vez de llegar al
 * broker y volver como un error de protocolo.
 */
public final class AdminDtos {

    private AdminDtos() {
    }

    static final String NOMBRE_VALIDO = "^[A-Za-z0-9._:-]+$";
    static final String MENSAJE_NOMBRE =
            "solo admite letras, digitos, punto, guion, guion bajo y dos puntos (sin espacios)";

    public record CrearColaRequest(
            @NotBlank(message = "el nombre de la cola es obligatorio")
            @Size(max = 255, message = "el nombre no puede superar los 255 caracteres")
            @Pattern(regexp = NOMBRE_VALIDO, message = "el nombre de la cola " + MENSAJE_NOMBRE)
            String nombre,

            Boolean durable,

            @Size(max = 255)
            @Pattern(regexp = NOMBRE_VALIDO, message = "el dead letter exchange " + MENSAJE_NOMBRE)
            String deadLetterExchange,

            @Size(max = 255)
            String deadLetterRoutingKey) {

        /** Por defecto durable: una cola de administracion que se pierde al reiniciar no sirve. */
        public boolean esDurable() {
            return durable == null || durable;
        }
    }

    public record CrearExchangeRequest(
            @NotBlank(message = "el nombre del exchange es obligatorio")
            @Size(max = 255, message = "el nombre no puede superar los 255 caracteres")
            @Pattern(regexp = NOMBRE_VALIDO, message = "el nombre del exchange " + MENSAJE_NOMBRE)
            String nombre,

            @NotBlank(message = "el tipo de exchange es obligatorio")
            @Pattern(regexp = "direct|topic|fanout|headers",
                     message = "el tipo debe ser uno de: direct, topic, fanout, headers")
            String tipo) {
    }

    public record BindingRequest(
            @NotBlank(message = "el nombre de la cola es obligatorio")
            @Pattern(regexp = NOMBRE_VALIDO, message = "el nombre de la cola " + MENSAJE_NOMBRE)
            String cola,

            @NotBlank(message = "el nombre del exchange es obligatorio")
            @Pattern(regexp = NOMBRE_VALIDO, message = "el nombre del exchange " + MENSAJE_NOMBRE)
            String exchange,

            @NotBlank(message = "la routing key es obligatoria")
            @Size(max = 255, message = "la routing key no puede superar los 255 caracteres")
            String routingKey) {
    }

    public record ColaCreadaResponse(String nombre) {
    }

    public record PurgaResponse(String nombre, int mensajesPurgados) {
    }
}
