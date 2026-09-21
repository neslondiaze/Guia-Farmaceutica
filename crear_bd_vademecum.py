#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Crea una base de datos SQLite a partir de medicamentos_vademecum_venezuela.json.

Esquema (normalizado y en espanol):

    recoleccion    metadatos de la ejecucion de scraping (fuente, fecha, total)
    paises         dimension de paises (FK desde medicamentos)
    laboratorios   dimension de laboratorios titulares (FK desde medicamentos)
    medicamentos   tabla principal, una fila por medicamento (id = codigo nacional)
    medicamentos_fts  indice FTS5 para busqueda de texto completo (nombre + laboratorio)

    vistas:        v_medicamentos (plana, con laboratorio y pais resueltos)
                   v_resumen_laboratorios, v_resumen_letras

Notas de diseno:
  * `medicamentos.id` es el codigo nacional (8 digitos) que aparece en la ficha;
    se guarda como TEXT para no perder ceros a la izquierda y ser fiel al JSON.
  * `letra` y `prefijo` se derivan de `url_fuente` (p. ej. .../alfa/a/m -> 'a', 'am');
    si no hay URL se usa la primera letra del nombre.
  * `medicamentos_fts` usa el tokenizador unicode61 con remove_diacritics=2, de modo
    que "solucion" encuentra "Solución" (hay 4718 nombres con acentos).

Uso:
    python crear_bd_vademecum.py                        # crea/actualiza la BD
    python crear_bd_vademecum.py --reemplazar           # borra y reconstruye
    python crear_bd_vademecum.py --buscar ibuprofeno    # busqueda FTS de prueba
