#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Enriquece la BD de medicamentos con la clasificacion clinica de vademecum.es.

Anade a `medicamentos_vademecum_venezuela.db` la informacion que la ficha de
cada medicamento trae y que `crear_bd_vademecum.py` no recoge:

    vias            dimension de vias de administracion ("Via oral", ...)
    formas          dimension de formas farmaceuticas ("Comprimido", ...)
    atc             clasificacion ATC jerarquica (1, 3, 4, 5 y 7 caracteres)
    monografias     monografia del principio activo (una fila por codigo ATC)
    medicamento_clinico  enlace medicamento -> ATC / via / forma / banderas
    monografias_fts indice FTS5 del texto clinico

    vistas:         v_clinico (plana), v_resumen_atc, v_resumen_vias,
                    v_resumen_formas

Hallazgo verificado (2026-09-21) sobre 15 fichas reales:
  * Nombre local, Pais, Laboratorio, Via, Forma y ATC: 15/15 (100 %).
  * Monografia clinica embebida en la propia ficha, con las secciones
    Mecanismo de accion, Indicaciones terapeuticas, Posologia, Modo de
    administracion, Contraindicaciones, Advertencias y precauciones,
    Insuficiencia hepatica/renal, Interacciones, Embarazo, Lactancia, Efectos
    sobre la capacidad de conducir, Reacciones adversas y Sobredosificacion.
  * El aviso "debes conectarte con tu email y clave o registrarte" es COSMETICO:
    el texto real de posologia viaja en el HTML anonimo dentro de
    <div id="texto_poso" style="display:none">, y el de la monografia en
    <div id="fichaATC">. No hace falta cuenta para recolectarlo (verificado con
    peticiones sin cookies).

Reanudable: cada ficha procesada se guarda en un JSONL (por defecto
datos_clinicos.jsonl) y se marca en `medicamento_clinico.leido_en`; al volver a
ejecutar solo se piden las que faltan. No se cachea el HTML en disco porque a
~300 KB por ficha serian ~2 GB para las 7057 fichas.

Uso:
    python enriquecer_clinico.py --limite 20            # prueba rapida
    python enriquecer_clinico.py                        # las 7057 fichas (~2 h)
    python enriquecer_clinico.py --desde 77003866       # reanudar desde un id
    python enriquecer_clinico.py --reprocesar           # volver a pedir todo
    python enriquecer_clinico.py --solo-informe         # no descarga, solo resume
    python enriquecer_clinico.py --buscar ibuprofeno    # FTS de monografias
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sqlite3
import sys
import time
from typing import Callable

from bs4 import BeautifulSoup

from crear_bd_vademecum import BD_POR_DEFECTO, abrir_bd
from vademecum_ve import descargar_html

# --------------------------------------------------------------------------- #
# Configuracion
# --------------------------------------------------------------------------- #

JSONL_POR_DEFECTO = "datos_clinicos.jsonl"
DELAY = 1.0  # segundos entre fichas (respetuoso con el servidor)

# --------------------------------------------------------------------------- #
# Esquema clinico (se anade a la base existente, sin borrar nada)
# --------------------------------------------------------------------------- #

ESQUEMA_CLINICO = """
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS vias (
    id     INTEGER PRIMARY KEY,
    nombre TEXT    NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS formas (
    id     INTEGER PRIMARY KEY,
    nombre TEXT    NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS atc (
    codigo      TEXT PRIMARY KEY,
    descripcion TEXT NOT NULL,
    nivel       INTEGER NOT NULL,
    padre       TEXT REFERENCES atc(codigo)
);

CREATE TABLE IF NOT EXISTS monografias (
    atc_codigo                 TEXT PRIMARY KEY REFERENCES atc(codigo),
    titulo                     TEXT,
    url_monografia             TEXT,
    embarazo                   TEXT,
    lactancia                  TEXT,
    notas                      TEXT,
    mecanismo_accion           TEXT,
    indicaciones_terapeuticas  TEXT,
    indicaciones_posologia     TEXT,
    posologia                  TEXT,
    modo_administracion        TEXT,
    contraindicaciones         TEXT,
    advertencias_precauciones  TEXT,
    insuficiencia_hepatica     TEXT,
    insuficiencia_renal        TEXT,
    interacciones              TEXT,
    embarazo_texto             TEXT,
    lactancia_texto            TEXT,
    conduccion                 TEXT,
    reacciones_adversas        TEXT,
    sobredosificacion          TEXT,
    actualizado_en             TEXT
);

CREATE TABLE IF NOT EXISTS medicamento_clinico (
    medicamento_id TEXT PRIMARY KEY REFERENCES medicamentos(id),
    atc_codigo     TEXT REFERENCES atc(codigo),
    via_id         INTEGER REFERENCES vias(id),
    forma_id       INTEGER REFERENCES formas(id),
    embarazo       TEXT,
    lactancia      TEXT,
    url_monografia TEXT,
    leido_en       TEXT,
    error          TEXT
);

CREATE INDEX IF NOT EXISTS idx_atc_padre          ON atc(padre);
CREATE INDEX IF NOT EXISTS idx_clinico_atc        ON medicamento_clinico(atc_codigo);
CREATE INDEX IF NOT EXISTS idx_clinico_via        ON medicamento_clinico(via_id);
CREATE INDEX IF NOT EXISTS idx_clinico_forma      ON medicamento_clinico(forma_id);
CREATE INDEX IF NOT EXISTS idx_monografias_titulo ON monografias(titulo);

CREATE VIRTUAL TABLE IF NOT EXISTS monografias_fts USING fts5(
    atc_codigo UNINDEXED,
    titulo,
    contenido,
    tokenize = 'unicode61 remove_diacritics 2'
);
"""

