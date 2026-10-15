package cl.duoc.barriodigital.rabbitadmin;

import cl.duoc.barriodigital.rabbitadmin.config.RabbitAdminProperties;
import cl.duoc.barriodigital.rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Indicador 7: el servicio se prueba con AmqpAdmin mockeado, sin broker. */
class RabbitAdminServiceTest {

    AmqpAdmin amqpAdmin;
    RabbitAdminService service;

    @BeforeEach
    void setUp() {
        amqpAdmin = mock(AmqpAdmin.class);
        service = new RabbitAdminService(amqpAdmin, new RabbitAdminProperties(List.of("q.conocida")));
    }

    @Test
    void crearCola_declara_durable_con_sus_argumentos_de_dead_letter() {
        when(amqpAdmin.declareQueue(any(Queue.class))).thenReturn("q.nueva");

        String nombre = service.crearCola("q.nueva", true, "cmd.dead.dlx", "nueva.dlq");

        assertThat(nombre).isEqualTo("q.nueva");
        var captor = org.mockito.ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        Queue declarada = captor.getValue();
        assertThat(declarada.getName()).isEqualTo("q.nueva");
        assertThat(declarada.isDurable()).isTrue();
        assertThat(declarada.getArguments())
                .containsEntry("x-dead-letter-exchange", "cmd.dead.dlx")
                .containsEntry("x-dead-letter-routing-key", "nueva.dlq");
    }

    @Test
    void crearCola_sin_dead_letter_no_agrega_argumentos_vacios() {
        service.crearCola("q.simple", true, null, "  ");

        var captor = org.mockito.ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        assertThat(captor.getValue().getArguments()).isEmpty();
    }

    @Test
    void eliminarCola_inexistente_es_404() {
        when(amqpAdmin.deleteQueue("q.fantasma")).thenReturn(false);

        assertThatThrownBy(() -> service.eliminarCola("q.fantasma"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("q.fantasma");
    }

    @Test
    void purgarCola_devuelve_cuantos_mensajes_descarto() {
        when(amqpAdmin.getQueueInfo("q.cmd.email")).thenReturn(new QueueInformation("q.cmd.email", 7, 1));
        when(amqpAdmin.purgeQueue("q.cmd.email")).thenReturn(7);

        assertThat(service.purgarCola("q.cmd.email")).isEqualTo(7);
    }

    @Test
    void purgarCola_inexistente_no_llama_al_broker() {
        when(amqpAdmin.getQueueInfo("q.fantasma")).thenReturn(null);

        assertThatThrownBy(() -> service.purgarCola("q.fantasma"))
                .isInstanceOf(NoSuchElementException.class);
        verify(amqpAdmin, never()).purgeQueue(anyString());
    }

    @Test
    void listar_sin_nombres_usa_las_colas_conocidas_de_configuracion() {
        when(amqpAdmin.getQueueInfo("q.conocida")).thenReturn(new QueueInformation("q.conocida", 3, 2));

        assertThat(service.listar(List.of()))
                .containsExactly(new RabbitAdminService.InfoCola("q.conocida", 3, 2));
    }

    @Test
    void listar_omite_las_colas_que_no_existen() {
        when(amqpAdmin.getQueueInfo("q.viva")).thenReturn(new QueueInformation("q.viva", 0, 0));
        when(amqpAdmin.getQueueInfo("q.muerta")).thenReturn(null);

        assertThat(service.listar(List.of("q.viva", "q.muerta")))
                .extracting(RabbitAdminService.InfoCola::nombre)
                .containsExactly("q.viva");
    }

    @Test
    void crearExchange_respeta_el_tipo_pedido() {
        service.crearExchange("mi.topic", "topic");

        var captor = org.mockito.ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TopicExchange.class);
        assertThat(captor.getValue().getName()).isEqualTo("mi.topic");
        assertThat(captor.getValue().isDurable()).isTrue();
    }

    @Test
    void crearExchange_con_tipo_desconocido_lo_rechaza_antes_del_broker() {
        assertThatThrownBy(() -> service.crearExchange("mi.raro", "cuantico"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("direct, topic, fanout o headers");
        verifyNoInteractions(amqpAdmin);
    }

    @Test
    void crearBinding_arma_la_terna_cola_exchange_routingkey() {
        service.crearBinding("q.cmd.email", "cmd.direct", "email.send");

        var captor = org.mockito.ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(captor.capture());
        Binding b = captor.getValue();
        assertThat(b.getDestination()).isEqualTo("q.cmd.email");
        assertThat(b.getDestinationType()).isEqualTo(Binding.DestinationType.QUEUE);
        assertThat(b.getExchange()).isEqualTo("cmd.direct");
        assertThat(b.getRoutingKey()).isEqualTo("email.send");
    }

    @Test
    void eliminarBinding_quita_la_misma_terna() {
        service.eliminarBinding("q.cmd.email", "cmd.direct", "email.send");

        var captor = org.mockito.ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).removeBinding(captor.capture());
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("email.send");
    }

    @Test
    void eliminarExchange_inexistente_es_404() {
        when(amqpAdmin.deleteExchange("ex.fantasma")).thenReturn(false);

        assertThatThrownBy(() -> service.eliminarExchange("ex.fantasma"))
                .isInstanceOf(NoSuchElementException.class);
    }
}
