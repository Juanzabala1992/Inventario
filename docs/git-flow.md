# Git Flow implementado

El repositorio utiliza las siguientes ramas:

1. `main`: version estable entregada.
2. `develop`: integracion de funcionalidades.
3. `feature/products-service`: microservicio Spring Boot de productos.
4. `feature/inventory-service`: microservicio Spring Boot de inventario y compras.
5. `feature/vue-client`: cliente Vue para operar la prueba.
6. `feature/docs-and-deployment`: Docker Compose, OpenAPI, Postman y README.
7. `release/v1.0.0`: validacion final antes de fusionar a `main`.
8. `feature/docs-git-flow`: explicacion de las ramas y del proceso aplicado.
9. `release/v1.0.1`: publicacion de la actualizacion documental.

Flujo aplicado:

```bash
git checkout -b feature/nombre develop
# implementar, validar y confirmar cambios
git checkout develop
git merge --no-ff feature/nombre

git checkout -b release/vX.Y.Z develop
# ejecutar pruebas y build
git checkout main
git merge --no-ff release/vX.Y.Z
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin main develop --tags
```

Los merges `--no-ff` conservan un commit de integracion por funcionalidad y
permiten identificar claramente el origen de cada cambio en el historial.

