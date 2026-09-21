#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Recolector de medicamentos de Venezuela publicados en vademecum.es.

Estructura real del sitio (verificada el 2026-09-21):

  * Indice alfabetico:  https://www.vademecum.es/venezuela/ve/alfa
        - Muestra los 50 medicamentos mas consultados + navegacion A-Z.
  * Nivel 1 (letra):    https://www.vademecum.es/venezuela/ve/alfa/<letra>
        - Devuelve 50 resultados y la navegacion de SEGUNDA letra.
  * Nivel 2 (2 letras): https://www.vademecum.es/venezuela/ve/alfa/<l1>/<l2>
        - Devuelve la lista COMPLETA de ese prefijo (sin limite de 50).
        - El sitio NO usa paginacion numerica: el drill-down por dos letras
          la sustituye (por eso ?page=N ignoraba el parametro).

Cada resultado es un <div class="med-item"> con:
    a.med-item__name     -> nombre + href de la ficha (/venezuela/medicamento/<id>/<slug>)
    div.med-item__detail -> laboratorio titular
    img.med-item__flag   -> pais (alt, p. ej. "Venezuela")
    a.med-item__equiv    -> href de equivalencias internacionales

Uso:
    python vademecum_ve.py                      # recoleccion completa
    python vademecum_ve.py --letras a c         # solo las letras a y c
    python vademecum_ve.py --sin-subletras      # solo nivel 1 (50 por letra)
    python vademecum_ve.py --delay 0.5 --salida medicamentos.json