VISTAS_CLINICAS = """
CREATE VIEW IF NOT EXISTS v_clinico AS
SELECT m.id            AS id,
       m.nombre        AS nombre,
       l.nombre        AS laboratorio,
       p.nombre        AS pais,
       c.atc_codigo    AS atc_codigo,
       a.descripcion   AS atc_descripcion,
       a.nivel         AS atc_nivel,
       v.nombre        AS via,
       f.nombre        AS forma,
       c.embarazo      AS embarazo,
       c.lactancia     AS lactancia,
       CASE WHEN mo.atc_codigo IS NULL THEN 0 ELSE 1 END AS tiene_monografia
FROM medicamentos m
LEFT JOIN laboratorios         l  ON l.id = m.laboratorio_id
LEFT JOIN paises               p  ON p.id = m.pais_id
LEFT JOIN medicamento_clinico  c  ON c.medicamento_id = m.id
LEFT JOIN atc                  a  ON a.codigo = c.atc_codigo
LEFT JOIN vias                 v  ON v.id = c.via_id
LEFT JOIN formas               f  ON f.id = c.forma_id
LEFT JOIN monografias          mo ON mo.atc_codigo = c.atc_codigo;

CREATE VIEW IF NOT EXISTS v_resumen_atc AS
SELECT a.codigo AS atc_codigo, a.descripcion AS descripcion, a.nivel AS nivel,
       COUNT(c.medicamento_id) AS medicamentos
FROM atc a
LEFT JOIN medicamento_clinico c ON c.atc_codigo = a.codigo
GROUP BY a.codigo, a.descripcion, a.nivel
ORDER BY medicamentos DESC, a.codigo;

CREATE VIEW IF NOT EXISTS v_resumen_vias AS
SELECT v.nombre AS via, COUNT(c.medicamento_id) AS medicamentos
FROM vias v
LEFT JOIN medicamento_clinico c ON c.via_id = v.id
GROUP BY v.nombre
ORDER BY medicamentos DESC;

CREATE VIEW IF NOT EXISTS v_resumen_formas AS
SELECT f.nombre AS forma, COUNT(c.medicamento_id) AS medicamentos
FROM formas f
LEFT JOIN medicamento_clinico c ON c.forma_id = f.id
GROUP BY f.nombre
ORDER BY medicamentos DESC;
"""

TABLAS_CLINICAS = ("monografias_fts", "medicamento_clinico", "monografias", "atc",
                   "formas", "vias")
VISTAS_CLINICAS_TODAS = ("v_clinico", "v_resumen_atc", "v_resumen_vias",
                         "v_resumen_formas")

# Columnas de texto de la monografia, en el orden en que se vuelcan al FTS.
COLUMNAS_MONOGRAFIA = (
    "mecanismo_accion", "indicaciones_terapeuticas", "indicaciones_posologia",
    "posologia", "modo_administracion", "contraindicaciones",
    "advertencias_precauciones", "insuficiencia_hepatica", "insuficiencia_renal",
    "interacciones", "embarazo_texto", "lactancia_texto", "conduccion",
    "reacciones_adversas", "sobredosificacion",
)
COLUMNAS_EXTRA = ("embarazo", "lactancia", "notas", "titulo", "url_monografia")

# Titulo de seccion -> columna. El orden importa: las variantes mas largas
# ("Indicaciones terapeuticas y Posologia") deben probarse antes que las cortas.
SECCIONES = (
    ("Indicaciones terapéuticas y Posología", "indicaciones_posologia"),
    ("Indicaciones terapéuticas / Posología", "indicaciones_posologia"),
    ("Efectos sobre la capacidad de conducir", "conduccion"),
    ("Advertencias y precauciones", "advertencias_precauciones"),
    ("Mecanismo de acción", "mecanismo_accion"),
    ("Indicaciones terapéuticas", "indicaciones_terapeuticas"),
    ("Modo de administración", "modo_administracion"),
    ("Insuficiencia hepática", "insuficiencia_hepatica"),
    ("Insuficiencia renal", "insuficiencia_renal"),
    ("Reacciones adversas", "reacciones_adversas"),
    ("Sobredosificación", "sobredosificacion"),
    ("Contraindicaciones", "contraindicaciones"),
    ("Interacciones", "interacciones"),
    ("Posología", "posologia"),
    ("Embarazo", "embarazo_texto"),
    ("Lactancia", "lactancia_texto"),
)