"""

from __future__ import annotations

import argparse
import json
import os
import sqlite3
import sys
import time
from typing import Callable

# --------------------------------------------------------------------------- #
# Configuracion
# --------------------------------------------------------------------------- #

JSON_POR_DEFECTO = "medicamentos_vademecum_venezuela.json"
BD_POR_DEFECTO = "medicamentos_vademecum_venezuela.db"

ESQUEMA = """
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS recoleccion (
    id       INTEGER PRIMARY KEY,
    fuente   TEXT    NOT NULL,
    fecha    TEXT    NOT NULL,
    total    INTEGER NOT NULL,
    errores  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS paises (
    id     INTEGER PRIMARY KEY,
    nombre TEXT    NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS laboratorios (
    id     INTEGER PRIMARY KEY,
    nombre TEXT    NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS medicamentos (
    id                TEXT    PRIMARY KEY,
    nombre            TEXT    NOT NULL,
    slug              TEXT    NOT NULL,
    laboratorio_id    INTEGER REFERENCES laboratorios(id),
    pais_id           INTEGER REFERENCES paises(id),
    letra             TEXT,
    prefijo           TEXT,
    url_ficha         TEXT    NOT NULL,
    url_equivalencias TEXT,
    url_fuente        TEXT
);

CREATE INDEX IF NOT EXISTS idx_medicamentos_nombre      ON medicamentos(nombre);
CREATE INDEX IF NOT EXISTS idx_medicamentos_slug        ON medicamentos(slug);
CREATE INDEX IF NOT EXISTS idx_medicamentos_letra       ON medicamentos(letra);
CREATE INDEX IF NOT EXISTS idx_medicamentos_prefijo     ON medicamentos(prefijo);
CREATE INDEX IF NOT EXISTS idx_medicamentos_laboratorio ON medicamentos(laboratorio_id);

CREATE VIRTUAL TABLE IF NOT EXISTS medicamentos_fts USING fts5(
    id UNINDEXED,
    nombre,
    laboratorio,
    tokenize = 'unicode61 remove_diacritics 2'
);

CREATE VIEW IF NOT EXISTS v_medicamentos AS
SELECT m.id            AS id,
       m.nombre        AS nombre,
       l.nombre        AS laboratorio,
       p.nombre        AS pais,
       m.letra         AS letra,
       m.prefijo       AS prefijo,
       m.slug          AS slug,
       m.url_ficha     AS url_ficha,
       m.url_equivalencias AS url_equivalencias,
       m.url_fuente    AS url_fuente
FROM medicamentos m
LEFT JOIN laboratorios l ON l.id = m.laboratorio_id
LEFT JOIN paises       p ON p.id = m.pais_id;

CREATE VIEW IF NOT EXISTS v_resumen_laboratorios AS
SELECT l.id AS laboratorio_id, l.nombre AS laboratorio, COUNT(*) AS medicamentos
FROM medicamentos m
JOIN laboratorios l ON l.id = m.laboratorio_id
GROUP BY l.id, l.nombre
ORDER BY medicamentos DESC, l.nombre;

CREATE VIEW IF NOT EXISTS v_resumen_letras AS
SELECT letra, COUNT(*) AS medicamentos
FROM medicamentos
GROUP BY letra
ORDER BY letra;
"""

TABLAS = ("medicamentos_fts", "medicamentos", "laboratorios", "paises", "recoleccion")
VISTAS = ("v_medicamentos", "v_resumen_laboratorios", "v_resumen_letras")

# --------------------------------------------------------------------------- #
# Utilidades
# --------------------------------------------------------------------------- #

def _derivar_letra_prefijo(url_fuente: str | None, nombre: str | None) -> tuple[str | None, str | None]:
    """Deriva (letra, prefijo) a partir de la URL de origen del listado.

    Ejemplos:  .../alfa/a      -> ('a', None)
               .../alfa/a/m    -> ('a', 'am')
               .../alfa/3/-    -> ('3', '3-')
    Si la URL no aporta la letra se usa la primera letra del nombre.
    """
    letra = prefijo = None
    if url_fuente and "/alfa/" in url_fuente:
        segmentos = [s for s in url_fuente.split("/alfa/")[-1].strip("/").split("/") if s]
        if segmentos:
            letra = segmentos[0].lower()
        if len(segmentos) > 1:
            prefijo = (segmentos[0] + segmentos[1]).lower()
    if not letra and nombre:
        letra = nombre[0].lower()
    return letra, prefijo


def abrir_bd(ruta: str) -> sqlite3.Connection:
    """Abre (y crea si hace falta) la base de datos con las opciones adecuadas."""
    conexion = sqlite3.connect(ruta)
    conexion.row_factory = sqlite3.Row
    conexion.execute("PRAGMA foreign_keys = ON")
    conexion.execute("PRAGMA journal_mode = WAL")
    return conexion


def crear_esquema(conexion: sqlite3.Connection, reemplazar: bool = False,
                  log: Callable[[str], None] | None = None) -> None:
    """Crea tablas, indices, vistas y la tabla FTS5.

    Con `reemplazar=True` elimina primero los objetos existentes para dejar la
    base limpia (util cuando el JSON de origen cambia por completo).
    """
    log = log or (lambda _mensaje: None)

    if reemplazar:
        log("   eliminando objetos existentes (--reemplazar)")
        for vista in VISTAS:
            conexion.execute(f"DROP VIEW IF EXISTS {vista}")
        for tabla in TABLAS:
            conexion.execute(f"DROP TABLE IF EXISTS {tabla}")
        conexion.commit()

    conexion.executescript(ESQUEMA)
    conexion.commit()


def _id_dimension(conexion: sqlite3.Connection, tabla: str, nombre: str,
                  cache: dict[str, int]) -> int:
    """Devuelve el id de una fila de tabla de dimension, creandola si no existe."""
    clave = f"{tabla}:{nombre}"
    if clave in cache:
        return cache[clave]
    conexion.execute(f"INSERT OR IGNORE INTO {tabla}(nombre) VALUES (?)", (nombre,))
    fila = conexion.execute(f"SELECT id FROM {tabla} WHERE nombre = ?", (nombre,)).fetchone()
    cache[clave] = fila["id"]
    return fila["id"]


# --------------------------------------------------------------------------- #
# Carga del JSON
# --------------------------------------------------------------------------- #

def cargar_documento(conexion: sqlite3.Connection, documento: dict,
                     log: Callable[[str], None] | None = None) -> int:
    """Inserta metadatos, dimensiones y medicamentos. Devuelve filas procesadas."""
    log = log or (lambda _mensaje: None)
    medicamentos = documento.get("medicamentos") or []

    conexion.execute("DELETE FROM recoleccion")
    conexion.execute(
        "INSERT INTO recoleccion(id, fuente, fecha, total, errores) VALUES (1, ?, ?, ?, ?)",
        (documento.get("fuente") or "", documento.get("fecha") or time.strftime("%Y-%m-%dT%H:%M:%S"),
         len(medicamentos), len(documento.get("errores") or [])),
    )

    cache: dict[str, int] = {}
    filas = []
    omitidos = 0
    for medicamento in medicamentos:
        identificador = medicamento.get("id")
        nombre = (medicamento.get("nombre") or "").strip()
        if identificador in (None, "") or not nombre:
            omitidos += 1
            continue
        laboratorio = (medicamento.get("laboratorio") or "").strip()
        pais = (medicamento.get("pais") or "").strip()
        letra, prefijo = _derivar_letra_prefijo(medicamento.get("url_fuente"), nombre)
        filas.append((
            str(identificador),
            nombre,
            (medicamento.get("slug") or "").strip(),
            _id_dimension(conexion, "laboratorios", laboratorio, cache) if laboratorio else None,
            _id_dimension(conexion, "paises", pais, cache) if pais else None,
            letra,
            prefijo,
            medicamento.get("url_ficha") or "",
            medicamento.get("url_equivalencias"),
            medicamento.get("url_fuente"),
        ))

    conexion.executemany(
        """INSERT OR REPLACE INTO medicamentos
           (id, nombre, slug, laboratorio_id, pais_id, letra, prefijo,
            url_ficha, url_equivalencias, url_fuente)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        filas,
    )
    conexion.commit()
    log(f"   medicamentos insertados/actualizados: {len(filas)}")
    if omitidos:
        log(f"   ADVERTENCIA: {omitidos} registros omitidos por no tener id o nombre")
    log(f"   laboratorios: {len({f[3] for f in filas if f[3]})} | "
        f"paises: {len({f[4] for f in filas if f[4]})}")
    return len(filas)


def construir_fts(conexion: sqlite3.Connection,
                  log: Callable[[str], None] | None = None) -> int:
    """Reconstruye el indice FTS5 (nombre + laboratorio) tras la carga."""
    log = log or (lambda _mensaje: None)
    conexion.execute("DELETE FROM medicamentos_fts")
    conexion.execute(
        """INSERT INTO medicamentos_fts(id, nombre, laboratorio)
           SELECT m.id, m.nombre, COALESCE(l.nombre, '')
           FROM medicamentos m
           LEFT JOIN laboratorios l ON l.id = m.laboratorio_id"""
    )
    conexion.commit()
    total = conexion.execute("SELECT COUNT(*) AS n FROM medicamentos_fts").fetchone()["n"]
    log(f"   indice FTS5 construido: {total} filas")
    return total


# --------------------------------------------------------------------------- #
# Consultas de utilidad
# --------------------------------------------------------------------------- #

def _consulta_fts(termino: str) -> str:
    """Convierte texto libre en una consulta FTS5 segura (tokens entrecomillados).

    El ultimo token se marca como prefijo ("*") para permitir busquedas
    parciales: 'ibup' encuentra 'Ibuprofeno'. Las comillas evitan que signos
    como '-' o '"' rompan la sintaxis de FTS5. Devuelve "" si no hay tokens.
    """
    tokens = [t for t in termino.replace('"', " ").split() if t]
    if not tokens:
        return ""
    partes = ['"' + t + '"' for t in tokens[:-1]]
    partes.append('"' + tokens[-1] + '"*')
    return " ".join(partes)


def buscar(conexion: sqlite3.Connection, termino: str, limite: int = 10) -> list[sqlite3.Row]:
    """Busca medicamentos por nombre/laboratorio usando el indice FTS5.

    Nota: SQLite exige el nombre completo de la tabla virtual en el operador
    MATCH (no admite alias), por eso no se usa `AS f` aqui.
    """
    consulta = _consulta_fts(termino)
    if not consulta:
        return []
    return conexion.execute(
        """SELECT v.*
           FROM medicamentos_fts
           JOIN v_medicamentos AS v ON v.id = medicamentos_fts.id
           WHERE medicamentos_fts MATCH ?
           ORDER BY rank
           LIMIT ?""",
        (consulta, limite),
    ).fetchall()


def resumen(conexion: sqlite3.Connection) -> dict:
    """Devuelve contadores y estadisticas basicas de la base."""
    contar = lambda sql: conexion.execute(sql).fetchone()[0]  # noqa: E731
    recoleccion = conexion.execute("SELECT * FROM recoleccion").fetchone()
    return {
        "medicamentos": contar("SELECT COUNT(*) FROM medicamentos"),
        "medicamentos_fts": contar("SELECT COUNT(*) FROM medicamentos_fts"),
        "laboratorios": contar("SELECT COUNT(*) FROM laboratorios"),
        "paises": contar("SELECT COUNT(*) FROM paises"),
        "letras": contar("SELECT COUNT(DISTINCT letra) FROM medicamentos"),
        "prefijos": contar("SELECT COUNT(DISTINCT prefijo) FROM medicamentos"),
        "sin_laboratorio": contar("SELECT COUNT(*) FROM medicamentos WHERE laboratorio_id IS NULL"),
        "huerfanos": contar(
            """SELECT COUNT(*) FROM medicamentos m
               WHERE laboratorio_id IS NOT NULL
                 AND NOT EXISTS (SELECT 1 FROM laboratorios l WHERE l.id = m.laboratorio_id)"""
        ),
        "fuente": recoleccion["fuente"] if recoleccion else None,
        "fecha": recoleccion["fecha"] if recoleccion else None,
    }


def mantenimiento(conexion: sqlite3.Connection,
                  log: Callable[[str], None] | None = None) -> None:
    """Optimiza el indice FTS5 y actualiza las estadisticas del planificador."""
    log = log or (lambda _mensaje: None)
    conexion.execute("INSERT INTO medicamentos_fts(medicamentos_fts) VALUES('optimize')")
    conexion.execute("ANALYZE")
    conexion.commit()
    conexion.execute("VACUUM")
    log("   mantenimiento aplicado (optimize FTS5 + ANALYZE + VACUUM)")


def crear_bd(ruta_json: str = JSON_POR_DEFECTO, ruta_bd: str = BD_POR_DEFECTO,
             reemplazar: bool = False, con_fts: bool = True,
             log: Callable[[str], None] | None = None) -> dict:
    """Crea la base SQLite a partir del JSON de medicamentos y devuelve el resumen.

    Es la funcion principal del modulo y puede usarse desde otro script:

        from crear_bd_vademecum import crear_bd
        resumen = crear_bd()          # medicamentos_vademecum_venezuela.db
    """
    log = log or (lambda mensaje: print(mensaje, flush=True))

    if not os.path.exists(ruta_json):
        raise FileNotFoundError(f"No existe el JSON de entrada: {ruta_json}")

    log(f"[1/4] Leyendo JSON: {ruta_json}")
    with open(ruta_json, encoding="utf-8") as archivo:
        documento = json.load(archivo)

    log(f"[2/4] Creando esquema en: {ruta_bd}")
    with abrir_bd(ruta_bd) as conexion:
        crear_esquema(conexion, reemplazar=reemplazar, log=log)

        log("[3/4] Cargando datos")
        cargar_documento(conexion, documento, log=log)

        log("[4/4] Construyendo indices de busqueda")
        if con_fts:
            construir_fts(conexion, log=log)
        mantenimiento(conexion, log=log)
        return resumen(conexion)



# --------------------------------------------------------------------------- #
# Interfaz de linea de comandos
# --------------------------------------------------------------------------- #

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Crea una base de datos SQLite desde medicamentos_vademecum_venezuela.json"
    )
    parser.add_argument("--json", default=JSON_POR_DEFECTO,
                        help=f"JSON de entrada (por defecto: {JSON_POR_DEFECTO})")
    parser.add_argument("--db", default=BD_POR_DEFECTO,
                        help=f"base de datos SQLite de salida (por defecto: {BD_POR_DEFECTO})")
    parser.add_argument("--reemplazar", action="store_true",
                        help="borrar tablas y vistas existentes antes de crear")
    parser.add_argument("--sin-fts", action="store_true",
                        help="no construir el indice de texto completo FTS5")
    parser.add_argument("--buscar", default=None,
                        help="termino de prueba; ejecuta una busqueda FTS al terminar")
    parser.add_argument("--limite", type=int, default=10,
                        help="maximo de resultados para --buscar (por defecto: 10)")
    opciones = parser.parse_args(argv)

    try:
        estadisticas = crear_bd(
            ruta_json=opciones.json,
            ruta_bd=opciones.db,
            reemplazar=opciones.reemplazar,
            con_fts=not opciones.sin_fts,
        )
    except FileNotFoundError as error:
        print(f"Error: {error}")
        return 1
    except json.JSONDecodeError as error:
        print(f"Error: el JSON de entrada no es valido ({error})")
        return 1

    print("\n" + "=" * 62)
    print(f"Base de datos creada: {os.path.abspath(opciones.db)}")
    print(f"   medicamentos      : {estadisticas['medicamentos']}")
    print(f"   indice FTS5       : {estadisticas['medicamentos_fts']}")
    print(f"   laboratorios      : {estadisticas['laboratorios']}")
    print(f"   paises            : {estadisticas['paises']}")
    print(f"   letras / prefijos : {estadisticas['letras']} / {estadisticas['prefijos']}")
    print(f"   sin laboratorio   : {estadisticas['sin_laboratorio']}")
    print(f"   FKs huerfanas     : {estadisticas['huerfanos']}")
    print(f"   fuente / fecha    : {estadisticas['fuente']} | {estadisticas['fecha']}")

    if opciones.buscar:
        print(f"\nBusqueda FTS de '{opciones.buscar}' (limite {opciones.limite}):")
        with abrir_bd(opciones.db) as conexion:
            for fila in buscar(conexion, opciones.buscar, opciones.limite):
                print(f"   [{fila['id']}] {fila['nombre']}  --  {fila['laboratorio']}")

    return 0


if __name__ == "__main__":
    sys.exit(main())

