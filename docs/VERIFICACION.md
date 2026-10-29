# Verificación de esta entrega

- Compilación y empaquetado: `mvn verify`, resultado BUILD SUCCESS.
- 25 pruebas ejecutadas; 0 fallos, 0 errores, 0 omitidas.
- 6 pruebas de selección, 2 de servicio y 17 de integración HTTP/JPA con H2.
- Migraciones Flyway y validación de esquema JPA comprobadas con H2 en modo PostgreSQL.
- Maven Wrapper para Windows y Linux incluido.
- Pruebas contra PostgreSQL real: preparadas en PostgresIT, no ejecutadas en este entorno por falta de Docker.
- Imagen y Compose: incluidos, no construidos ni ejecutados aquí por falta de Docker.
- CI: workflow incluido para GitHub Actions; comprobar el resultado de la ejecución remota antes de afirmar que PostgreSQL está verificado.
- Alojamiento público y conexión al portfolio: pendientes.

No se incluyen credenciales reales ni información empresarial. El proyecto usa datos ficticios.

## Preparación de alojamiento

Añadidos render.yaml y el perfil cloud, y ampliada la guía. YAML validado estructuralmente. Las 25 pruebas previas corresponden al código Java, que no cambia en esta revisión. El perfil cloud y el despliegue deben comprobarse en el alojamiento; La base de datos Neon se ha creado; el despliegue de la API sigue pendiente.