MARCAS_MURO = ("debes conectarte", "Regístrate", "Conéctate")

# --------------------------------------------------------------------------- #
# Analisis de la ficha HTML
# --------------------------------------------------------------------------- #

def _cabecera(sopa: BeautifulSoup) -> dict:
    """Extrae los metadatos del bloque superior de la ficha.

    Estructura real observada:

        <H1 class="rojoc33"><strong>IBUPROFENO CALOX Comprimido 600 mg</strong></H1>
        Nombre local: ... <br/>País: <b>Venezuela</b> <br/>Laboratorio: <b>Calox</b>
        <br/>Vía: <b>Vía oral</b> <br/>Forma: <b>Comprimido</b>
        <br/>ATC: <a href="/principios-activos-ibuprofeno-m01ae01-ve"><strong>Ibuprofeno (M01AE01)</strong></a>
    """
    titulo = sopa.find("h1")
    contenedor = titulo.parent if titulo else sopa
    # El bloque de metadatos termina en el primer <hr>
    html_cabecera = str(contenedor).split("<hr")[0]
    texto = BeautifulSoup(html_cabecera, "html.parser").get_text("\n")

    datos: dict = {"descripcion_atc": None}
    patrones = {
        "nombre_local": r"Nombre local:\s*([^\n]+)",
        "pais": r"Pa[ií]s:\s*([^\n]+)",
        "laboratorio": r"Laboratorio:\s*([^\n]+)",
        "via": r"V[ií]a:\s*([^\n]+)",
        "forma": r"Forma:\s*([^\n]+)",
    }
    for clave, patron in patrones.items():
        coincidencia = re.search(patron, texto)
        datos[clave] = coincidencia.group(1).strip() if coincidencia else None

    atc = re.search(r"ATC:\s*([^\n(]+?)\s*\(([A-Z0-9]{3,7})\)", texto)
    if atc:
        datos["descripcion_atc"] = atc.group(1).strip()
        datos["atc_codigo"] = atc.group(2)
    else:
        datos["atc_codigo"] = None

    # Jerarquia ATC: enlaces del tipo /atc-es?atc=M01AE
    jerarquia = []
    for enlace in (titulo.parent if titulo else sopa).find_all("a", href=True):
        if "/atc-es" not in enlace["href"]:
            continue
        etiqueta = re.sub(r"\s+", " ", enlace.get_text(" ", strip=True))
        coincidencia = re.match(r"([A-Z0-9]{1,7}):\s*(.+)", etiqueta)
        if coincidencia:
            jerarquia.append((coincidencia.group(1), coincidencia.group(2).strip()))
    datos["jerarquia"] = jerarquia

    # Enlace a la monografia del principio activo
    datos["url_monografia"] = None
    for enlace in (titulo.parent if titulo else sopa).find_all("a", href=True):
        if "/principios-activos-" in enlace["href"]:
            datos["url_monografia"] = "https://www.vademecum.es" + enlace["href"]
            break
    return datos


def _banderas(sopa: BeautifulSoup) -> dict:
    """Extrae embarazo, lactancia y notas de los iconos con `alt`/`title`.

    Marcado real:

        <img src=".../emb_4.gif" alt="Contraindicado" title="Contraindicado">
        <img src=".../lactancia_precaucion.gif" alt="lactancia: precaución" title="...">
        <img src=".../fotosensibilidada.gif" alt="Produce reacciones de
             fotosensibilidad. El paciente evitará exponerse a la luz solar.">
    """
    embarazo = lactancia = None
    notas: list[str] = []
    for imagen in sopa.find_all("img"):
        origen = imagen.get("src") or ""
        alt = (imagen.get("alt") or imagen.get("title") or "").strip()
        nombre = origen.rsplit("/", 1)[-1].lower()
        if not alt or "lupa" in nombre or "bandera" in nombre:
            continue
        if nombre.startswith("emb_"):
            embarazo = alt
        elif nombre.startswith("lactancia"):
            lactancia = re.sub(r"^lactancia:\s*", "", alt, flags=re.IGNORECASE)
        elif nombre not in {"logo.gif", "logo.png"}:
            notas.append(alt)
    return {
        "embarazo": _limpio(embarazo),
        "lactancia": _limpio(lactancia),
        "notas": _limpio(" | ".join(dict.fromkeys(notas))),
    }


