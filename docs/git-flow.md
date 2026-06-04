# Git Flow sugerido

Ramas recomendadas para evidenciar Git Flow:

1. `main`: version estable entregada.
2. `develop`: integracion de funcionalidades.
3. `feature/products-service`: microservicio Spring Boot de productos.
4. `feature/inventory-service`: microservicio Spring Boot de inventario y compras.
5. `feature/vue-client`: cliente Vue para operar la prueba.
6. `feature/docker-docs`: Docker Compose, OpenAPI y README.
7. `release/v1.0.0`: validacion final antes de fusionar a `main`.

Comandos de referencia:

```bash
git checkout -b develop
git checkout -b feature/products-service
git checkout develop
git merge --no-ff feature/products-service
git checkout -b feature/inventory-service
git checkout develop
git merge --no-ff feature/inventory-service
git checkout -b feature/vue-client
git checkout develop
git merge --no-ff feature/vue-client
git checkout -b release/v1.0.0
git checkout main
git merge --no-ff release/v1.0.0
git tag v1.0.0
```

