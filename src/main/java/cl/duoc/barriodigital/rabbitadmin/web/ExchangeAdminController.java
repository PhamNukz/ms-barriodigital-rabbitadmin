package cl.duoc.barriodigital.rabbitadmin.web;

import cl.duoc.barriodigital.rabbitadmin.service.RabbitAdminService;
import cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.CrearExchangeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.MENSAJE_NOMBRE;
import static cl.duoc.barriodigital.rabbitadmin.web.AdminDtos.NOMBRE_VALIDO;

/** {@code @Validated}: el nombre de la ruta se valida igual que el del cuerpo. */
@Validated
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
    public void eliminar(@PathVariable
                         @Size(max = 255, message = "el nombre no puede superar los 255 caracteres")
                         @Pattern(regexp = NOMBRE_VALIDO, message = "el nombre del exchange " + MENSAJE_NOMBRE)
                         String nombre) {
        service.eliminarExchange(nombre);
    }
}