"""

from __future__ import annotations

import argparse
import gzip
import json
import os
import re
import subprocess
import sys
import time
import urllib.request
from typing import Callable

from bs4 import BeautifulSoup

# --------------------------------------------------------------------------- #
# Configuracion
# --------------------------------------------------------------------------- #

BASE_URL = "https://www.vademecum.es"
RUTA_INDICE = "/venezuela/ve/alfa"

OUTPUT_FILE = "medicamentos_vademecum_venezuela.json"
CACHE_DIR = "cache_vademecum"
LOG_FILE = "logs/recoleccion_vademecum.log"

DELAY = 1.0          # segundos entre peticiones (respetuoso con el servidor)
MAX_REINTENTOS = 4   # reintentos por URL
TIMEOUT = 45         # segundos

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)
CABECERAS = {
    "User-Agent": USER_AGENT,
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "es-ES,es;q=0.9,en;q=0.8",
}

# Letras del primer nivel (a-z) mas el bloque numerico "3".
LETRAS_PRIMER_NIVEL = [chr(c) for c in range(ord("a"), ord("z") + 1)] + ["3"]

RE_ID_FICHA = re.compile(r"/venezuela/medicamento/(\d+)/")
RE_ESPACIOS = re.compile(r"\s+")


class RecoleccionError(RuntimeError):
    """Error irrecuperable durante la recoleccion."""


def _limpiar(texto: str) -> str:
    """Colapsa espacios/saltos de linea y quita espacios sobrantes."""
    return RE_ESPACIOS.sub(" ", texto or "").strip()


# --------------------------------------------------------------------------- #
# Capa HTTP: curl_cffi -> curl CLI -> urllib
# Se prueban varios clientes porque algunos WAF rechazan ciertos fingerprints
# TLS con HTTP 500/520 (lo que ocurria con curl_cffi impersonate="chrome124").
# --------------------------------------------------------------------------- #

def _descargar_curl_cffi(url: str, timeout: int = TIMEOUT):
    from curl_cffi import requests as curl_requests  # import diferido

    respuesta = curl_requests.get(
        url, headers=CABECERAS, impersonate="chrome", timeout=timeout
    )
    return respuesta.status_code, respuesta.text


def _descargar_curl(url: str, timeout: int = TIMEOUT):
    proceso = subprocess.run(
        ["curl", "-sS", "-L", "--compressed", "-A", USER_AGENT,
         "--max-time", str(timeout), "-w", "\n[[HTTP:%{http_code}]]", url],
        capture_output=True, text=True, errors="replace",
    )
    if proceso.returncode != 0:
        raise RuntimeError(
            f"curl codigo {proceso.returncode}: {proceso.stderr.strip()[:200]}"
        )
    salida = proceso.stdout
    coincidencia = re.search(r"\[\[HTTP:(\d{3})\]\].*$", salida, flags=re.DOTALL)
    if not coincidencia:
        return 200, salida
    return int(coincidencia.group(1)), salida[: coincidencia.start()]


def _descargar_urllib(url: str, timeout: int = TIMEOUT):
    peticion = urllib.request.Request(
        url, headers={**CABECERAS, "Accept-Encoding": "gzip"}
    )
    with urllib.request.urlopen(peticion, timeout=timeout) as respuesta:
        datos = respuesta.read()
        if respuesta.headers.get("Content-Encoding") == "gzip":
            datos = gzip.decompress(datos)
        codificacion = respuesta.headers.get_content_charset() or "utf-8"
        return respuesta.status, datos.decode(codificacion, errors="replace")


DESCARGADORES = (
    ("curl_cffi", _descargar_curl_cffi),
    ("curl", _descargar_curl),
    ("urllib", _descargar_urllib),
)


def _nombre_cache(url: str) -> str:
    """Nombre de archivo estable y legible para cachear una URL."""
    relativo = url.replace(BASE_URL, "").strip("/") or "indice"
    return re.sub(r"[^A-Za-z0-9._-]+", "_", relativo) + ".html"


def descargar_html(url: str, delay: float = DELAY,
                   max_reintentos: int = MAX_REINTENTOS,
                   cache_dir: str | None = None,
                   log: Callable[[str], None] | None = None) -> str:
    """Descarga una URL y devuelve su HTML.

    Prueba varios clientes HTTP, reintenta con backoff exponencial ante
    429/5xx y opcionalmente cachea el HTML en disco para poder reanudar una
    recoleccion interrumpida sin volver a golpear el servidor.
    """
    log = log or (lambda _mensaje: None)

    ruta_cache = None
    if cache_dir:
        os.makedirs(cache_dir, exist_ok=True)
        ruta_cache = os.path.join(cache_dir, _nombre_cache(url))
        if os.path.exists(ruta_cache):
            with open(ruta_cache, encoding="utf-8") as archivo:
                html = archivo.read()
            if html.strip():
                log(f"    cache: {url} ({len(html)} bytes)")
                return html

    ultimo_error = "sin detalle"
    for intento in range(1, max_reintentos + 1):
        for nombre, descargador in DESCARGADORES:
            try:
                codigo, html = descargador(url)
            except Exception as error:  # noqa: BLE001 - se registra y se reintenta
                ultimo_error = f"{nombre}: {type(error).__name__}: {error}"
                log(f"    aviso {nombre} intento {intento}: {ultimo_error[:160]}")
                continue

            if codigo == 200 and html.strip():
                if ruta_cache:
                    with open(ruta_cache, "w", encoding="utf-8") as archivo:
                        archivo.write(html)
                log(f"    ok {nombre}: {url} ({len(html)} bytes)")
                return html

            ultimo_error = f"{nombre}: HTTP {codigo}"
            if codigo == 404:
                raise RecoleccionError(f"404 no encontrado: {url}")
            log(f"    aviso {nombre} intento {intento}: HTTP {codigo} en {url}")

        if intento < max_reintentos:
            espera = min(2 ** intento, 20)
            log(f"    reintentando en {espera}s...")
            time.sleep(espera)

    raise RecoleccionError(
        f"No se pudo descargar {url} tras {max_reintentos} intentos ({ultimo_error})"
    )



# --------------------------------------------------------------------------- #
# Parsers del HTML de listado
# --------------------------------------------------------------------------- #

def parse_medicamentos(html: str, url_fuente: str | None = None) -> list[dict]:
    """Extrae todos los medicamentos (`div.med-item`) de un HTML.

    Devuelve una lista de dicts con las claves: id, nombre, laboratorio, pais,
    slug, url_ficha, url_equivalencias y url_fuente.
    """
    sopa = BeautifulSoup(html, "html.parser")
    medicamentos: list[dict] = []

    for item in sopa.select("div.med-item"):
        enlace = item.select_one("a.med-item__name")
        if enlace is None:
            continue

        nombre = _limpiar(enlace.get_text(" ", strip=True))
        href = (enlace.get("href") or "").strip()
        if not nombre or not href:
            continue

        coincidencia_id = RE_ID_FICHA.search(href)
        detalle = item.select_one("div.med-item__detail")
        bandera = item.select_one("img.med-item__flag")
        equivalencias = item.select_one("a.med-item__equiv")
        href_equiv = (equivalencias.get("href") or "").strip() if equivalencias else ""

        medicamentos.append({
            "id": coincidencia_id.group(1) if coincidencia_id else None,
            "nombre": nombre,
            "laboratorio": _limpiar(detalle.get_text(" ", strip=True)) if detalle else None,
            "pais": ((bandera.get("alt") or "").strip() or None) if bandera else None,
            "slug": href.rstrip("/").split("/")[-1] or None,
            "url_ficha": BASE_URL + href if href.startswith("/") else href,
            "url_equivalencias": (
                (BASE_URL + href_equiv) if href_equiv.startswith("/") else (href_equiv or None)
            ),
            "url_fuente": url_fuente,
        })

    return medicamentos


def parse_subletras(html: str) -> list[dict]:
    """Devuelve los enlaces de segunda letra (`a.alpha-sub__item`) de un listado.

    Cada elemento es {"etiqueta": "am", "url": "https://.../alfa/a/m"}.
    """
    sopa = BeautifulSoup(html, "html.parser")
    subletras: list[dict] = []
    vistos: set[str] = set()

    for enlace in sopa.select("a.alpha-sub__item"):
        href = (enlace.get("href") or "").strip()
        if not href or href in vistos:
            continue
        vistos.add(href)
        subletras.append({
            "etiqueta": _limpiar(enlace.get_text(" ", strip=True)),
            "url": BASE_URL + href if href.startswith("/") else href,
        })

    return subletras


def parse_letras_primer_nivel(html: str) -> list[str]:
    """Devuelve las letras del indice alfabetico (`a.alpha-nav__letter`)."""
    sopa = BeautifulSoup(html, "html.parser")
    letras = [
        _limpiar(enlace.get_text(" ", strip=True)).lower()
        for enlace in sopa.select("a.alpha-nav__letter")
    ]
    return [letra for letra in dict.fromkeys(letras) if letra]


def parse_titulo_listado(html: str) -> str | None:
    """Titulo del bloque de resultados (util para el resumen/log)."""
    sopa = BeautifulSoup(html, "html.parser")
    titulo = sopa.select_one(".popular-card__title") or sopa.select_one("h1")
    return _limpiar(titulo.get_text(" ", strip=True)) if titulo else None


def url_letra(letra: str, subletra: str | None = None) -> str:
    """Construye la URL de nivel 1 o de nivel 2 para una letra/subletra."""
    ruta = f"{BASE_URL}{RUTA_INDICE}/{letra}"
    if subletra:
        ruta = f"{ruta}/{subletra}"
    return ruta



# --------------------------------------------------------------------------- #
# Recoleccion
# --------------------------------------------------------------------------- #

def _clave_medicamento(medicamento: dict) -> str:
    """Clave de deduplicacion: codigo nacional si existe, si no nombre+laboratorio."""
    if medicamento.get("id"):
        return f"id:{medicamento['id']}"
    return f"nm:{medicamento.get('nombre', '')}|{(medicamento.get('laboratorio') or '').lower()}"


def recolectar_medicamentos(letras: list[str] | None = None,
                            incluir_subletras: bool = True,
                            delay: float = DELAY,
                            cache_dir: str | None = CACHE_DIR,
                            max_subletras: int | None = None,
                            guardar_en: str | None = None,
                            log: Callable[[str], None] | None = None,
                            on_progreso: Callable[[dict], None] | None = None) -> dict:
    """Recolecta los medicamentos de https://www.vademecum.es/venezuela/ve/alfa.

    Recorre el indice alfabetico y, por cada letra, visita sus sub-paginas de
    segunda letra, que son las que contienen la lista completa (el primer nivel
    solo muestra 50 resultados). Deduplica por codigo de medicamento.

    Parametros
    ----------
    letras            : letras a procesar (por defecto a-z mas "3").
    incluir_subletras : si es False se usa solo el nivel 1 (50 por letra).
    delay             : pausa en segundos entre peticiones.
    cache_dir         : carpeta de cache del HTML (None para no cachear).
    max_subletras     : limite de sub-paginas por letra (util para pruebas).
    guardar_en        : si se indica, escribe el JSON parcial tras cada letra.
    log               : callback de logging (por defecto, print).
    on_progreso       : callback invocado con el estado tras cada letra.

    Devuelve
    --------
    dict con las claves: fuente, fecha, total, por_letra, errores, medicamentos.
    """
    log = log or (lambda mensaje: print(mensaje, flush=True))
    letras = letras or list(LETRAS_PRIMER_NIVEL)
    indice_url = BASE_URL + RUTA_INDICE

    log(f"[1/3] Leyendo indice alfabetico: {indice_url}")
    html_indice = descargar_html(indice_url, delay=delay, cache_dir=cache_dir, log=log)
    letras_disponibles = parse_letras_primer_nivel(html_indice)
    log(f"      letras en el indice: {' '.join(letras_disponibles) or '(no detectadas)'}")

    letras_objetivo = [letra for letra in letras if letra in letras_disponibles] or letras

    medicamentos: dict[str, dict] = {}
    por_letra: dict[str, int] = {}
    errores: list[dict] = []

    log(f"[2/3] Recolectando {len(letras_objetivo)} letras "
        f"({'con' if incluir_subletras else 'sin'} sub-paginas)")


    for posicion, letra in enumerate(letras_objetivo, start=1):
        log(f"\n[{posicion}/{len(letras_objetivo)}] letra '{letra.upper()}'")
        urls_nivel2: list[dict] = []
        try:
            html_letra = descargar_html(url_letra(letra), delay=delay,
                                        cache_dir=cache_dir, log=log)
            log(f"      titulo: {parse_titulo_listado(html_letra)}")
            for medicamento in parse_medicamentos(html_letra, url_letra(letra)):
                medicamentos.setdefault(_clave_medicamento(medicamento), medicamento)

            if incluir_subletras:
                urls_nivel2 = parse_subletras(html_letra)
                log(f"      sub-paginas de segunda letra: {len(urls_nivel2)}")
        except RecoleccionError as error:
            log(f"      ERROR en nivel 1: {error}")
            errores.append({"url": url_letra(letra), "error": str(error)})
            time.sleep(delay)
            continue

        seleccion = urls_nivel2[:max_subletras] if max_subletras else urls_nivel2
        for indice_sub, subletra in enumerate(seleccion, start=1):
            try:
                html_sub = descargar_html(subletra["url"], delay=delay,
                                          cache_dir=cache_dir, log=log)
                nuevos = parse_medicamentos(html_sub, subletra["url"])
                for medicamento in nuevos:
                    medicamentos.setdefault(_clave_medicamento(medicamento), medicamento)
                log(f"      [{indice_sub}/{len(seleccion)}] prefijo "
                    f"'{subletra['etiqueta']}': {len(nuevos)} medicamentos")
            except RecoleccionError as error:
                log(f"      ERROR en sub-pagina '{subletra['etiqueta']}': {error}")
                errores.append({"url": subletra["url"], "error": str(error)})
            finally:
                time.sleep(delay)

        por_letra[letra] = len(medicamentos)
        log(f"      acumulado: {len(medicamentos)} medicamentos unicos")

        if on_progreso:
            on_progreso({"letra": letra, "total": len(medicamentos),
                         "por_letra": dict(por_letra), "errores": len(errores)})
        if guardar_en:
            guardar_json(list(medicamentos.values()), guardar_en,
                         por_letra=por_letra, errores=errores)

    log(f"\n[3/3] Finalizado: {len(medicamentos)} medicamentos unicos, "
        f"{len(errores)} errores")

    return {
        "fuente": indice_url,
        "fecha": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "total": len(medicamentos),
        "por_letra": por_letra,
        "errores": errores,
        "medicamentos": list(medicamentos.values()),
    }


def guardar_json(medicamentos: list[dict], ruta: str,
                 por_letra: dict | None = None,
                 errores: list | None = None) -> str:
    """Escribe los medicamentos en un JSON legible y devuelve la ruta absoluta."""
    os.makedirs(os.path.dirname(os.path.abspath(ruta)) or ".", exist_ok=True)
    documento = {
        "fuente": BASE_URL + RUTA_INDICE,
        "fecha": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "total": len(medicamentos),
        "por_letra": por_letra or {},
        "errores": errores or [],
        "medicamentos": medicamentos,
    }
    with open(ruta, "w", encoding="utf-8") as archivo:
        json.dump(documento, archivo, ensure_ascii=False, indent=2)
    return os.path.abspath(ruta)



# --------------------------------------------------------------------------- #
# Interfaz de linea de comandos
# --------------------------------------------------------------------------- #

def _crear_logger(ruta_log: str | None) -> Callable[[str], None]:
    """Devuelve un logger que imprime y, opcionalmente, escribe en archivo."""
    archivo_log = None
    if ruta_log:
        os.makedirs(os.path.dirname(os.path.abspath(ruta_log)) or ".", exist_ok=True)
        archivo_log = open(ruta_log, "a", encoding="utf-8")
        archivo_log.write(f"\n=== {time.strftime('%Y-%m-%d %H:%M:%S')} ===\n")
        archivo_log.flush()

    def log(mensaje: str) -> None:
        print(mensaje, flush=True)
        if archivo_log:
            archivo_log.write(mensaje + "\n")
            archivo_log.flush()

    return log


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Recolecta los medicamentos de Venezuela publicados en vademecum.es"
    )
    parser.add_argument("--salida", default=OUTPUT_FILE,
                        help=f"ruta del JSON de salida (por defecto: {OUTPUT_FILE})")
    parser.add_argument("--letras", nargs="*", default=None,
                        help="letras concretas a procesar, p. ej. --letras a b c")
    parser.add_argument("--sin-subletras", action="store_true",
                        help="no descender al segundo nivel (solo 50 por letra)")
    parser.add_argument("--max-subletras", type=int, default=None,
                        help="limite de sub-paginas por letra (pruebas)")
    parser.add_argument("--delay", type=float, default=DELAY,
                        help=f"segundos entre peticiones (por defecto: {DELAY})")
    parser.add_argument("--cache-dir", default=CACHE_DIR,
                        help="carpeta de cache HTML; use '' para desactivar "
                             f"(por defecto: {CACHE_DIR})")
    parser.add_argument("--log", default=LOG_FILE,
                        help="archivo de log; use '' para desactivar "
                             f"(por defecto: {LOG_FILE})")
    opciones = parser.parse_args(argv)

    log = _crear_logger(opciones.log or None)
    letras = [letra.lower() for letra in opciones.letras] if opciones.letras else None

    try:
        resultados = recolectar_medicamentos(
            letras=letras,
            incluir_subletras=not opciones.sin_subletras,
            delay=opciones.delay,
            cache_dir=opciones.cache_dir or None,
            max_subletras=opciones.max_subletras,
            guardar_en=opciones.salida,
            log=log,
        )
    except KeyboardInterrupt:
        log("\nInterrumpido por el usuario.")
        return 130
    except RecoleccionError as error:
        log(f"\nError de recoleccion: {error}")
        return 1

    ruta = guardar_json(resultados["medicamentos"], opciones.salida,
                        por_letra=resultados["por_letra"],
                        errores=resultados["errores"])

    log("\n" + "=" * 60)
    log(f"Total medicamentos unicos: {resultados['total']}")
    log(f"Errores: {len(resultados['errores'])}")
    log(f"JSON guardado en: {ruta}")
    log("Resumen acumulado por letra:")
    for letra, acumulado in resultados["por_letra"].items():
        log(f"   {letra.upper()}: {acumulado}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

