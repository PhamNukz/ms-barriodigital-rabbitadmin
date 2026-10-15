package cl.duoc.barriodigital.rabbitadmin.web;

import cl.duoc.barriodigital.rabbitadmin.service.RabbitAdminService;
import cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.CrearExchangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/exchanges")
public class ExchangeAdminController {

    private final RabbitAdminService service;

    public ExchangeAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void crear(@Valid @RequestBody CrearExchangeRequest req) {
        service.crearExchange(req.nombre(), req.tipo());
    }

    @DeleteMapping("/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String nombre) {
        service.eliminarExchange(nombre);
    }
}
