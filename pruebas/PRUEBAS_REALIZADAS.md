# Pruebas de BeeSafe Beta

Pruebas realizadas durante la preparación del paquete, en entorno Linux con Java 21. Las clases se compilaron para compatibilidad con Java 17+.

## Pruebas ejecutadas y aprobadas

1. `GET /api/health`: servidor responde correctamente.
2. `POST /api/simulation` como Técnico: iniciar, pausar, reanudar y finalizar.
3. Simulación activa: al menos dos lecturas por magnitud y colmena después de varios segundos; reloj avance y pausa correctamente.
4. Permisos: Apicultor no puede registrar colmenas (HTTP 403).
5. Técnico puede registrar una nueva colmena, fuente electromagnética y lectura manual.
6. Apicultor puede guardar una observación apícola.
7. Investigador puede guardar una nota de análisis.
8. Apicultor puede registrar una actividad de seguimiento y actualizar su estado.
9. Exportación de CSV con registros etiquetados por procedencia.
10. Cierre y reinicio del servidor: se conservan nuevas colmenas, lecturas, observaciones y notas.
11. Restablecimiento de demostración: vuelve a tres colmenas, dos fuentes y simulación detenida.
12. Prueba automatizada de navegación de todas las pantallas visibles para los tres roles, además de calculadoras y búsqueda en Centro de Física II, sin errores JavaScript detectados.

## Limitaciones de validación

- El flujo Windows `.bat`/PowerShell fue revisado, pero **no pudo ejecutarse en Windows dentro de este entorno**. Requiere Java instalado y visible en `PATH`.
- No se usaron sensores físicos ni observaciones de campo reales.
- El motor de cálculo no estima riesgos biológicos.
- La interfaz usa SVG y MathJax empaquetado localmente, no servicios remotos.
