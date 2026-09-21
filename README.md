# Guía Farmacéutica — Venezuela

Conjunto de herramientas para **recolectar el catálogo de medicamentos de
Venezuela** publicado en [vademecum.es](https://www.vademecum.es/venezuela),
almacenarlo como dataset JSON y cargarlo en una **base de datos SQLite**
consultable (con búsqueda de texto completo FTS5 y clasificación clínica ATC).

## Contenido del repositorio

| Archivo | Descripción |
|---|---|
| `vademecum_ve.py` | **Módulo base de scraping**: recorre el índice alfabético (`/venezuela/ve/alfa/<letra>[/<subletra>]`), con triple capa HTTP, reintentos con backoff y caché opcional. Exporta `descargar_html()` y `recolectar()`. |
| `scraper_vademecum.py` | **Script alternativo de scraping** con `DELAY = 1.5 s` entre peticiones. |
| `medicamentos_vademecum_venezuela.json` | **Dataset versionado** (3,75 MB): 7057 medicamentos con `id` (código nacional de 8 dígitos), `nombre`, `slug`, `laboratorio`, `pais`, `url_ficha`, `url_equivalencias`, `url_fuente`. |
| `crear_bd_vademecum.py` | **Carga JSON → SQLite**: crea el esquema normalizado, dimensiones, índices, vistas e índice FTS5. Idempotente y con CLI. |
| `enriquecer_clinico.py` | **Enriquecimiento clínico**: descarga las fichas individuales y extrae vía, forma farmacéutica, código ATC, jerarquía ATC y la monografía del principio activo (15 secciones clínicas). Reanudable. |
| `requirements.txt` | Dependencias Python. |

## Requisitos

- Python **>= 3.10** (tipado `X | None`, `sqlite3` con FTS5; probado con 3.12 y SQLite 3.46.1)
- Dependencias:

```bash
pip install -r requirements.txt        # curl_cffi + beautifulsoup4
```

> **Nota**: la capa HTTP tiene *fallback* triple (`curl_cffi` → `curl` CLI →
> `urllib`), por lo que funciona sin `curl_cffi`, pero se recomienda
> instalarlo: evita bloqueos TLS del servidor.

## 1. Recolectar el catálogo (JSON)

```bash
python vademecum_ve.py                        # índice completo -> JSON por defecto
python vademecum_ve.py --delay 0.5 --salida medicamentos_vademecum_venezuela.json
```

- Respeta el servidor: `DELAY = 1.0 s` por defecto entre peticiones.
- Reintentos con backoff exponencial ante HTTP 429/5xx.
- La caché (`--cache-dir cache_vademecum`) permite reanudar una recolección
  interrumpida sin volver a golpear el servidor.

## 2. Crear la base de datos SQLite

```bash
python crear_bd_vademecum.py                  # crea/actualiza medicamentos_vademecum_venezuela.db
python crear_bd_vademecum.py --reemplazar     # borra y reconstruye desde cero
python crear_bd_vademecum.py --buscar ibuprofeno --limite 5
```

Esquema generado:

```
recoleccion      metadatos de la carga (fuente, fecha, total, errores)
paises           dimensión de países
laboratorios     dimensión de laboratorios titulares (136 en el dataset actual)
medicamentos     tabla principal (id TEXT = código nacional, 7057 filas)
medicamentos_fts índice FTS5 (nombre + laboratorio), insensible a acentos

v_medicamentos             vista plana con laboratorio y país resueltos
v_resumen_laboratorios     conteo por laboratorio
v_resumen_letras           conteo por letra (27 letras: a-z + '3')
```

Uso desde Python:

```python
from crear_bd_vademecum import crear_bd, abrir_bd, buscar

crear_bd()                                    # {'medicamentos': 7057, ...}
con = abrir_bd("medicamentos_vademecum_venezuela.db")
for fila in buscar(con, "solucion", 10):      # encuentra también "Solución"
    print(fila["id"], fila["nombre"], fila["laboratorio"])
```

## 3. Enriquecimiento clínico (ATC, vía, forma y monografías)

Descarga una vez cada ficha de medicamento y extrae:

- **Metadatos de la ficha**: vía de administración, forma farmacéutica
  normalizada, código ATC (5 o 7 caracteres), banderas de embarazo/lactancia.
- **Jerarquía ATC completa** derivada de la ficha (`M` → `M01` → `M01A` →
  `M01AE` → `M01AE01`), con descripción de cada nivel.
- **Monografía clínica del principio activo**, embebida en la propia ficha
  (no hace falta cuenta de registro: el aviso de "conéctate" es cosmético y el
  contenido viaja en el HTML). Secciones: mecanismo de acción, indicaciones
  terapéuticas, posología, modo de administración, contraindicaciones,
  advertencias y precauciones, insuficiencia hepática y renal, interacciones,
  embarazo, lactancia, efectos sobre la conducción, reacciones adversas y
  sobredosificación.

```bash
python enriquecer_clinico.py --limite 20      # prueba rápida (20 fichas)
python enriquecer_clinico.py                  # las 7057 fichas (~3 h a 1 s/ficha)
python enriquecer_clinico.py --solo-informe   # cobertura y tops sin descargar
python enriquecer_clinico.py --buscar hemorragia   # FTS dentro de las monografías
```

- **Reanudable**: cada lote de 25 fichas se vuelca a la BD y a
  `datos_clinicos.jsonl` (marcado en `medicamento_clinico.leido_en`); al
  relanzar solo se piden las pendientes. Ctrl-C guarda el lote en curso.
- Objetos nuevos en la BD: `vias`, `formas`, `atc`, `monografias`,
  `medicamento_clinico`, `monografias_fts` y las vistas `v_clinico`,
  `v_resumen_atc`, `v_resumen_vias`, `v_resumen_formas`.

Ejemplos de consulta:

```sql
-- Todos los AINE (nivel ATC 5) de un laboratorio
SELECT v.nombre, v.laboratorio, v.forma
FROM v_clinico v
WHERE v.laboratorio = 'Calox' AND v.atc_codigo LIKE 'M01A%';

-- Jerarquía: todo lo que pertenece a "Antiinflamatorios y antirreumáticos no esteroideos"
SELECT v.* FROM v_clinico v JOIN atc a ON a.codigo = v.atc_codigo
WHERE a.padre = 'M01A';

-- Buscar dentro del texto clínico de las monografías
SELECT atc_codigo, titulo FROM monografias_fts
WHERE monografias_fts MATCH 'hemorragia';

-- Cobertura por vía de administración
SELECT * FROM v_resumen_vias;
```

## Notas de diseño

- `medicamentos.id` es **TEXT** (código nacional con ceros a la izquierda
  potenciales); fiel 1:1 al JSON.
- `letra` y `prefijo` se derivan de `url_fuente` (`.../alfa/a/m` → `a`, `am`).
- El FTS5 usa el tokenizador `unicode61 remove_diacritics 2`: buscar
  `solucion` encuentra `Solución` y viceversa (4718 nombres con acentos).
- Los medicamentos que comparten prefijo antes del laboratorio (IBUPROFENO,
  ACETAMINOFEN, …) agrupan genéricos homólogos; el enriquecimiento añade la
  agrupación definitiva vía código ATC.
- Datos clínicos y de clasificación proceden de las fichas de
  [vademecum.es](https://www.vademecum.es) (© Vidal Vademecum Spain, S.A.);
  la información está dirigida a profesionales sanitarios.

## Estado

- [x] Scraper del índice alfabético → JSON (7057 medicamentos, 136 laboratorios)
- [x] Carga JSON → SQLite con FTS5
- [x] Enriquecimiento clínico (ATC, vía, forma, monografías)
- [ ] Interfaz de consulta (API/CLI amigable) — previsto
