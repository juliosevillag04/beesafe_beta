# BeeSafe Beta — Guía técnica y científica

## 1. Objetivo

Prototipo académico autocontenido para el proyecto APS de Física II. Un solo apiario ficticio, tres colmenas iniciales, dos fuentes electromagnéticas descriptivas, tres perfiles demo y **una simulación lineal compartida**. **No existen sensores conectados ni mediciones experimentales.**

## 2. Tecnología entregada (implementación real)

- **Java 17+**, servidor HTTP nativo `com.sun.net.httpserver.HttpServer` y motor de simulación con `ScheduledExecutorService`.
- **HTML5, CSS3, JavaScript ES2022** para la interfaz web local.
- **MathJax 3** empaquetado para renderizar las fórmulas LaTeX sin conexión a internet.
- **SVG** para los gráficos de tendencias y los íconos; informes mediante el sistema de impresión del navegador (opción Guardar como PDF).
- **JSON local** (`datos/estado.json`) para la persistencia de la beta. Operaciones de lectura y escritura gestionadas por Java y guardado atómico cuando el sistema de archivos lo permite.
- **SSE (Server-Sent Events)** con `GET /api/events` para actualizaciones compartidas.

**Nota de alcance:** las tecnologías React, TypeScript, Spring Boot y PostgreSQL se evaluaron en la planificación; no forman parte de la implementación empaquetada. Se escogió Java sin dependencias externas y almacenamiento local para que la beta sea ejecutable con un archivo BAT y sin instalaciones adicionales aparte de Java. Un despliegue multiusuario en internet requerirá implementar autenticación real, autorización en el servidor y otra infraestructura.

## 3. Organización de archivos

```text
BeeSafe_Beta/
├── Iniciar_BeeSafe.bat
├── Detener_BeeSafe.bat
├── Compilar_BeeSafe.bat
├── backend/
│   ├── src/BeeSafeServer.java
│   └── bin/BeeSafeServer*.class
├── web/
│   ├── index.html
│   ├── styles.css
│   ├── app.js
│   ├── assets/*.png
│   └── vendor/mathjax.js
├── datos/                   # se genera estado.json al ejecutar
├── scripts/iniciar.ps1
├── scripts/detener.ps1
├── logs/
└── docs/
```

## 4. Reglas de la simulación

- Una única sesión activa a la vez; solo el **Técnico** puede iniciar, pausar, reanudar, finalizar o reiniciar la demostración.
- Cadencia de **1 lectura por segundo por magnitud y colmena** (campo magnético RMS ficticio en µT y campo eléctrico RMS ficticio en V/m).
- El simulador incorpora un único evento transitorio programado cerca de COL-002 a partir de aproximadamente 48 segundos de ejecución y vuelve después a sus valores base.
- Los resultados oscilan de forma reproducible en un escenario sintético. **No son una predicción física real**: el campo de un transformador o una línea multipolar no se puede estimar con el modelo de un único conductor recto infinito.
- Las observaciones apícolas se registran **independientemente** de los campos. No existe una lógica de “campo sube ⇒ abeja se desorienta”.
- Cerrar la pestaña no detiene el backend. Detener el servidor sí finaliza el proceso de simulación; al reabrir, la sesión anterior permanece guardada como finalizada y su historial se conserva.

## 5. Perfiles

| Módulo | Apicultor | Técnico | Investigador |
|---|---|---|---|
| Ver apiario/colmenas | Sí | Sí | Sí |
| Registrar nuevas colmenas y fuentes | No | Sí | No |
| Controlar simulación global | No | Sí | No |
| Agregar mediciones manuales | No | Sí | No |
| Registrar observaciones de abejas | Sí | Sí | Sí |
| Registrar notas científicas | No | No | Sí |
| Registrar/actualizar seguimiento | Sí | Sí | Solo lectura |
| Biblioteca de Física II | Sí | Sí | Sí |
| Calculadoras físicas | Desde Centro de Física II | Sí | Sí |
| Informes | Sí | Sí | Sí |

El control de permisos es **ilustrativo**, no un sistema de identidad real. No uses esta beta para datos privados ni en una red pública.

## 6. Fórmulas de referencia

El Centro de Física II incluye: intensidad del campo eléctrico `E=F/|q|`, fuerza eléctrica, campo de carga puntual, superposición, campo aproximadamente uniforme entre placas; campo de un conductor rectilíneo largo `B=μ₀I/(2πr)`, fuerza magnética, Lorentz, flujo magnético y Faraday; frecuencia, período, frecuencia angular, voltaje/corriente sinusoidal, valores RMS; longitud de onda; reactancias, impedancia RLC; promedio, desviación estándar, cambio porcentual y tasa de actividad `R=N/Δt`.

**Advertencias físicas:**
- Los campos magnéticos RMS de 60 Hz son diferentes de la variación sinusoidal instantánea. No actualizar la pantalla 60 veces por segundo no contradice la frecuencia de la onda.
- La ley de Faraday se aplica a un flujo definido sobre una espira.
- `λ=c/f` describe longitud de onda en el vacío de una onda propagante, no la extensión espacial de campos cercanos de 60 Hz.
- `B=μ₀I/(2πr)` exige conductor recto idealizado, infinito y corriente estacionaria.
- La fórmula RMS `X_max/√2` requiere una sinusoide pura.
- La correlación estadística, menos aún con datos sintéticos, **no prueba causalidad**.

**Material académico:** Sears y Zemansky, *Física universitaria*, volumen 2, 12.ª edición; presentaciones de Física II (Campo eléctrico, Magnetismo–Campo magnético, Inducción electromagnética, Ley de Gauss y Corriente alterna).

## 7. API local

- `GET /api/health`: estado del servidor.
- `GET /api/state`: datos completos.
- `GET /api/live`: últimas lecturas, reloj y eventos.
- `GET /api/events`: eventos de actualización SSE.
- `POST /api/simulation`: `start`, `pause`, `resume`, `stop`, `reset` (rol Técnico).
- `POST /api/hives`: nueva colmena (Técnico).
- `POST /api/sources`: nueva fuente (Técnico).
- `POST /api/measurements`: medición manual (Técnico).
- `POST /api/observations`: observación apícola (todos).
- `POST /api/notes`: nota de investigación (Investigador).
- `POST /api/tasks`, `POST /api/tasks/status`: actividades de seguimiento.
- `GET /api/export.csv`: exportación CSV de mediciones.
- `POST /api/shutdown`: cierre local y guardado final usado por el BAT de detención (perfil técnico).

Los POST requieren cabecera `X-Demo-Role` con `apicultor`, `tecnico` o `investigador`, pero esta cabecera **no autentica** a nadie: puede elegirse libremente en la beta.

El servidor escucha únicamente en `127.0.0.1:8765` (no expone el proyecto a internet por defecto).

## 8. Iniciar y detener

- `Iniciar_BeeSafe.bat`: PowerShell comprueba Java, inicia el servicio y abre el navegador.
- `Detener_BeeSafe.bat`: detiene solo el PID verificado del proceso BeeSafe.
- `Compilar_BeeSafe.bat`: recompila el código Java si se modifica (requiere JDK 17+).

## 9. Licencia de terceros

MathJax se incluye con su licencia en `web/vendor/MathJax-LICENSE.txt`. Las ilustraciones y logos del proyecto se entregaron o generaron específicamente para BeeSafe.