def _secciones(sopa: BeautifulSoup) -> dict:
    """Recorre los <h2> de la ficha y vuelca cada seccion clinica en su columna.

    Los encabezados llevan el principio activo tras un <br/> interno:
    `<h2>Posología</br>Ibuprofeno</h2>`, por lo que se corta en el primer salto.
    La posologia, ademas, se sirve dentro de `<div id="texto_poso">` (oculto por
    CSS y mostrado con JavaScript); se prefiere ese div porque viene ya limpio.
    """
    valores: dict = {}
    for encabezado in sopa.find_all("h2"):
        primero = str(encabezado).split("<br")[0]
        titulo = BeautifulSoup(primero, "html.parser").get_text(" ", strip=True)
        clave = None
        for etiqueta, columna in SECCIONES:
            if _plano(titulo).startswith(_plano(etiqueta)):
                clave = columna
                break
        if not clave or clave in valores:
            continue
        trozos: list[str] = []
        for hermano in encabezado.next_siblings:
            nombre_etiqueta = getattr(hermano, "name", None)
            if nombre_etiqueta in {"h1", "h2", "h3"}:
                break
            if nombre_etiqueta in {"script", "style", "a"}:
                continue
            clase = " ".join(hermano.get("class") or []) if hasattr(hermano, "get") else ""
            if "zona_" in clase:
                continue  # botones "Conéctate / Regístrate"
            texto = getattr(hermano, "get_text", None)
            if texto:
                trozos.append(hermano.get_text(" ", strip=True))
        contenido = _limpio(" ".join(trozos))
        if contenido:
            valores[clave] = contenido

    # Posologia: el div oculto trae el texto definitivo y sin el aviso de registro
    oculto = sopa.find("div", id="texto_poso")
    if oculto:
        posologia = _limpio(oculto.get_text(" ", strip=True))
        if posologia:
            valores["posologia"] = posologia
    return valores


def parsear_ficha(html: str, url: str | None = None) -> dict:
    """Convierte el HTML de una ficha en un registro clinico listo para la BD.

    Devuelve un diccionario con los metadatos (`nombre_local`, `pais`,
    `laboratorio`, `via`, `forma`, `atc_codigo`, `descripcion_atc`, `jerarquia`,
    `url_monografia`, `embarazo`, `lactancia`, `notas`) y el texto de cada
    seccion clinica. Los campos que no aparezcan quedan en None.
    """
    sopa = BeautifulSoup(html, "html.parser")
    for etiqueta in sopa(["script", "nav", "footer"]):
        etiqueta.decompose()

    registro = _cabecera(sopa)
    registro.update(_banderas(sopa))
    registro.update(_secciones(sopa))

    # Respaldo: la ficha publica el ATC tambien en una variable JavaScript
    if not registro.get("atc_codigo"):
        variable = re.search(r"prm_atc\s*=\s*'([A-Z0-9]{3,7})'", html)
        if variable:
            registro["atc_codigo"] = variable.group(1)
    registro["url"] = url
    return registro


# --------------------------------------------------------------------------- #
# Volcado a SQLite
# --------------------------------------------------------------------------- #

def crear_esquema_clinico(conexion: sqlite3.Connection,
                          log: Callable[[str], None] | None = None) -> None:
    """Anade el esquema clinico a una base ya existente (no borra datos)."""
    log = log or (lambda _mensaje: None)
    conexion.executescript(ESQUEMA_CLINICO)
    conexion.executescript(VISTAS_CLINICAS)
    conexion.commit()
    log("   esquema clinico listo (atc, monografias, vias, formas, "
        "medicamento_clinico + 4 vistas)")


