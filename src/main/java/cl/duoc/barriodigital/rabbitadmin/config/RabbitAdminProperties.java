package cl.duoc.barriodigital.rabbitadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * AMQP 0-9-1 no tiene operacion de "listar colas": enumerar la topologia es cosa
 * del plano HTTP de management, no del protocolo. Como este servicio administra
 * por AMQP, {@code GET /admin/queues} sin parametros inspecciona esta lista, y
 * con {@code ?names=a,b} inspecciona las que le pidan.
 */
@ConfigurationProperties(prefix = "barriodigital.rabbitadmin")
public record RabbitAdminProperties(List<String> colasConocidas) {

    public RabbitAdminProperties {
        colasConocidas = colasConocidas == null ? List.of() : List.copyOf(colasConocidas);
    }
}
