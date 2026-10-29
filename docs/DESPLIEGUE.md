# Desplegar separado del portfolio

El código y Compose están preparados, pero crear este proyecto **no crea un servidor público ni contrata alojamiento**. Sites ejecuta su propio runtime; no es el destino de este proceso Java. El frontend y la API se despliegan por separado.

## Contrato del alojamiento

Necesitas un servicio que ejecute una imagen Docker Java y una instancia PostgreSQL accesible por JDBC. Configuración:

- Comando de construcción: `docker build -t supply-demo .`
- Puerto interno: 8080, o variable `PORT` del alojamiento.
- Salud: `/actuator/health/readiness`.
- `DB_URL`: `jdbc:postgresql://HOST:5432/BASE` (opciones TLS según el proveedor).
- `DB_USER`, `DB_PASSWORD`: usuario y contraseña de la base.
- `ADMIN_TOKEN`: clave aleatoria de al menos 32 caracteres como secreto.
- `CORS_ORIGIN`: origen exacto de la web, sin barra final ni rutas.

El proceso aplica Flyway antes de servir tráfico. El rol de base de datos necesita crear y modificar el esquema. Para una operación real conviene separar el rol de migraciones del de ejecución. Los datos ficticios se insertan una sola vez con V2, no en cada reinicio.

La imagen corre con un usuario sin privilegios. El Compose de desarrollo solo publica puertos en 127.0.0.1 y no incluye TLS: no lo expongas directamente como despliegue público.

## Verificación de despliegue

1. Ejecuta `./mvnw verify -Ppostgres-it` con Docker disponible.
2. Construye la imagen y prueba Compose localmente.
3. Configura secretos y PostgreSQL, publica con HTTPS y comprueba `/actuator/health`.
4. Comprueba dos recomendaciones (CHEAPEST/FASTEST) y que una ruta admin sin clave devuelve 401.
5. Verifica desde el navegador del portfolio que CORS permite la lectura.
6. Mantén toda modificación admin fuera de la web pública.

## Conexión futura desde la web

Este fragmento es orientativo; `API_BASE` debe ser la URL HTTPS real que devuelva el alojamiento. La conexión no está añadida al portfolio todavía.

```javascript
const params = new URLSearchParams({
  sku: 'DEMO-001', region: 'MADRID', quantity: '10', strategy: 'CHEAPEST'
});
const response = await fetch(`${API_BASE}/api/v1/recommendations?${params}`);
const body = await response.json();
if (!response.ok) throw new Error(body.detail ?? 'No se pudo consultar');
// Mostrar proveedor, precio, plazo y fecha con textContent, no con innerHTML.
```

No hay ninguna clave necesaria para la consulta pública. La demo web debe ofrecer un formulario para seleccionar producto, región, cantidad y estrategia, y mostrar estados de carga/error. El backend puede evolucionar y desplegarse sin reconstruir la web, manteniendo el contrato de `/api/v1`.

## Configuración preparada para Render + Neon

`render.yaml` declara una API Docker en el plan gratuito de Render, con región Frankfurt. No crea una base de datos en Render. Hay que crear PostgreSQL aparte en Neon y proporcionar DB_URL (formato JDBC), DB_USER y DB_PASSWORD en Render. Usa una conexión directa de Neon para esta primera versión y conserva las opciones TLS que correspondan al driver JDBC.

El Blueprint genera una clave ADMIN_TOKEN aleatoria en Render; no reutiliza ni publica la clave local. Configura CORS_ORIGIN con el origen del portfolio, sin barra final. El perfil cloud reduce el pool a tres conexiones y permite liberar las ociosas; el entorno local mantiene su configuración actual.

Pasos una vez conectado GitHub:

1. Publicar el proyecto en un repositorio, excluyendo .env y target.
2. Crear el proyecto Neon en una región europea próxima y obtener credenciales JDBC.
3. Crear el servicio a partir de render.yaml y completar los secretos solicitados. Confirmar que se selecciona Free y no añadir recursos de pago.
4. Verificar salud, Swagger, recomendación pública y rechazo admin sin token.
5. Conectar el formulario de la web a la URL real de la API.

La instancia gratuita puede suspenderse por inactividad. La interfaz debe tolerar el arranque en frío con un estado de carga y posibilidad de reintento. Este archivo no garantiza disponibilidad continua ni límites gratuitos futuros.