def volcar_registros(conexion: sqlite3.Connection, registros: list[dict],
                     log: Callable[[str], None] | None = None) -> dict:
    """Vuelca los registros clinicos en las tablas y devuelve contadores.

    Es idempotente: el ATC y las monografias se fusionan con COALESCE (nunca se
    pisa un texto ya guardado con un NULL) y `medicamento_clinico` se reemplaza
    por medicamento, de modo que se puede reejecutar sin duplicar filas.
    """
    log = log or (lambda _mensaje: None)
    from crear_bd_vademecum import _id_dimension

    cache: dict[str, int] = {}
    momento = time.strftime("%Y-%m-%dT%H:%M:%S")
    completos = 0

    for registro in registros:
        codigo = registro.get("atc_codigo")
        if codigo:
            # Descripciones conocidas: la del propio codigo y las de sus padres
            descripciones = {codigo: registro.get("descripcion_atc") or codigo}
            for codigo_padre, descripcion in registro.get("jerarquia") or []:
                descripciones.setdefault(codigo_padre, descripcion)
            for nivel in derivar_jerarquia(codigo):
                # `descripcion NOT NULL`: si el sitio no la da se usa el codigo
                conexion.execute(
                    """INSERT INTO atc(codigo, descripcion, nivel, padre)
                       VALUES (?, ?, ?, ?)
                       ON CONFLICT(codigo) DO UPDATE SET
                           descripcion = CASE WHEN atc.descripcion = atc.codigo
                                              THEN excluded.descripcion
                                              ELSE atc.descripcion END""",
                    (nivel, descripciones.get(nivel) or nivel, len(nivel),
                     padre_de(nivel)),
                )

            columnas = ("atc_codigo", "titulo", "url_monografia", "embarazo",
                        "lactancia", "notas", *COLUMNAS_MONOGRAFIA, "actualizado_en")
            valores = {
                "atc_codigo": codigo,
                "titulo": registro.get("descripcion_atc") or codigo,
                "url_monografia": registro.get("url_monografia"),
                "actualizado_en": momento,
            }
            for columna in (*COLUMNAS_MONOGRAFIA, "embarazo", "lactancia", "notas"):
                valores[columna] = registro.get(columna)
            asignaciones = ", ".join(
                f"{c} = COALESCE(excluded.{c}, monografias.{c})"
                for c in columnas if c != "atc_codigo")
            conexion.execute(
                f"""INSERT INTO monografias({', '.join(columnas)})
                    VALUES ({', '.join('?' * len(columnas))})
                    ON CONFLICT(atc_codigo) DO UPDATE SET {asignaciones}""",
                tuple(valores.get(c) for c in columnas),
            )

        conexion.execute(
            """INSERT OR REPLACE INTO medicamento_clinico
               (medicamento_id, atc_codigo, via_id, forma_id, embarazo, lactancia,
                url_monografia, leido_en, error)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (str(registro["id"]),
             codigo,
             _id_dimension(conexion, "vias", registro["via"], cache)
             if registro.get("via") else None,
             _id_dimension(conexion, "formas", registro["forma"], cache)
             if registro.get("forma") else None,
             registro.get("embarazo"), registro.get("lactancia"),
             registro.get("url_monografia"), momento, registro.get("error")),
        )
        if codigo and not registro.get("error"):
            completos += 1

    conexion.commit()
    contadores = {
        "procesados": len(registros),
        "con_atc": completos,
        "atc": conexion.execute("SELECT COUNT(*) FROM atc").fetchone()[0],
        "monografias": conexion.execute("SELECT COUNT(*) FROM monografias").fetchone()[0],
        "vias": conexion.execute("SELECT COUNT(*) FROM vias").fetchone()[0],
        "formas": conexion.execute("SELECT COUNT(*) FROM formas").fetchone()[0],
    }
    log(f"   volcados {contadores['procesados']} registros | ATC distintos: "
        f"{contadores['atc']} | monografias: {contadores['monografias']} | "
        f"vias: {contadores['vias']} | formas: {contadores['formas']}")
    return contadores


# --------------------------------------------------------------------------- #
# Busqueda y resumen
# --------------------------------------------------------------------------- #

def construir_fts_monografias(conexion: sqlite3.Connection,
                              log: Callable[[str], None] | None = None) -> int:
    """Reconstruye el indice FTS5 de las monografias clinicas."""
    log = log or (lambda _mensaje: None)
    conexion.execute("DELETE FROM monografias_fts")
    texto = " || ' ' || ".join(
        f"COALESCE(NULLIF({columna}, ''), '')" for columna in COLUMNAS_MONOGRAFIA)
    conexion.execute(
        f"""INSERT INTO monografias_fts(atc_codigo, titulo, contenido)
            SELECT atc_codigo, COALESCE(titulo, atc_codigo),
                   {texto} || ' ' || COALESCE(notas, '')
            FROM monografias"""
    )
    conexion.commit()
    conexion.execute("INSERT INTO monografias_fts(monografias_fts) VALUES('optimize')")
    conexion.commit()
    total = conexion.execute("SELECT COUNT(*) FROM monografias_fts").fetchone()[0]
    log(f"   indice FTS5 de monografias: {total} filas")
    return total


def buscar_monografias(conexion: sqlite3.Connection, termino: str,
                       limite: int = 5) -> list[sqlite3.Row]:
    """Busca texto dentro de las monografias (mismo criterio que `buscar`)."""
    from crear_bd_vademecum import _consulta_fts

    consulta = _consulta_fts(termino)
    if not consulta:
        return []
    return conexion.execute(
        """SELECT m.atc_codigo, m.titulo,
                  snippet(monografias_fts, 2, '[', ']', '...', 12) AS extracto
           FROM monografias_fts
           JOIN monografias m ON m.atc_codigo = monografias_fts.atc_codigo
           WHERE monografias_fts MATCH ?
           ORDER BY rank
           LIMIT ?""",
        (consulta, limite),
    ).fetchall()


def resumen_clinico(conexion: sqlite3.Connection) -> dict:
    """Contadores del enriquecimiento (cobertura de ATC, via, forma, monografia)."""
    def contar(sql: str) -> int:
        return conexion.execute(sql).fetchone()[0]

    return {
        "medicamentos": contar("SELECT COUNT(*) FROM medicamentos"),
        "leidos": contar("SELECT COUNT(*) FROM medicamento_clinico WHERE leido_en IS NOT NULL"),
        "con_atc": contar("SELECT COUNT(*) FROM medicamento_clinico WHERE atc_codigo IS NOT NULL"),
        "con_via": contar("SELECT COUNT(*) FROM medicamento_clinico WHERE via_id IS NOT NULL"),
        "con_forma": contar("SELECT COUNT(*) FROM medicamento_clinico WHERE forma_id IS NOT NULL"),
        "con_error": contar("SELECT COUNT(*) FROM medicamento_clinico WHERE error IS NOT NULL"),
        "atc_distintos": contar("SELECT COUNT(DISTINCT atc_codigo) FROM medicamento_clinico"),
        "atc_nodos": contar("SELECT COUNT(*) FROM atc"),
        "monografias": contar("SELECT COUNT(*) FROM monografias"),
        "con_indicaciones": contar(
            "SELECT COUNT(*) FROM monografias WHERE indicaciones_terapeuticas IS NOT NULL"),
        "con_posologia": contar("SELECT COUNT(*) FROM monografias WHERE posologia IS NOT NULL"),
        "con_interacciones": contar(
            "SELECT COUNT(*) FROM monografias WHERE interacciones IS NOT NULL"),
        "vias": contar("SELECT COUNT(*) FROM vias"),
        "formas": contar("SELECT COUNT(*) FROM formas"),
    }


# --------------------------------------------------------------------------- #
# Motor de recoleccion (reanudable)
# --------------------------------------------------------------------------- #

def enriquecer(ruta_bd: str = BD_POR_DEFECTO, ruta_jsonl: str = JSONL_POR_DEFECTO,
               limite: int | None = None, desde: str | None = None,
               delay: float = DELAY, reprocesar: bool = False,
               guardar_cada: int = 25, pausa_entre_lotes: float = 2.0,
               log: Callable[[str], None] | None = None) -> dict:
    """Descarga las fichas pendientes y vuelca su contenido clinico en la base.

    Cada `guardar_cada` fichas se vuelca a SQLite y al JSONL, de modo que si el
    proceso se interrumpe (Ctrl-C, corte de red) no se pierde el trabajo hecho:
    al relanzarlo solo se piden las fichas que falten.
    """
    log = log or (lambda mensaje: print(mensaje, flush=True))
    if not os.path.exists(ruta_bd):
        raise FileNotFoundError(
            f"No existe la base de datos: {ruta_bd}\n"
            "Ejecuta primero: python crear_bd_vademecum.py")

    with abrir_bd(ruta_bd) as conexion:
        crear_esquema_clinico(conexion, log=log)

        def ya_leidos() -> set[str]:
            return {fila[0] for fila in conexion.execute(
                "SELECT medicamento_id FROM medicamento_clinico WHERE leido_en IS NOT NULL")}

        # Recupera lo que quedo en el JSONL pero no llego a SQLite
        pendientes = {} if reprocesar else leer_jsonl(ruta_jsonl)
        pendientes = {k: v for k, v in pendientes.items() if k not in ya_leidos()}
        if pendientes:
            log(f"[1/3] Recuperando {len(pendientes)} fichas del JSONL anterior")
            volcar_registros(conexion, list(pendientes.values()), log=log)

        consulta = "SELECT id, url_ficha AS url FROM medicamentos WHERE url_ficha <> ''"
        parametros: list = []
        if desde:
            consulta += " AND id >= ?"
            parametros.append(desde)
        consulta += " ORDER BY id"
        todos = conexion.execute(consulta, parametros).fetchall()
        tareas = [fila for fila in todos if fila["id"] not in ya_leidos()]
        if limite:
            tareas = tareas[:limite]

        log(f"[2/3] Fichas pendientes: {len(tareas)} de {len(todos)}")
        if not tareas:
            construir_fts_monografias(conexion, log=log)
            return resumen_clinico(conexion)

        log(f"[3/3] Descargando con pausa de {delay} s entre fichas")
        lote: list[dict] = []
        inicio = time.time()
        con_atc = errores = 0
        ultimo = None

        for numero, fila in enumerate(tareas, 1):
            ultimo = fila["id"]
            registro: dict = {"id": fila["id"]}
            try:
                html = descargar_html(fila["url"], delay=0, cache_dir=None)
                registro.update(parsear_ficha(html, fila["url"]))
                if registro.get("atc_codigo"):
                    con_atc += 1
            except KeyboardInterrupt:
                log("   interrumpido por el usuario: guardando lo pendiente...")
                break
            except Exception as error:  # noqa: BLE001 - se anota y se continua
                registro["error"] = f"{type(error).__name__}: {error}"
                errores += 1
            lote.append(registro)

            if len(lote) >= guardar_cada or numero == len(tareas):
                escribir_jsonl(ruta_jsonl, lote)
                volcar_registros(conexion, lote, log=log)
                transcurrido = time.time() - inicio
                ritmo = numero / transcurrido if transcurrido else 0
                restante = (len(tareas) - numero) / ritmo if ritmo else 0
                log(f"   [{numero}/{len(tareas)}] id {fila['id']} | ATC "
                    f"{registro.get('atc_codigo') or 'n/d'} | via "
                    f"{registro.get('via') or 'n/d'} | ultimo id guardado: {ultimo} | "
                    f"{ritmo * 60:.1f} fichas/min | ETA {restante / 60:.0f} min")
                lote = []
                time.sleep(pausa_entre_lotes)  # respiro para no saturar el servidor
            time.sleep(delay)

        if lote:  # resto sin guardar (fin normal o Ctrl-C)
            escribir_jsonl(ruta_jsonl, lote)
            volcar_registros(conexion, lote, log=log)

        construir_fts_monografias(conexion, log=log)
        conexion.execute("ANALYZE")
        conexion.commit()
        log(f"   descarga terminada: {con_atc} con ATC, {errores} errores")
        return resumen_clinico(conexion)


# --------------------------------------------------------------------------- #
# Interfaz de linea de comandos
# --------------------------------------------------------------------------- #

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Anade la clasificacion clinica (ATC, via, forma y monografias) "
                    "a la base de medicamentos de vademecum.es")
    parser.add_argument("--db", default=BD_POR_DEFECTO,
                        help=f"base de datos SQLite (por defecto: {BD_POR_DEFECTO})")
    parser.add_argument("--jsonl", default=JSONL_POR_DEFECTO,
                        help=f"fichero reanudable de fichas ya leidas "
                             f"(por defecto: {JSONL_POR_DEFECTO})")
    parser.add_argument("--limite", type=int, default=None,
                        help="maximo de fichas a descargar en esta pasada")
    parser.add_argument("--desde", default=None,
                        help="reanudar a partir de este id de medicamento (incluido)")
    parser.add_argument("--delay", type=float, default=DELAY,
                        help=f"segundos entre fichas (por defecto: {DELAY})")
    parser.add_argument("--guardar-cada", type=int, default=25,
                        help="fichas por lote antes de volcar a disco (por defecto: 25)")
    parser.add_argument("--reprocesar", action="store_true",
                        help="ignorar el progreso guardado y volver a pedir todas")
    parser.add_argument("--solo-informe", action="store_true",
                        help="no descarga nada: solo muestra el estado actual")
    parser.add_argument("--buscar", default=None,
                        help="termino a buscar dentro de las monografias (FTS)")
    parser.add_argument("--reconstruir-fts", action="store_true",
                        help="reconstruir el indice FTS5 de monografias y salir")
    opciones = parser.parse_args(argv)

    if not os.path.exists(opciones.db):
        print(f"Error: no existe la base de datos: {opciones.db}")
        print("Ejecuta primero: python crear_bd_vademecum.py")
        return 1

    try:
        if opciones.solo_informe or opciones.reconstruir_fts or opciones.buscar:
            with abrir_bd(opciones.db) as conexion:
                crear_esquema_clinico(conexion)
                if opciones.reconstruir_fts:
                    construir_fts_monografias(conexion,
                                              log=lambda m: print(m, flush=True))
                    return 0
                if opciones.buscar:
                    print(f"\nBusqueda de '{opciones.buscar}' en las monografias:")
                    for fila in buscar_monografias(conexion, opciones.buscar, 5):
                        print(f"\n   [{fila['atc_codigo']}] {fila['titulo']}")
                        print(f"      {fila['extracto']}")
                if opciones.solo_informe:
                    mostrar_informe(conexion)
            return 0

        estadisticas = enriquecer(
            ruta_bd=opciones.db, ruta_jsonl=opciones.jsonl, limite=opciones.limite,
            desde=opciones.desde, delay=opciones.delay,
            reprocesar=opciones.reprocesar, guardar_cada=opciones.guardar_cada,
        )
    except FileNotFoundError as error:
        print(f"Error: {error}")
        return 1
    except KeyboardInterrupt:
        print("\nInterrumpido por el usuario.")
        return 130

    print("\n" + "=" * 62)
    print(f"Enriquecimiento clinico: {os.path.abspath(opciones.db)}")
    for clave, valor in estadisticas.items():
        print(f"   {clave:20s}: {valor}")
    print("=" * 62)
    return 0


def mostrar_informe(conexion: sqlite3.Connection) -> None:
    """Imprime cobertura del enriquecimiento y los grupos mas frecuentes."""
    datos = resumen_clinico(conexion)
    print("\n" + "=" * 62)
    print("ESTADO DEL ENRIQUECIMIENTO CLINICO")
    print("=" * 62)
    for clave, valor in datos.items():
        print(f"   {clave:20s}: {valor}")

    if datos["medicamentos"]:
        porcentaje = 100 * datos["leidos"] / datos["medicamentos"]
        print(f"\n   cobertura de fichas leidas: {porcentaje:.1f} %")

    if datos["atc_distintos"]: 
        print("\nTop 10 grupos ATC (nivel 5) por numero de medicamentos:")
        for fila in conexion.execute(
                """SELECT atc_codigo, descripcion, medicamentos FROM v_resumen_atc
                   WHERE nivel = 7 AND medicamentos > 0 LIMIT 10"""):
            print(f"   {fila['atc_codigo']:9s} {fila['medicamentos']:5d}  {fila['descripcion']}")

    if datos["vias"]:
        print("\nTop 8 vias de administracion:")
        for fila in conexion.execute("SELECT * FROM v_resumen_vias LIMIT 8"):
            print(f"   {fila['medicamentos']:5d}  {fila['via']}")

    if datos["formas"]:
        print("\nTop 10 formas farmaceuticas:")
        for fila in conexion.execute("SELECT * FROM v_resumen_formas LIMIT 10"):
            print(f"   {fila['medicamentos']:5d}  {fila['forma']}")


def _plano(texto: str | None) -> str:
    """Normaliza un texto: sin acentos, en minusculas y sin espacios dobles.

    Se usa para comparar titulos de seccion, que en el sitio aparecen unas veces
    con acentos ("Posología") y otras sin ellos ("Posologia").
    """
    if not texto:
        return ""
    import unicodedata
    sin_tildes = "".join(c for c in unicodedata.normalize("NFD", texto)
                         if unicodedata.category(c) != "Mn")
    return re.sub(r"\s+", " ", sin_tildes).strip().lower()


def _limpio(texto: str | None) -> str | None:
    """Colapsa espacios, quita el aviso de registro y devuelve None si vacio.

    La ficha intercala el aviso "Para acceder a la informacion de posologia en
    Vademecum.es debes conectarte con tu email y clave o registrarte. Conectate
    Registrate" entre el titulo de la seccion y su contenido real; aqui se
    elimina para que el texto guardado sea solo informacion clinica.
    """
    if not texto:
        return None
    texto = re.sub(r"\s+", " ", texto).strip()
    texto = re.sub(
        r"Para acceder a la informaci[oó]n[^.]*\.\s*(?:Con[eé]ctate|Reg[ií]strate)?\s*",
        "", texto, flags=re.IGNORECASE)
    texto = re.sub(r"\b(?:Conéctate|Regístrate|Conectate|Registrate)\b\s*", "", texto)
    texto = re.sub(r"\s+", " ", texto).strip()
    return texto if len(texto) >= 3 else None


def derivar_jerarquia(codigo: str) -> list[str]:
    """Devuelve la cadena de codigos ATC desde el nivel 1 hasta el propio.

    Los niveles oficiales son 1, 3, 4, 5 y 7 caracteres:

        M01AE01 -> ['M', 'M01', 'M01A', 'M01AE', 'M01AE01']
        R05CB   -> ['R', 'R05', 'R05CB']        (algunas fichas dan solo 5)
    """
    if not codigo:
        return []
    longitudes = [n for n in (1, 3, 4, 5, 7) if n < len(codigo)] + [len(codigo)]
    return [codigo[:n] for n in longitudes]


def padre_de(codigo: str) -> str | None:
    """Codigo ATC del nivel inmediatamente superior (None si es de nivel 1)."""
    cadena = derivar_jerarquia(codigo)
    return cadena[-2] if len(cadena) > 1 else None


# --------------------------------------------------------------------------- #
# Persistencia incremental (JSONL)
# --------------------------------------------------------------------------- #

def leer_jsonl(ruta: str) -> dict[str, dict]:
    """Carga el JSONL de fichas ya procesadas: {id_medicamento: registro}."""
    procesadas: dict[str, dict] = {}
    if not os.path.exists(ruta):
        return procesadas
    with open(ruta, encoding="utf-8") as archivo:
        for linea in archivo:
            linea = linea.strip()
            if not linea:
                continue
            try:
                registro = json.loads(linea)
            except json.JSONDecodeError:
                continue  # linea truncada por un corte previo: se ignora
            if registro.get("id"):
                procesadas[str(registro["id"])] = registro
    return procesadas


def escribir_jsonl(ruta: str, registros: list[dict]) -> None:
    """Anade registros al JSONL de forma atomica (append + flush + fsync)."""
    if not registros:
        return
    with open(ruta, "a", encoding="utf-8") as archivo:
        for registro in registros:
            archivo.write(json.dumps(registro, ensure_ascii=False) + "\n")
        archivo.flush()
        os.fsync(archivo.fileno())


if __name__ == "__main__":
    sys.exit(main())

