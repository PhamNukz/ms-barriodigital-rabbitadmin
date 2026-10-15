package cl.duoc.barriodigital.rabbitadmin.web;

import cl.duoc.barriodigital.rabbitadmin.service.RabbitAdminService;
import cl.duoc.barriodigital.rabbitadmin.service.RabbitAdminService.InfoCola;
import cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.ColaCreadaResponse;
import cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.CrearColaRequest;
import cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.PurgaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Acceso restringido a Admin en SecurityConfig, para todo /admin/**. */
@RestController
@RequestMapping("/admin/queues")
public class QueueAdminController {

    private final RabbitAdminService service;

    public QueueAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColaCreadaResponse crear(@Valid @RequestBody CrearColaRequest req) {
        return new ColaCreadaResponse(service.crearCola(
                req.nombre(), req.esDurable(), req.deadLetterExchange(), req.deadLetterRoutingKey()));
    }

    @DeleteMapping("/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String nombre) {
        service.eliminarCola(nombre);
    }

    /**
     * @param names colas a inspeccionar. Si no viene, se usan las declaradas en
     *              configuracion: AMQP no permite enumerar colas.
     */
    @GetMapping
    public List<InfoCola> listar(@RequestParam(required = false) List<String> names) {
        return service.listar(names);
    }

    @PostMapping("/{nombre}/purge")
    public PurgaResponse purgar(@PathVariable String nombre) {
        return new PurgaResponse(nombre, service.purgarCola(nombre));
    }
}
