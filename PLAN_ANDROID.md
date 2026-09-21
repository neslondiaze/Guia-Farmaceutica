# Plan de trabajo — App Android "Guia Farmaceutica" (APK)

> Documento maestro de planificación. Stack fijado: **Java · Android SDK
> (minSdk 24, Android 7.0+) · SQLite local · Material Design Components ·
> PdfDocument API + CSV · Glide · Gson**.

## 0. Objetivo y alcance

Construir la APK **Guia Farmaceutica**: guía offline de medicamentos de
Venezuela consumiendo los artefactos de este repositorio
(`medicamentos_vademecum_venezuela.db`, `medicamentos_vademecum_venezuela.json`,
`README.md`, `clasificacion_medicamentos.md`).

**Fuente de verdad de datos**: `medicamentos_vademecum_venezuela.db`
(7057 medicamentos · 1270 monografías · 48 vías · 107 formas · 2033 códigos ATC).

**Alcance v1.0 (MVP)**: catálogo offline navegable + buscador + ficha clínica
+ filtros por clasificación + exportar a PDF/CSV. **Fuera de alcance v1.0**:
cuentas de usuario, backend, sincronización remota, recordatorios de tomas.

**Criterio global de aceptación**: APK `release` firmada que instala en un
dispositivo Android 7.0, abre sin red y muestra los 7057 medicamentos.

## 1. Mapa de datos de entrada → modelo Android

| Origen (repo) | Contenido | Uso en la app |
|---|---|---|
| `medicamentos_vademecum_venezuela.db` | Tablas `medicamentos`, `laboratorios`, `paises`, `medicamento_clinico`, `vias`, `formas`, `atc`, `monografias`, `medicamentos_fts`, `monografias_fts`; vistas `v_medicamentos`, `v_clinico`, `v_resumen_*` | **BD semilla** copiada a almacenamiento interno en el primer arranque |
| `medicamentos_vademecum_venezuela.json` | Dataset crudo (7057 registros) | Respaldo/alternativa de siembra si la BD cambia de esquema |
| `clasificacion_medicamentos.md` | Los 9 ejes de clasificación + SQL de ejemplo | Especificación funcional de filtros y ficha (ver §5) |
| `README.md` | Esquema, vistas y consultas ya validadas | Referencia de queries a portar a DAO |

Ejes de clasificación a exponer en la app (ver `clasificacion_medicamentos.md`):
**ATC 5 niveles** (14 clases anatómicas, ~108 grupos terapéuticos, 1270
principios activos) · **vía** (48) · **forma** (107) · **laboratorio** (136) ·
**letra/prefijo** · **embarazo/lactancia** · **monografía** (15 secciones).

> ⚠️ Nota técnica: la BD usa tablas virtuales **FTS5**. Room soporta FTS4 de
> forma nativa (`@Fts4`); para FTS5 se usará `SupportSQLiteQuery`/`@RawQuery`
> o `SQLiteOpenHelper` clásico (ver Fase 2). Android 7.0+ incluye SQLite con
> FTS5, no hay problema de compatibilidad.

## 2. Arquitectura y estructura del proyecto Android

- Patrón **MVVM**: `ui/` (Activities/Fragments) → `viewmodel/` →
  `repository/` → `data/` (Room/DAO/entidades).
- Paquete base sugerido: `ve.guiafarmaceutica.app`.
- Estructura:

```
app/src/main/
  assets/databases/medicamentos_vademecum_venezuela.db  # BD semilla
  java/ve/guiafarmaceutica/app/
    data/        # AppDatabase, entidades Room, DAOs, SeedManager
    repository/  # MedicamentoRepository, MonografiaRepository
    viewmodel/   # BusquedaViewModel, FichaViewModel, FiltrosViewModel, ExportViewModel
    ui/          # MainActivity, fragments (Busqueda, Ficha, Clasificacion, Favoritos, Exportar)
    util/        # ExportHelper (PDF/CSV), FileUtils, Constants
  res/           # Material 3: themes.xml, strings (es), layouts, drawables
```

- Dependencias Gradle (`app/build.gradle`): `material`, `room-runtime` +
  `room-compiler` (annotationProcessor), `lifecycle-viewmodel/livedata`,
  `recyclerview`, `glide` + `glide-compiler`, `gson`.
- `minSdk 24`, `targetSdk` vigente, `compileSdk` vigente, Java 17.

## 3. Fases de trabajo

### Fase 0 — Preparación (0,5 día)

- [ ] Crear proyecto Android Studio (Empty Views Activity, Java, minSdk 24).
- [ ] Añadir dependencias Gradle (Material, Room, Lifecycle, RecyclerView, Glide, Gson).
- [ ] Copiar la BD a `app/src/main/assets/databases/` (verificar ~24 MB).
- [ ] Tema Material 3 (`Theme.Material3.DayNight`), paleta, `strings.xml` en español.
- **Aceptación**: compila `debug`, sin warnings bloqueantes.

### Fase 1 — Capa de datos (1–2 días)

- [ ] `SeedManager`: copia la BD de `assets/` al primer arranque (solo si no
      existe); verifica integridad (`PRAGMA quick_check` + conteo 7057).
- [ ] Entidades Room espejo de: `medicamentos`, `laboratorios`, `vias`,
      `formas`, `atc`, `monografias`, `medicamento_clinico`.
- [ ] DAOs con las consultas ya validadas en `clasificacion_medicamentos.md`:
  - `v_clinico` plana (id, nombre, laboratorio, país, ATC, vía, forma, embarazo, lactancia)
  - resumen por clase ATC nivel 1, grupo terapéutico, laboratorio, vía, forma
  - búsqueda FTS5 en `medicamentos_fts` y `monografias_fts` vía `@RawQuery`
