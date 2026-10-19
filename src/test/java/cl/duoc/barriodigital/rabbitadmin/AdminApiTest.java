package cl.duoc.barriodigital.rabbitadmin;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Indicador 8 (validacion con mensajes claros) e indicador 6 (la API existe y
 * esta gateada). El broker se reemplaza por un mock: estos tests no necesitan
 * RabbitMQ corriendo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtDecoder jwtDecoder;

    @MockBean
    AmqpAdmin amqpAdmin;

    private static SimpleGrantedAuthority rol(String r) {
        return new SimpleGrantedAuthority("ROLE_" + r);
    }

    // ---------- seguridad ----------

    @Test
    void sin_token_401() throws Exception {
        mvc.perform(get("/admin/queues")).andExpect(status().isUnauthorized());
    }

    @Test
    void con_rol_distinto_de_admin_403() throws Exception {
        mvc.perform(get("/admin/queues").with(jwt().authorities(rol("Funcionario"))))
           .andExpect(status().isForbidden());
    }

    @Test
    void crear_cola_sin_rol_admin_403() throws Exception {
        mvc.perform(post("/admin/queues")
                .contentType("application/json")
                .content("{\"nombre\":\"q.nueva\"}")
                .with(jwt().authorities(rol("Vecino"))))
           .andExpect(status().isForbidden());
    }

    @Test
    void listar_con_rol_admin_200() throws Exception {
        mvc.perform(get("/admin/queues").with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isOk());
    }

    @Test
    void crear_cola_con_rol_admin_201() throws Exception {
        when(amqpAdmin.declareQueue(any(Queue.class))).thenReturn("q.nueva");

        mvc.perform(post("/admin/queues")
                .contentType("application/json")
                .content("{\"nombre\":\"q.nueva\",\"deadLetterExchange\":\"cmd.dead.dlx\"}")
                .with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.nombre").value("q.nueva"));
    }

    // ---------- validacion, con mensaje legible ----------

    @Test
    void nombre_vacio_es_400_y_dice_que_es_obligatorio() throws Exception {
        mvc.perform(post("/admin/queues")
                .contentType("application/json")
                .content("{\"nombre\":\"\"}")
                .with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.title").value("Entrada invalida"))
           .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("obligatorio")));
    }

    @Test
    void nombre_con_espacios_es_400_y_explica_que_caracteres_valen() throws Exception {
        mvc.perform(post("/admin/queues")
                .contentType("application/json")
                .content("{\"nombre\":\"cola con espacios\"}")
                .with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("sin espacios")));
    }

    @Test
    void tipo_de_exchange_invalido_es_400_y_lista_los_validos() throws Exception {
        mvc.perform(post("/admin/exchanges")
                .contentType("application/json")
                .content("{\"nombre\":\"mi.exchange\",\"tipo\":\"cuantico\"}")
                .with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.detail").value(
                   org.hamcrest.Matchers.containsString("direct, topic, fanout, headers")));
    }

    @Test
    void binding_sin_routing_key_es_400() throws Exception {
        mvc.perform(post("/admin/bindings")
                .contentType("application/json")
                .content("{\"cola\":\"q.cmd.email\",\"exchange\":\"cmd.direct\"}")
                .with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.detail").value(
                   org.hamcrest.Matchers.containsString("routing key es obligatoria")));
    }

    @Test
    void nombre_con_prefijo_reservado_amq_es_400() throws Exception {
        mvc.perform(post("/admin/queues")
                .contentType("application/json")
                .content("{\"nombre\":\"amq.mia\"}")
                .with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("amq.")));
    }

    @Test
    void nombre_invalido_en_la_ruta_es_400_con_mensaje() throws Exception {
        mvc.perform(delete("/admin/queues/cola!invalida").with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.title").value("Entrada invalida"))
           .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("sin espacios")));
    }

    @Test
    void purgar_cola_reservada_es_400() throws Exception {
        mvc.perform(post("/admin/queues/amq.rabbitmq.trace/purge").with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isBadRequest());
    }

    @Test
    void nombre_invalido_en_names_es_400() throws Exception {
        mvc.perform(get("/admin/queues").param("names", "q.cmd.email", "mal nombre")
                .with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("names")));
    }

    @Test
    void eliminar_exchange_con_nombre_invalido_es_400() throws Exception {
        mvc.perform(delete("/admin/exchanges/amq.direct").with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("exchange")));
    }

    /** Redeclarar con otros argumentos (p. ej. q.cmd.email como clasica) es configuracion invalida: 409 legible. */
    @Test
    void redeclarar_cola_con_otros_argumentos_es_409_con_el_motivo_del_broker() throws Exception {
        when(amqpAdmin.declareQueue(any(Queue.class))).thenThrow(new org.springframework.amqp.AmqpIOException(
                new java.io.IOException(new RuntimeException(
                        "reply-code=406, reply-text=PRECONDITION_FAILED - inequivalent arg 'x-queue-type'"
                                + " for queue 'q.cmd.email'"))));

        mvc.perform(post("/admin/queues")
                .contentType("application/json")
                .content("{\"nombre\":\"q.cmd.email\"}")
                .with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("x-queue-type")));
    }

    // ---------- not found ----------

    @Test
    void eliminar_cola_inexistente_es_404_con_mensaje() throws Exception {
        when(amqpAdmin.deleteQueue("q.fantasma")).thenReturn(false);

        mvc.perform(delete("/admin/queues/q.fantasma").with(jwt().authorities(rol("Admin"))))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("q.fantasma")));
    }

    /**
     * Lo que importa es que el endpoint no exija token. El health de RabbitMQ queda
     * desactivado solo en el perfil de test (no hay broker); en produccion sigue
     * activo y este endpoint responde 503 con el broker caido, que es lo correcto.
     */
    @Test
    void actuator_health_queda_abierto_sin_token() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
