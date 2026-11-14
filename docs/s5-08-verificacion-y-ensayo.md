# S5-08 - Control de versión y acta de ensayo

## Estado documental al 6 de noviembre de 2026

- Base revisada: `3b4380d062de6154ad1f71c6ceb9d3faadfcf883`.
- Manual S4-12 actualizado con registro pendiente, acceso y moderación S5-09.
- Guía Ubuntu y guion preparados a partir de archivos del repositorio.
- Gustavo confirmó acceso al login, al dashboard y health con servicios UP en
  `https://srv01.tailb2f071.ts.net`. Son comprobaciones reportadas por el usuario;
  no se inspeccionó la configuración interna del servidor Debian.
- Se realizó un ensayo parcial: E01 a E04 aprobados. El commit reportado
  del servidor coincide con la base documental; la configuración completa
  y el resto del ensayo siguen pendientes de verificación.

## Dependencias y evidencia disponible

| Elemento | Evidencia revisada | Límite |
|---|---|---|
| S5-06 | Responsable reportó commit, entorno Debian, puertos y comando de arranque | Pendiente revisar override y configuración HTTPS |
| S5-07 | Aprobada según confirmación de Gustavo | No se adjuntó el reporte MySQL ni el commit exacto probado |
| CI de main | Job `test` exitoso para 3b4380d | No hay job MySQL en el workflow de esa versión |
| Pruebas locales | Gustavo reportó 907 pruebas, sin fallos, errores ni omitidas | No acredita MySQL ni el servidor desplegado |
| Seed + Docker | Perfil prod en Compose; seed exige demo y recursos de fotos externos al contexto backend | Contrastar con los ajustes reales de S5-06 |

Fuentes:
- https://github.com/ArturoDev25/RentaDeAutos/issues/154
- https://github.com/ArturoDev25/RentaDeAutos/pull/163
- https://github.com/ArturoDev25/RentaDeAutos/actions/runs/42151076184/job/127674189501

## 1. Ficha del entorno real

| Campo | Valor |
|---|---|
| Servidor | srv01 |
| Fecha de despliegue reportada | 2026-11-04 |
| Sistema operativo | Debian; versión no proporcionada |
| Docker | 29.9.0 |
| Docker Compose | 5.6.0 |
| Ruta del proyecto | `~/RentaDeAutos` |
| Rama desplegada | `main` |
| Commit desplegado | `3b4380d062de6154ad1f71c6ceb9d3faadfcf883` |
| Estado de Git reportado | Limpio, sin cambios locales |
| Comando de arranque | `docker compose up -d --build` |
| Servicios | MySQL 8.0, backend Spring Boot con Java 21 y frontend nginx |
| Perfil de Spring | `prod` |
| Configuración | `.env` y `docker-compose.override.yml`; contenido del override pendiente de revisión |
| Puertos publicados | MySQL: 3307; backend: 8080; frontend: 8081 |
| Acceso por Tailscale | `http://100.80.219.96:8081` |
| URL utilizada en las comprobaciones | `https://srv01.tailb2f071.ts.net/login.html` |
| Health | API y base de datos UP, comprobado por Gustavo |
| Datos de demostración | El responsable reporta que el perfil `prod` no carga el catálogo de demostración |

En la carpeta del proyecto del **servidor Debian**, el responsable puede obtener:

```bash
git rev-parse HEAD
git status --short
docker compose ps
docker compose images
docker compose exec backend printenv SPRING_PROFILES_ACTIVE TZ JAVA_TOOL_OPTIONS
```

Usar los mismos overrides que se emplearon al arrancar. Revisar antes de compartir
la última salida si JAVA_TOOL_OPTIONS contiene otros parámetros sensibles.
El commit de la carpeta no basta si se construyeron imágenes con otro código:
confirmar la reconstrucción y registrar modificaciones locales/identidad de imágenes.
No compartir `.env` ni `docker inspect` completo.

## 2. Acta de ensayo de principio a fin

Las comprobaciones preliminares reportadas por Gustavo se distinguen del ensayo
completo. Completar las demás filas solo tras observar el resultado.

| ID | Comprobación | Resultado | Folio/evidencia/incidencia |
|---|---|---|---|
| E01 | Acceso a la URL y health con base UP | Aprobado | Gustavo confirmó acceso y servicios UP |
| E02 | Registro crea cuenta pendiente e impide login | Aprobado | Cuenta nueva registrada; acceso bloqueado por aprobación pendiente |
| E03 | Personal entra al panel y consulta pendientes | Aprobado | Administrador localizó la cuenta nueva en Clientes pendientes |
| E04 | Aprobar permite login de cliente | Aprobado | Aprobación exitosa y acceso posterior del cliente al portal |
| E05 | Rechazar mantiene bloqueo y distingue estado | No ejecutado | |
| E06 | Catálogo, filtros y detalle coherentes | No ejecutado | |
| E07 | Reserva válida queda pendiente y persiste | No ejecutado | |
| E08 | Fechas inválidas y traslape se rechazan | No ejecutado | |
| E09 | Personal confirma y cliente ve el cambio | No ejecutado | |
| E10 | Entrega válida produce EN_CURSO y aparece en Mis rentas | No ejecutado | |
| E11 | Devolución produce FINALIZADA y total final | No ejecutado | |
| E12 | Reservaciones/rentas pertenecen al cliente autenticado | No ejecutado | |
| E13 | Reportes y auditoría corresponden a las operaciones | No ejecutado | |
| E14 | Cierre de sesión y acceso protegido funcionan | No ejecutado | |
| E15 | Se detienen/levantan servicios y se conservan datos | No ejecutado | |
| E16 | Manual y comandos coinciden con la versión desplegada | No ejecutado | |

## 3. Cierre

- [ ] Responsable confirma pasos reales de S5-06 y se ajusta la guía.
- [ ] Evidencia S5-07 corresponde al commit y configuración presentados.
- [ ] Ensayo completo registrado con participantes y duración.
- [ ] Incidencias corregidas y pasos afectados repetidos.
- [ ] Capturas conservadas de S4-12 contrastadas o sustituidas.
- [ ] Manual y configuración quedan vinculados a una versión identificable.

## Resultado de esta entrega

Se entrega la actualización del manual, la guía de despliegue y el guion
de demostración. Se comprobaron el acceso, el registro pendiente, el bloqueo
de inicio de sesión y la aprobación de clientes.

El despliegue real reportado utiliza Debian. La guía de Ubuntu se conserva
como referencia solicitada por la tarea; no se acredita su ejecución en Ubuntu.

Quedan pendientes el ensayo completo, la revisión del override de Compose
y la vinculación del reporte MySQL de S5-07 con la versión desplegada.
La documentación se entrega para revisión; no se acredita todavía el cierre
completo de S5-08.

No cerrar S5-08 únicamente porque existen los archivos. El ensayo y la verificación
contra el entorno real son criterios de aceptación independientes.