- [ ] `MedicamentoRepository` + `MonografiaRepository` (fuente única de verdad).
- [ ] Favoritos locales: tabla propia `favoritos(medicamento_id, foto_path, nota, creado_en)` — la BD semilla es de solo lectura.
- **Aceptación**: test instrumentado que abre la BD copiada y devuelve 7057 filas; FTS `MATCH 'hemorragia'` responde < 300 ms en gama media.

### Fase 2 — Buscador y catálogo (2–3 días)

- [ ] `SearchView` + `RecyclerView` con `ListAdapter`/`DiffUtil`, paginación
      (`LIMIT/OFFSET`) y debounce 300 ms.
- [ ] Orden: por relevancia FTS (`rank`), luego alfabético.
- [ ] Pantalla de resultados con chips de conteo (n resultados).
- [ ] Estado vacío / error / sin red innecesaria (todo es local).
- **Aceptación**: "ibu" → Ibuprofeno primero; "solucion" matchea "Solución"
  (validar `remove_diacritics` del FTS5 semilla).

### Fase 3 — Ficha del medicamento (2 días)

- [ ] Cabecera: nombre, laboratorio, país, forma, vía, código ATC + breadcrumb
      de jerarquía (`M → M01 → M01A → M01AE → M01AE01`, navegable).
- [ ] Secciones plegables (15 de la monografía: mecanismo, indicaciones,
      posología, contraindicaciones, interacciones, embarazo, lactancia…).
- [ ] Banderas de seguridad (embarazo/lactancia) con color semántico.
- [ ] "Genéricos homólogos": mismo `atc_codigo`, otros laboratorios.
- [ ] Foto (Glide): placeholder vectorial + foto opcional del usuario por
      favorito (cámara/galería, guardada en almacenamiento interno).
- **Aceptación**: ficha de `N02BE01` (Paracetamol) muestra sus 95 presentaciones.

### Fase 4 — Clasificación / Explorar por grupos (2 días)

- [ ] Pantalla "Explorar": 14 clases anatómicas ATC → grupos terapéuticos →
      principios activos → medicamentos (navegación por `padre` de `atc`).
- [ ] Filtros combinables: laboratorio (136), vía (48), forma (107),
      letra/prefijo.
- [ ] Contadores por grupo (queries `v_resumen_*`).
- **Aceptación**: J (Antiinfecciosos) lista 1040; `C09%` lista sus subgrupos.

### Fase 5 — Reportes PDF y CSV (2 días)

- [ ] `ExportHelper`: **PDF** con `android.graphics.pdf.PdfDocument`
      (ficha individual o lista filtrada: cabecera, tabla, pie con fecha).
- [ ] **CSV** con flujos de archivos (`ContentResolver` + Storage Access
      Framework, `ACTION_CREATE_DOCUMENT`): `;` como separador, UTF-8 con BOM
      para Excel, escape de comillas.
- [ ] Compartir vía `FileProvider` + `ACTION_SEND`.
- [ ] Sin permisos peligrosos: solo SAF; compatible con Scoped Storage (API 29+).
- **Aceptación**: exportar los 396 de `M01` a CSV y abrirlo en Excel;
  ficha en PDF de 1 página legible.

### Fase 6 — Pulido, QA y release (2–3 días)

- [ ] Modo oscuro/claro, accesibilidad (TalkBack, tamaño de texto), rotación.
- [ ] Rendimiento: listas de 7057 ítems con `DiffUtil`; profiler de memoria
      (Glide + cursor window).
- [ ] Pruebas en emulador API 24 y dispositivo físico; prueba sin red
      (modo avión).
- [ ] `minifyEnabled`/`shrinkResources` en `release`, firma con keystore,
      `versionCode`/`versionName`, generación de la **APK**.
- [ ] Actualizar este plan con desviaciones reales.
- **Aceptación**: APK firmada, instalable en Android 7.0, abre offline.

## 4. Estimación total

| Fase | Días |
|---|---|
| 0 Preparación | 0,5 |
| 1 Capa de datos | 1–2 |
| 2 Buscador | 2–3 |
| 3 Ficha | 2 |
| 4 Clasificación | 2 |
| 5 PDF/CSV | 2 |
| 6 QA + release | 2–3 |
| **Total** | **≈ 12–15 días laborables** |

## 5. Riesgos y decisiones

1. **FTS5 + Room**: Room solo abstrae FTS4; usar `@RawQuery`/`SupportSQLiteQuery`
   contra `medicamentos_fts`/`monografias_fts` existentes. Alternativa: reindexar
   en FTS4 al sembrar (más trabajo, no recomendado).
2. **Tamaño de la APK**: BD ~24 MB en `assets/` (comprime a ~8–10 MB en el APK).
   Si supera límites de distribución, mover la BD a descarga inicial con Gson+JSON.
3. **Texto con HTML/entidades**: las monografías traen restos (`&nbsp;`,
   `<exp>`); sanear al mostrar (`Html.fromHtml` + limpieza) una sola vez en el
   repositorio, no en la UI.
4. **Variantes con coma en vías** ("Vía intramuscular," vs sin coma):
   normalizar en la capa DAO con `TRIM(nombre, ' ,')` o vista de limpieza.
5. **Fotos con Glide**: la BD no trae imágenes; Glide cubre placeholders y las
   fotos que el usuario añada a favoritos.
6. **Los datos clínicos son informativos** (© Vidal Vademecum Spain): mostrar
   aviso de "uso profesional sanitario" en la ficha, coherente con la fuente.
