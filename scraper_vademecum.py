# Instalar dependencias:
# pip install curl_cffi beautifulsoup4

from curl_cffi import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin
import json
import time
import string
import os

BASE_URL = "https://www.vademecum.es"
OUTPUT_FILE = "medicamentos_vademecum_venezuela.json"
DELAY = 1.5  # segundos entre peticiones (sé respetuoso con el servidor)
MAX_PAGES = 50  # límite de seguridad por letra


def get_soup(session, url, max_retries=3):
    """Hace la petición con reintentos y devuelve un BeautifulSoup o None."""
    for intento in range(1, max_retries + 1):
        try:
            response = session.get(url, timeout=30)
            if response.status_code == 200:
                return BeautifulSoup(response.text, "html.parser")
            elif response.status_code == 404:
                print(f"   ⚠️  404 en {url}")
                return None
            else:
                print(f"   ⚠️  HTTP {response.status_code} en {url} (intento {intento})")
        except Exception as e:
            print(f"   ⚠️  Error en {url} (intento {intento}): {e}")
        time.sleep(2 * intento)  # backoff exponencial
    return None


def parse_medicamentos(soup):
    """Extrae la lista de medicamentos de una página y devuelve una lista de dicts."""
    medicamentos = []
    for item in soup.select("div.med-item"):
        name_tag = item.select_one("a.med-item__name")
        if not name_tag:
            continue

        nombre = name_tag.get_text(strip=True)
        href = name_tag.get("href", "").strip()
        url_ficha = urljoin(BASE_URL, href) if href else None

        flag_tag = item.select_one("img.med-item__flag")
        pais = flag_tag.get("alt") if flag_tag else None

        equiv_tag = item.select_one("a.med-item__equiv")
        url_equiv = (
            urljoin(BASE_URL, equiv_tag["href"])
            if equiv_tag and equiv_tag.get("href")
            else None
        )

        medicamentos.append({
            "nombre": nombre,
            "url_ficha": url_ficha,
            "pais": pais,
            "url_equivalencias": url_equiv,
        })
    return medicamentos


def has_next_page(soup):
    """Devuelve True si hay un enlace de 'siguiente página'."""
    # Ajusta este selector si en tu HTML el botón de siguiente tiene otra clase.
    next_link = soup.select_one("a.pagination__next, a.next, li.next > a")
    return next_link is not None


def scrape_letra(session, letra):
    """Recorre todas las páginas de una letra y devuelve la lista de medicamentos."""
    print(f"\n🔤 Procesando letra '{letra.upper()}'...")
    todos = []
    page = 1

    while page <= MAX_PAGES:
        # Muchos sitios usan ?page=N; si no, la primera página es la URL base.
        if page == 1:
            url = f"{BASE_URL}/venezuela/ve/{letra}"
        else:
            url = f"{BASE_URL}/venezuela/ve/{letra}?page={page}"

        soup = get_soup(session, url)
        if soup is None:
            break

        items = parse_medicamentos(soup)
        print(f"   📄 Página {page}: {len(items)} medicamentos")

        if not items:
            break

        todos.extend(items)

        if not has_next_page(soup):
            break

        page += 1
        time.sleep(DELAY)

    print(f"   ✅ Letra '{letra.upper()}': {len(todos)} medicamentos en total")
    return todos


def main():
    #session = requests.Session(impersonate="chrome")
    session = requests.Session(impersonate="chrome124")
    todos_medicamentos = []
    resumen = {}

    for letra in string.ascii_lowercase:  # 'a' a 'z'
        try:
            medicamentos = scrape_letra(session, letra)
            resumen[letra] = len(medicamentos)
            todos_medicamentos.extend(medicamentos)
        except KeyboardInterrupt:
            print("\n⛔ Interrumpido por el usuario.")
            break
        except Exception as e:
            print(f"   ❌ Error inesperado en letra '{letra}': {e}")
            resumen[letra] = 0
            # Si el servidor devuelve 500 de forma persistente, es señal clara de bloqueo anti-bot
            raise RuntimeError("Aborting: Server returned HTTP 500 for multiple letters. Likely blocked by anti-bot measures — check headers/IP/JS challenge.")

        time.sleep(DELAY)  # pausa entre letras

    # Guardar JSON final
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(todos_medicamentos, f, ensure_ascii=False, indent=4)

    print("\n" + "=" * 50)
    print(f"✅ Total medicamentos extraídos: {len(todos_medicamentos)}")
    print(f"📁 Guardados en: {os.path.abspath(OUTPUT_FILE)}")
    print("\n📊 Resumen por letra:")
    for letra, count in resumen.items():
        print(f"   {letra.upper()}: {count}")


if __name__ == "__main__":
    main()