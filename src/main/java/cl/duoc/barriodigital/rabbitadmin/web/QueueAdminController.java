package cl.duoc.barriodigital.rabbitadmin.web;

import cl.duoc.barriodigital.rabbitadmin.service.RabbitAdminService;
import cl.duoc.barriodigital.rabbitadmin.service.RabbitAdminService.InfoCola;
import cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.ColaCreadaResponse;
import cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.CrearColaRequest;
import cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.PurgaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.MENSAJE_NOMBRE;
import static cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.NOMBRE_VALIDO;

/**
 * Acceso restringido a Admin en SecurityConfig, para todo /admin/**.
 * {@code @Validated} hace que tambien se validen los nombres de la ruta y de
 * {@code ?names=}, no solo los del cuerpo.
 */
@Validated
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
    public void eliminar(@PathVariable
                         @Size(max = 255, message = "el nombre no puede superar los 255 caracteres")
                         @Pattern(regexp = NOMBRE_VALIDO, message = "el nombre de la cola " + MENSAJE_NOMBRE)
                         String nombre) {
        service.eliminarCola(nombre);
    }

    /**
     * @param names colas a inspeccionar. Si no viene, se usan las declaradas en
     *              configuracion: AMQP no permite enumerar colas.
     */
    @GetMapping
    public List<InfoCola> listar(@RequestParam(required = false)
                                 List<@Pattern(regexp = NOMBRE_VALIDO,
                                         message = "cada nombre en 'names' " + MENSAJE_NOMBRE) String> names) {
        return service.listar(names);
    }

    @PostMapping("/{nombre}/purge")
    public PurgaResponse purgar(@PathVariable
                                @Size(max = 255, message = "el nombre no puede superar los 255 caracteres")
                                @Pattern(regexp = NOMBRE_VALIDO, message = "el nombre de la cola " + MENSAJE_NOMBRE)
                                String nombre) {
        return new PurgaResponse(nombre, service.purgarCola(nombre));
    }
}
