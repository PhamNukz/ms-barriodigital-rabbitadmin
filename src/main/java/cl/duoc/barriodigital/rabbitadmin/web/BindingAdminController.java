package cl.duoc.barriodigital.rabbitadmin.web;

import cl.duoc.barriodigital.rabbitadmin.service.RabbitAdminService;
import cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.BindingRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/bindings")
public class BindingAdminController {

    private final RabbitAdminService service;

    public BindingAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void crear(@Valid @RequestBody BindingRequest req) {
        service.crearBinding(req.cola(), req.exchange(), req.routingKey());
    }

    /** DELETE con cuerpo: un binding se identifica por la terna, no por un id. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Valid @RequestBody BindingRequest req) {
        service.eliminarBinding(req.cola(), req.exchange(), req.routingKey());
    }
}
