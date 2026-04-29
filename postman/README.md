# Colección Postman de la API de PriceWise

Colección completa del API REST del backend, sincronizada en disco como YAML para versionarla con el resto del proyecto. Formato compatible con la integración de Git en Postman.

## Contenido

```
postman/
├── collections/
│   └── PriceWise API/
│       ├── .resources/definition.yaml
│       ├── Autenticación/
│       ├── Health/
│       ├── Productos/
│       ├── Competidores (Keepa)/
│       ├── API Keys/
│       ├── Scheduler (Admin) [DEPRECATED]/
│       └── Admin/
└── globals/
    └── workspace.globals.yaml
```

Cada subcarpeta agrupa las peticiones de un módulo. Cada archivo `*.request.yaml` es una petición HTTP.

## Importar la colección

Opción A, Postman Desktop:

1. Abrir Postman.
2. `File` → `Import` (o `Cmd/Ctrl + O`).
3. Arrastrar la carpeta `postman/collections/PriceWise API/` al diálogo.
4. Postman reconstruye la colección con todas sus subcarpetas.

Opción B, Postman CLI:

```bash
postman collection import postman/collections/PriceWise\ API/
```

## Variables

Definidas en `.resources/definition.yaml`:

1. `baseUrl`, valor por defecto `http://localhost:9090`. URL del backend, cambiarla si se apunta a Railway u otro entorno.
2. `token`, vacío por defecto. JWT que se rellena automáticamente tras el login.

Para apuntar al backend desplegado, editar `baseUrl` en la pestaña Variables de la colección.

## Flujo de uso

1. Ejecutar `Autenticación / Login` con las credenciales facilitadas.
2. Un script de test guarda el JWT en la variable `token`.
3. El resto de peticiones llevan `Authorization: Bearer {{token}}` configurado a nivel de colección.

Si llega `401 Unauthorized`, el token ha caducado: repetir el paso 1.

## Credenciales

Credenciales de demostración disponibles bajo demanda. Ver la descripción del release v1.0.0 en GitHub.

## Endpoints deprecados

La carpeta `Scheduler (Admin) [DEPRECATED]/` contiene cuatro peticiones (`Estado`, `Ejecutar Job Ahora`, `Pausar`, `Reanudar`) que apuntan a `/api/admin/scheduler/*`. Esos endpoints no existen en la versión actual del backend. Devuelven 404.

Se conservan en la colección como referencia. Los jobs de Quartz (`PriceMonitorJob`) se ejecutan automáticamente cada 6 horas en segundo plano sin necesidad de interfaz REST.

## Referencia de endpoints

Ver `../docs/README.md` para parámetros, roles requeridos y ejemplos de respuesta.
