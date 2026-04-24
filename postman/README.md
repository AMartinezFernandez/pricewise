# Colección Postman — PriceWise API

Colección completa del API REST del backend, sincronizada en disco como YAML (formato [Postman CLI / Git integration](https://learning.postman.com/docs/collections/using-git-with-postman/)) para poder versionarla con el resto del proyecto.

## Contenido

```
postman/
├── collections/
│   └── PriceWise API/
│       ├── .resources/definition.yaml     # Configuración de la colección
│       ├── Autenticación/                 # Login, registro, perfil
│       ├── Health/                        # Health check
│       ├── Productos/                     # CRUD y monitorización
│       ├── Competidores (Keepa)/          # Sincronización con Amazon
│       ├── API Keys/                      # Gestión de API keys de Keepa por empresa
│       ├── Scheduler (Admin)/             # Jobs Quartz
│       └── Admin/                         # Gestión de usuarios y estadísticas
└── globals/
    └── workspace.globals.yaml             # Variables globales (vacío)
```

Cada subcarpeta agrupa las peticiones de un módulo. Cada archivo `*.request.yaml` es una petición HTTP.

## Cómo importar la colección

### Opción A — Postman Desktop (recomendado)

1. Abre Postman.
2. `File → Import` (o `Cmd/Ctrl + O`).
3. Arrastra la carpeta completa `postman/collections/PriceWise API/` sobre el diálogo.
4. Postman detecta el formato YAML y reconstruye la colección con todas sus subcarpetas.

### Opción B — Postman CLI

```bash
postman collection import postman/collections/PriceWise\ API/
```

## Configuración previa

La colección define dos variables en `.resources/definition.yaml`:

| Variable   | Valor por defecto        | Uso                                               |
|------------|--------------------------|---------------------------------------------------|
| `baseUrl`  | `http://localhost:9090`  | URL del backend. Cámbiala si usas Railway o otro. |
| `token`    | `""`                     | JWT que se rellena automáticamente en el login.   |

Para apuntar al backend desplegado, edita `baseUrl` en la pestaña **Variables** de la colección dentro de Postman.

## Flujo de uso

1. **Ejecuta `Autenticación / Login`** con las credenciales del tribunal.
2. Un script de test guarda el JWT en la variable `token` de la colección.
3. El resto de peticiones ya llevan `Authorization: Bearer {{token}}` configurado a nivel de colección. No hay que copiar el token manualmente.

Si recibes `401 Unauthorized`, el token ha caducado — repite el paso 1.

## Credenciales

Las credenciales de demostración se facilitan bajo demanda al tribunal. Consulta la descripción del [release v1.0.0](https://github.com/AMartinezFernandez/pricewise/releases/tag/v1.0.0).

## Referencia completa de endpoints

Consulta [`../docs/README.md`](../docs/README.md) para la descripción detallada de cada endpoint, parámetros, roles requeridos y ejemplos de respuesta.
