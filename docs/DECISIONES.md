# Decisiones y límites

## Un servicio y una base de datos

Una API pequeña no justifica microservicios. El dominio se organiza por funcionalidades con separación entre HTTP, caso de uso y persistencia. Se puede ampliar sin introducir infraestructura distribuida antes de necesitarla.

## Java 17 / Spring Boot 3.5

Versión LTS de Java compatible con Spring Boot 3.5 y disponible en el entorno de construcción. Se usa un parche concreto (3.5.16) para reproducibilidad. Dependencias gestionadas por el BOM de Boot excepto springdoc. Revisar versiones y vulnerabilidades antes de exponerlo públicamente.

## Consultar sin reservar

La consulta es de solo lectura y puede repetirse. Su resultado refleja el estado de la base en ese momento; no garantiza stock al realizar una futura compra. Para reservar haría falta un caso de uso transaccional separado, con control de concurrencia sobre el stock e idempotencia.

## Concurrencia

Las ofertas llevan `@Version`. El cliente envía la versión que ha leído; una versión obsoleta produce 409. Además del chequeo explícito, Hibernate compara la versión en el UPDATE/DELETE para detectar carreras entre transacciones simultáneas. La base aplica unicidad de producto/proveedor/región incluso bajo concurrencia.

## Importes y fechas

BigDecimal y NUMERIC(12,2) evitan errores de coma flotante. Se rechazan más de dos decimales. Los plazos usan días naturales y UTC; un calendario laboral real se modelaría como otra regla y tendría pruebas propias.

## Seguridad de la demo

Separa lectura pública de administración. La API usa un token de administración único, aleatorio y configurable, comparado mediante hashes SHA-256 de longitud fija y comparación constante. No se almacena en código ni se registra. No hay JWT ni login ficticio: para este único administrador no aportan valor.

No se usan sesiones, HTTP Basic ni cookies. CSRF se desactiva porque la credencial debe ser un Bearer explícito y no se adjunta automáticamente por el navegador. CORS permite solo GET desde el origen configurado y no admite credenciales. CORS no es autenticación ni impide peticiones desde curl.

Es una solución de portfolio, no un sistema de identidades multiusuario. Si se amplía: OAuth2/OIDC, permisos individuales, rotación sin interrupción y auditoría de modificaciones. Por ahora rotar el token requiere reiniciar la API.

## Antes de publicación pública

- HTTPS en el proxy o plataforma; no enviar tokens por HTTP público.
- PostgreSQL en una red privada y secretos gestionados por el alojamiento.
- Límites de peticiones y tamaño de peticiones en el proxy; esta versión no incluye rate limiting.
- Configurar el origen exacto del portfolio y comprobar que no se incrusta la clave en el cliente.
- Probar PostgreSQL y la imagen Docker en el entorno de destino.
- Definir presupuesto, copias de seguridad, observabilidad y estrategia de recuperación si se conserva información importante.

## Rendimiento

Hay un índice para producto/región/stock y una consulta con fetch del proveedor para evitar un N+1 en la recomendación. La selección final se realiza sobre las ofertas elegibles en memoria. Este catálogo es pequeño: no se atribuyen garantías de latencia ni se incluyen benchmarks inventados. Arrow, caché o una consulta SQL limitada se evaluarían tras medir un problema real.
