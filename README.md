# ms-barriodigital-rabbitadmin

Microservicio **administrador de RabbitMQ** (Caso §5). Spring Boot 3.3.5 / Java 17,
puerto **8086**, **sin base de datos**: es plano de control y es stateless.

Matriz de funcionalidades del proyecto:
[barriodigital-docs/Cumplimiento-Prueba2.md](https://github.com/PhamNukz/barriodigital-docs/blob/main/Cumplimiento-Prueba2.md).

## Por qué es un repo aparte

`ms-barriodigital-notify` ya es dueño de la topología y ya tiene la dependencia AMQP,
así que era el candidato obvio. No se hizo ahí, y la razón es de diseño: `notify` es un
worker del **plano de datos** y esto es **plano de control**. `notify` hoy no expone HTTP
en absoluto; darle endpoints que crean y borran colas convertiría al único componente sin
superficie de red en el más peligroso del sistema. El detalle completo, con sus
contraindicaciones y el costo que se acepta, está en
[Justificacion-Mensajeria.md §3](../barriodigital-docs/Justificacion-Mensajeria.md).

## La regla: la topología base es código, lo operativo es API

`notify` declara la topología con `@Bean` y **la re-aplica en cada arranque**. Este
servicio la **muta en runtime**. Son dos modelos en tensión y tienen una consecuencia
observable que conviene saber antes de que sorprenda:

> una cola borrada con `DELETE /admin/queues/q.cmd.email` **reaparece** cuando `notify` reinicia.

No es un bug. Este servicio sirve para **crear colas nuevas, purgar, inspeccionar y
reparar**; no para redefinir la topología del sistema, que vive en el repositorio y se
versiona.

## API

Todo `/admin/**` exige rol **Admin** (JWT de Entra ID, claim `roles`). La regla está en
un solo lugar, `SecurityConfig`, y no repartida en anotaciones por método.

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/admin/queues` | Crea una cola. Cuerpo: `nombre`, `durable` (opcional, por defecto `true`), `deadLetterExchange`, `deadLetterRoutingKey` |
| `DELETE` | `/admin/queues/{nombre}` | Borra la cola. `404` si no existía |
| `GET` | `/admin/queues[?names=a,b]` | Mensajes y consumidores de cada cola |
| `POST` | `/admin/queues/{nombre}/purge` | Vacía la cola y devuelve cuántos mensajes descartó |
| `POST` | `/admin/exchanges` | Crea un exchange. `tipo`: `direct`, `topic`, `fanout` o `headers` |
| `DELETE` | `/admin/exchanges/{nombre}` | Borra el exchange. `404` si no existía |
| `POST` | `/admin/bindings` | Liga cola ↔ exchange con una routing key |
| `DELETE` | `/admin/bindings` | Quita ese binding (cuerpo con la misma terna) |

### Por qué `GET /admin/queues` recibe los nombres

AMQP 0-9-1 **no tiene operación de "listar colas"**: enumerar la topología es cosa del
plano HTTP de management (`:15672/api/queues`), no del protocolo. Como este servicio
administra por AMQP —vía `AmqpAdmin`—, el listado inspecciona las colas que se le
nombren: con `?names=a,b` las que pidas, y sin parámetro las de
`barriodigital.rabbitadmin.colas-conocidas` en `application.yml`, que vienen cargadas
con las 6 del proyecto.

## Errores

Todo sale como `ProblemDetail` con un `detail` que se puede leer sin abrir el código:

```
POST /admin/queues  {"nombre": "cola con espacios"}
400  {"title":"Entrada invalida",
      "detail":"nombre: el nombre de la cola solo admite letras, digitos, punto,
                guion, guion bajo y dos puntos (sin espacios)"}
```

`404` cuando la cola o el exchange no existen, y `502` cuando el broker rechaza la
operación —por ejemplo `PRECONDITION_FAILED` al redeclarar algo que ya existe con otros
argumentos—, porque ese fallo es del sistema de abajo y no de este servicio.

## Correr y probar

```bash
mvn -B verify
```

Los tests **no necesitan RabbitMQ**: `AmqpAdmin` se mockea. Cubren la validación con sus
mensajes, la seguridad (`401` sin token, `403` sin rol Admin, `200`/`201` con Admin) y el
servicio contra un `AmqpAdmin` falso.

Variables de entorno: `RABBITMQ_ADDRESSES` (lista `host:puerto` de los dos nodos, para
failover; si falta se usan `RABBITMQ_HOST` y `RABBITMQ_PORT`), `RABBITMQ_USER`,
`RABBITMQ_PASSWORD`, más las de Entra (`AAD_ISSUER_URI`, `AAD_API_CLIENT_ID`,
`AAD_REQUIRED_SCOPE`). En `ec2-apps` el broker se alcanza por IP privada; `sg-mq` ya
permite 5672 desde `sg-apps`, así que no hay recurso AWS que crear.

## `/actuator/health` baja a 503 si el broker no responde

Es deliberado: `spring-boot-starter-amqp` agrega el health indicator de RabbitMQ y un
administrador de RabbitMQ que no alcanza el broker está genuinamente DOWN. Vale tenerlo
presente al escribir el healthcheck del contenedor en **D8**: si se usa `/actuator/health`,
este servicio quedará `unhealthy` cada vez que `ec2-mq` esté caído, que es justo cuando más
se lo quiere a mano para diagnosticar. Si eso molesta, el healthcheck del compose puede
apuntar a `/actuator/health/liveness` en vez de al agregado.

## Pendiente

`Dockerfile`, `ci.yml`, `build-image.yml` y la entrada en `apps/compose.yml` con
`-Xmx192m` son los ítems **D7–D10** del backlog, de la ola siguiente.
