![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-brightgreen)

# OpenAPI

- Source export in repo: [`api-docs.json`](api-docs.json)
- Local endpoint when app is running: `GET /v3/api-docs`
- Swagger UI: `/swagger-ui/index.html`

## Refresh the stored spec

1. Run backend locally:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

2. Export current spec:

```bash
curl -sS http://localhost:8081/v3/api-docs -o docs/openapi/api-docs.json
```
