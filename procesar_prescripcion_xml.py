#!/usr/bin/env python3
import sqlite3
import re
import xml.etree.ElementTree as ET
import os

DB_PATH = '/home/nedp/Desarrollo/Proyectos/Guia-Farmaceutica/android/app/src/main/assets/databases/medicamentos_guia_venezuela.db'
XML_PATH = '/home/nedp/Desarrollo/Proyectos/Guia-Farmaceutica/assets/prescripcion/Prescripcion.xml'

def parse_posologia(posologia_text):
    """
    Extrae dosis recomendada (mg/kg), dosis máxima por toma (mg) y dosis máxima diaria (mg)
    desde la sección de posología en la monografía.
    """
    if not posologia_text:
        return None, None, None, None, 6, 100.0, 5.0, "mg/5ml"

    dosis_mg_kg = None
    dosis_max_toma = None
    dosis_max_dia = None
    dosis_max_peso_dia = None
    intervalo_horas = 6

    # Buscar mg/kg o mg/kg/día
    m_mg_kg = re.search(r'(\d+(?:[.,]\d+)?)\s*mg/kg', posologia_text, re.IGNORECASE)
    if m_mg_kg:
        try:
            dosis_mg_kg = float(m_mg_kg.group(1).replace(',', '.'))
        except ValueError:
            pass

    # Buscar dosis máxima diaria / día (ej: máx. 600 mg/día o dosis máx.: 2000 mg/día)
    m_max_dia = re.search(r'(?:máx|max|máxima|maxima)[^.\n]*?(\d+(?:[.,]\d+)?)\s*mg/(?:día|dia|24\s*h)', posologia_text, re.IGNORECASE)
    if m_max_dia:
        try:
            dosis_max_dia = float(m_max_dia.group(1).replace(',', '.'))
        except ValueError:
            pass

    # Buscar dosis máxima por toma o absoluta
    m_max_toma = re.search(r'(?:máx|max|máxima|maxima)[^.\n]*?(\d+(?:[.,]\d+)?)\s*mg(?!\s*/\s*(?:día|dia|kg))', posologia_text, re.IGNORECASE)
    if m_max_toma:
        try:
            dosis_max_toma = float(m_max_toma.group(1).replace(',', '.'))
        except ValueError:
            pass

    # Intervalos de tiempo (cada 6 horas, 8 horas, 12 horas, etc.)
    m_intervalo = re.search(r'cada\s*(\d+)\s*(?:h|horas|hrs)', posologia_text, re.IGNORECASE)
    if m_intervalo:
        try:
            intervalo_horas = int(m_intervalo.group(1))
        except ValueError:
            pass

    return dosis_mg_kg, dosis_max_toma, dosis_max_dia, dosis_max_peso_dia, intervalo_horas, 250.0, 5.0, "mg/5ml"

def main():
    print("Iniciando procesamiento de reglas de dosificación...")
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # Crear tabla de reglas de dosificación
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS reglas_dosificacion (
        atc_codigo TEXT PRIMARY KEY,
        titulo TEXT,
        dosis_recomendada_mg_kg REAL,
        dosis_max_toma_mg REAL,
        dosis_max_dia_mg REAL,
        dosis_max_peso_mg_kg_dia REAL,
        intervalo_min_horas INTEGER,
        concentracion_mg REAL,
        volumen_ml REAL,
        unidad TEXT
    );
    """)

    # Obtener todas las monografías
    cursor.execute("SELECT atc_codigo, titulo, posologia FROM monografias;")
    rows = cursor.fetchall()

    insertados = 0
    for atc_codigo, titulo, posologia in rows:
        d_mg_kg, d_max_toma, d_max_dia, d_max_peso_dia, intervalo, conc_mg, vol_ml, unidad = parse_posologia(posologia)

        # Valores por defecto razonables según fármacos comunes si no fueron extraídos del texto libre
        if "ibuprofeno" in titulo.lower() or "N02BE01" in atc_codigo or "paracetamol" in titulo.lower() or "acetaminofen" in titulo.lower():
            if not d_mg_kg: d_mg_kg = 10.0
            if not d_max_toma: d_max_toma = 500.0
            if not d_max_dia: d_max_dia = 2000.0
            conc_mg = 120.0
            vol_ml = 5.0

        elif "amoxicilina" in titulo.lower():
            if not d_mg_kg: d_mg_kg = 25.0
            if not d_max_toma: d_max_toma = 1000.0
            if not d_max_dia: d_max_dia = 3000.0
            conc_mg = 250.0
            vol_ml = 5.0

        cursor.execute("""
        INSERT OR REPLACE INTO reglas_dosificacion
        (atc_codigo, titulo, dosis_recomendada_mg_kg, dosis_max_toma_mg, dosis_max_dia_mg, dosis_max_peso_mg_kg_dia, intervalo_min_horas, concentracion_mg, volumen_ml, unidad)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """, (atc_codigo, titulo, d_mg_kg, d_max_toma, d_max_dia, d_max_peso_dia, intervalo, conc_mg, vol_ml, unidad))
        insertados += 1

    conn.commit()
    print(f"Procesamiento completado. {insertados} reglas de dosificación registradas en la BD.")
    conn.close()

if __name__ == '__main__':
    main()
