package cl.duoc.barriodigital.rabbitadmin.service;

import cl.duoc.barriodigital.rabbitadmin.config.RabbitAdminProperties;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Unico punto del servicio que conoce RabbitMQ. Los controladores hablan de
 * nombres, tipos y routing keys; aqui adentro viven {@link AmqpAdmin},
 * {@link QueueBuilder}, {@link ExchangeBuilder} y {@link Binding}.
 *
 * <p><b>Frontera con la topologia declarada.</b> La topologia base del sistema es
 * codigo: {@code ms-barriodigital-notify} la declara con {@code @Bean} y la
 * re-aplica en cada arranque. Este servicio la muta en runtime. La consecuencia
 * observable es que una cola borrada con {@code DELETE /admin/queues/q.cmd.email}
 * <b>reaparece</b> cuando {@code notify} reinicia. No es un bug: la regla es
 * <i>la topologia base es codigo, lo operativo es API</i>. Este servicio sirve
 * para crear colas nuevas, purgar, inspeccionar y reparar; no para redefinir la
 * topologia del sistema, que vive en el repositorio y se versiona.
 * Ver Justificacion-Mensajeria.md §3.
 */
@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final List<String> colasConocidas;

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitAdminProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.colasConocidas = props.colasConocidas();
    }

    /** Datos de una cola, sin exponer tipos de Spring AMQP hacia la capa web. */
    public record InfoCola(String nombre, int mensajes, int consumidores) {
    }

    // ---------- colas ----------

    public String crearCola(String nombre, boolean durable,
                            String deadLetterExchange, String deadLetterRoutingKey) {
        QueueBuilder builder = durable ? QueueBuilder.durable(nombre) : QueueBuilder.nonDurable(nombre);
        if (tieneTexto(deadLetterExchange)) {
            builder.withArgument("x-dead-letter-exchange", deadLetterExchange);
        }
        if (tieneTexto(deadLetterRoutingKey)) {
            builder.withArgument("x-dead-letter-routing-key", deadLetterRoutingKey);
        }
        return amqpAdmin.declareQueue(builder.build());
    }

    public void eliminarCola(String nombre) {
        if (!amqpAdmin.deleteQueue(nombre)) {
            throw new NoSuchElementException("No existe la cola " + nombre);
        }
    }

    /** @return cuantos mensajes se descartaron. */
    public int purgarCola(String nombre) {
        exigirQueExista(nombre);
        return amqpAdmin.purgeQueue(nombre);
    }

    /**
     * @param nombres colas a inspeccionar; si viene vacio se usan las de
     *                {@code barriodigital.rabbitadmin.colas-conocidas}, porque AMQP
     *                no permite enumerar (ver {@link RabbitAdminProperties}).
     */
    public List<InfoCola> listar(List<String> nombres) {
        List<String> aConsultar = (nombres == null || nombres.isEmpty()) ? colasConocidas : nombres;
        return aConsultar.stream()
                .map(amqpAdmin::getQueueInfo)
                .filter(java.util.Objects::nonNull)
                .map(i -> new InfoCola(i.getName(), i.getMessageCount(), i.getConsumerCount()))
                .toList();
    }

    // ---------- exchanges ----------

    public void crearExchange(String nombre, String tipo) {
        amqpAdmin.declareExchange(construirExchange(nombre, tipo));
    }

    public void eliminarExchange(String nombre) {
        if (!amqpAdmin.deleteExchange(nombre)) {
            throw new NoSuchElementException("No existe el exchange " + nombre);
        }
    }

    // ---------- bindings ----------

    public void crearBinding(String cola, String exchange, String routingKey) {
        amqpAdmin.declareBinding(construirBinding(cola, exchange, routingKey));
    }

    public void eliminarBinding(String cola, String exchange, String routingKey) {
        amqpAdmin.removeBinding(construirBinding(cola, exchange, routingKey));
    }

    // ---------- interno ----------

    private void exigirQueExista(String nombre) {
        if (amqpAdmin.getQueueInfo(nombre) == null) {
            throw new NoSuchElementException("No existe la cola " + nombre);
        }
    }

    private Exchange construirExchange(String nombre, String tipo) {
        // La validacion de entrada ya restringe el tipo; esto cubre las llamadas
        // que no vienen por HTTP.
        return switch (tipo == null ? "" : tipo.toLowerCase()) {
            case "direct" -> ExchangeBuilder.directExchange(nombre).durable(true).build();
            case "topic" -> ExchangeBuilder.topicExchange(nombre).durable(true).build();
            case "fanout" -> ExchangeBuilder.fanoutExchange(nombre).durable(true).build();
            case "headers" -> ExchangeBuilder.headersExchange(nombre).durable(true).build();
            default -> throw new IllegalArgumentException(
                    "Tipo de exchange no soportado: '" + tipo + "'. Use direct, topic, fanout o headers");
        };
    }

    /** Binding generico: no necesita saber de que tipo es el exchange destino. */
    private Binding construirBinding(String cola, String exchange, String routingKey) {
        return new Binding(cola, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }

    private static boolean tieneTexto(String s) {
        return s != null && !s.isBlank();
    }
}
