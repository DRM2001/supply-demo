# Supply Demo — Backend de aprovisionamiento

API REST independiente del portfolio de Daniel Rodríguez. Implementación nueva y demostrativa inspirada en el problema de seleccionar proveedores. **Todos los productos, proveedores, precios y stocks son ficticios. No se incluye código ni información de empleadores.**

## Qué demuestra

- Java 17, Spring Boot 3.5, Spring MVC, Bean Validation y DTOs con records.
- PostgreSQL, Spring Data JPA, migraciones Flyway y restricciones en base de datos.
- Reglas de selección aisladas de Spring, importes BigDecimal y reloj inyectable.
- Consultas públicas y administración con token Bearer, sin sesiones ni cookies.
- Control de concurrencia con `@Version` y conflictos HTTP 409.
- OpenAPI/Swagger, pruebas unitarias e integración, Docker y pipeline de CI.

La web CV no cambia: este repositorio contiene solo el backend. No es una réplica del sistema profesional original. No incorpora Arrow ni Kafka: el catálogo reducido no justifica esa complejidad.

## Arranque rápido con Docker Desktop

Necesitas Docker Desktop iniciado, con Docker Compose. Los comandos siguientes funcionan en PowerShell:

```powershell
Copy-Item .env.example .env
```

Edita `.env`: sustituye la contraseña de PostgreSQL y la clave de administración. Usa una clave aleatoria de al menos 32 caracteres; puedes generar una de 64 caracteres así:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
$rng.Dispose()
```

No compartas el resultado ni lo subas al repositorio. Pégalo como valor de `ADMIN_TOKEN` en `.env` y arranca:

```powershell
docker compose up --build -d
docker compose logs -f api
```

Cuando termine el arranque:

- Swagger: http://localhost:8080/swagger-ui/index.html
- Contrato OpenAPI: http://localhost:8080/v3/api-docs
- Salud: http://localhost:8080/actuator/health

Primera consulta (PowerShell):

```powershell
Invoke-RestMethod 'http://localhost:8080/api/v1/recommendations?sku=DEMO-001&region=MADRID&quantity=10&strategy=CHEAPEST'
```

Resultado esperado: proveedor 1, precio unitario 12.50, total 125.00, plazo 3 días. Con `strategy=FASTEST`, gana el proveedor 2 (14.00, un día). La fecha se calcula en cada petición.

Para detener sin borrar datos: `docker compose down`. La base de datos persiste en el volumen `supply-data`. No es necesario borrar el volumen para ejecutar pruebas.

## Ejecutar desde el IDE

Requiere JDK 17 o superior. Arranca solamente la base de datos:

```powershell
docker compose up -d db
```

Abre `pom.xml` como proyecto Maven y configura estas variables en la configuración de ejecución de `SupplyApplication`:

```text
DB_URL=jdbc:postgresql://localhost:5432/supply
DB_USER=supply
DB_PASSWORD=<la contraseña que has puesto en .env>
ADMIN_TOKEN=<tu clave aleatoria de al menos 32 caracteres>
CORS_ORIGIN=http://localhost:3000
```

**Spring Boot no lee `.env` automáticamente.** Docker Compose sí lo usa para interpolar `compose.yml`; al arrancar desde el IDE debes configurar las variables explícitamente. No añadas dotenv ni subas archivos con secretos.

También puedes ejecutar `./mvnw spring-boot:run` en Linux/macOS o `.\mvnw.cmd spring-boot:run` en Windows, con esas variables en el entorno del proceso.

## Endpoints

| Método | Ruta | Acceso | Propósito |
| --- | --- | --- | --- |
| GET | `/api/v1/products?page=0&size=20` | Público | Catálogo paginado; tamaño máximo 100 |
| GET | `/api/v1/suppliers` | Público | Proveedores ficticios |
| GET | `/api/v1/regions` | Público | Valores válidos de región |
| GET | `/api/v1/recommendations` | Público | Recomendar para sku, region, quantity y strategy |
| GET | `/api/v1/admin/offers/{id}` | Bearer | Consultar oferta y versión |
| POST | `/api/v1/admin/offers` | Bearer | Crear una oferta |
| PUT | `/api/v1/admin/offers/{id}` | Bearer | Actualizar precio, stock y plazo con versión |
| DELETE | `/api/v1/admin/offers/{id}?version=0` | Bearer | Borrar verificando versión |

Regiones: `MADRID`, `ANDALUCIA`, `CATALUNA`. Estrategias: `CHEAPEST` (por defecto), `FASTEST`. Cantidad: 1–10.000. Los valores de enum y SKU distinguen mayúsculas/minúsculas.

Consulta siempre primero la versión de la oferta antes de modificarla. El token se envía como `Authorization: Bearer <ADMIN_TOKEN>`. Swagger permite introducirlo con Authorize y no lo persiste. No introduzcas el token en el frontend público ni en el JavaScript del portfolio.

Ejemplo de alta (hay una combinación libre para DEMO-003):

```json
{
  "sku": "DEMO-003",
  "supplierId": 1,
  "region": "MADRID",
  "unitPrice": 5.50,
  "stock": 20,
  "leadDays": 2
}
```

Ejemplo de actualización:

```json
{
  "version": 0,
  "unitPrice": 6.00,
  "stock": 10,
  "leadDays": 1
}
```

El `POST` devuelve 201 y `Location`; el `PUT` devuelve la nueva versión. Repetir una modificación con la versión anterior devuelve 409. La combinación producto/proveedor/región es única.

## Reglas de negocio

1. Se filtran ofertas por producto y región y con stock suficiente para toda la cantidad solicitada.
2. `CHEAPEST`: menor precio, después menor plazo. `FASTEST`: menor plazo, después menor precio.
3. Los empates se resuelven por ID de proveedor y de oferta para que el resultado sea reproducible.
4. El total es precio unitario por cantidad con BigDecimal. Moneda única EUR; no hay impuestos, gastos de envío ni descuentos.
5. Los plazos son días naturales desde la fecha UTC de la petición.
6. Una recomendación **no compra, reserva ni descuenta stock**. No se reparte una cantidad entre proveedores.

Errores con formato Problem Details: 400 parámetros inválidos, 401 token ausente/incorrecto, 403 acceso no permitido, 404 recurso inexistente, 409 conflicto, 422 sin stock suficiente. Nunca se devuelven consultas SQL, claves ni stack traces al cliente.

## Pruebas

```powershell
.\mvnw.cmd verify
```

En Linux/macOS: `./mvnw verify`. Ejecuta pruebas unitarias y de integración HTTP/JPA con H2 en modo PostgreSQL, sin depender de Docker. No demuestra por sí solo compatibilidad PostgreSQL.

Para ejecutar el mismo contrato contra PostgreSQL real con Testcontainers (Docker Desktop debe estar iniciado):

```powershell
.\mvnw.cmd verify -Ppostgres-it
```

El workflow incluido está configurado para ejecutar este segundo comando en GitHub Actions cuando subas el proyecto a un repositorio. No omite silenciosamente PostgreSQL si falta Docker. Informes: `target/surefire-reports/`, `target/failsafe-reports/` y cobertura en `target/site/jacoco/index.html`. No se afirma un porcentaje mínimo de cobertura.

## Arquitectura

`web` contiene controladores y errores; `catalog` y `offer`, las entidades y repositorios; `recommendation`, el caso de uso y la política de selección; `config`, seguridad, CORS y OpenAPI; `domain`, enums compartidos.

Controller → Service (`@Transactional`) → Repository → PostgreSQL.

La política pura `SelectionPolicy` permite probar las reglas sin arrancar Spring. `Clock` permite verificar fechas sin depender del día de ejecución. JPA usa `open-in-view=false`; las entidades no se exponen como respuestas HTTP.

Más detalles en [docs/DECISIONES.md](docs/DECISIONES.md). Despliegue separado y conexión al portfolio en [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md).

## Alcance y honestidad del portfolio

Este es un proyecto de demostración construido con asistencia de IA. Antes de presentarlo como evidencia de tus conocimientos, ejecútalo, revisa las decisiones, modifica reglas y entiende sus pruebas. No lo describas como experiencia en producción. El objetivo es que puedas explicar y defender cada parte.

## Demo pública: preparación

Se incluye render.yaml para desplegar la API en el plan Free de Render y conectarla a PostgreSQL de Neon. Consulta docs/DESPLIEGUE.md. Este archivo no despliega por sí solo: requiere un repositorio y las cuentas de alojamiento.
