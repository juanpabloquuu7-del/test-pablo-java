# Integración y cierre

Cuando cada área está terminada (o por módulo, a medida que se entregan), se junta todo en `develop`.

## Integración continua por módulo

- Cada dev abre PR `dev-x` -> `develop` con cada módulo terminado. No hace falta esperar a que termine todo el área.
- Como los archivos son distintos, los PR no chocan. Los únicos archivos que tocan ambos son los de registro (`AreaAModules.java` y `AreaBModules.java`, uno por dev) y las secciones separadas de `config.properties`.
- Tras cada merge a `develop`, el otro dev hace `git pull --rebase origin develop` y comprueba que sigue compilando.

## Antes del primer PR de cada área

- [ ] `mvn clean compile` sin errores.
- [ ] El módulo aparece en el menú principal sin la marca "(en desarrollo)".
- [ ] Probado con los datos de `02-data-base.sql` y los propios.

## Cierre (cuando las dos áreas están en `develop`)

| Quién | Tarea |
|---|---|
| Dev A | Generar `docs/database.sql` unificado concatenando `01-schema.sql`, `02-data-base.sql`, `10-extras-a.sql`, `11-data-a.sql`, `20-extras-b.sql` y `21-data-b.sql` (en ese orden) y probarlo sobre una base limpia |
| Dev B | Ensamblar `README.md` con `docs/readme-a.md` y `docs/readme-b.md`: título, descripción, autores, tecnologías, requisitos, configuración de BD, instalación y ejecución, estructura, patrones, funcionalidades, errores y logs, limitaciones, licencia |
| Los dos | Revisión cruzada: cada uno prueba el área del otro con la lista de abajo |
| Dev A | PR `develop` -> `main` y `mvn clean package` (genera el jar) |
| Dev B | Aprobar el PR, `git tag v1.0.0` |

## Lista de pruebas cruzadas

1. Matricular un estudiante con dos acudientes y un hermano; verificar descuento por hermanos en la pensión.
2. Registrar notas de los cuatro períodos y comprobar los promedios.
3. Marcar 4 ausencias en una semana y ver la alerta.
4. Intentar inscribir en un club lleno: debe rechazarse.
5. Registrar un pago y abrir el PDF de la factura (CUFE y QR presentes).
6. Prestar un recurso, intentar prestarlo otra vez (debe rechazarse), devolverlo.
7. Reporte de morosos y de préstamos vencidos con datos reales.
8. Entradas inválidas en cada menú: letras donde van números, fechas mal escritas, nota 5.5.
9. Un error de base de datos (apagar MySQL) no cierra el programa y queda en `logs/error_log.txt`.

Los hallazgos se abren como issues; los corrige el dueño del módulo y el otro dev verifica.

## Criterio de salida

- Lista de pruebas sin fallos abiertos.
- `mvn clean package` genera el jar y arranca contra una base limpia creada con `docs/database.sql`.
- README completo y etiqueta `v1.0.0` en `main`.
