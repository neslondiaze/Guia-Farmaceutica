# Clasificación de los medicamentos de la base de datos

Guía de los ejes de clasificación disponibles en `medicamentos_vademecum_venezuela.db`
(7057 medicamentos, 7057 fichas clínicas leídas, 1270 monografías).

La vista `v_clinico` ya reúne medicamento + laboratorio + ATC + vía + forma +
banderas de embarazo/lactancia en una sola consulta.

## 1. Clase anatómica ATC (nivel 1) — 14 clases, cobertura 100 %

| Grupo | Medicamentos | Descripción |
|---|---|---|
| J | 1040 | Antiinfecciosos para uso sistémico |
| A | 1039 | Tracto alimentario y metabolismo |
| C | 985 | Sistema cardiovascular |
| N | 827 | Sistema nervioso |
| R | 754 | Sistema respiratorio |
| M | 551 | Sistema musculoesquelético |
| D | 493 | Dermatológicos |
| G | 308 | Sistema genitourinario y hormonas sexuales |
| B | 257 | Sangre y órganos hematopoyéticos |
| L | 234 | Antineoplásicos e inmunomoduladores |
| S | 210 | Órganos de los sentidos |
| P | 167 | Antiparasitarios, insecticidas y repelentes |
| H | 138 | Preparados hormonales sistémicos |
| V | 54 | Varios |

```sql
SELECT substr(atc_codigo, 1, 1) AS clase, COUNT(*)
FROM medicamento_clinico
GROUP BY 1 ORDER BY 2 DESC;
```

## 2. Grupo terapéutico ATC (nivel 2, 3 letras)

Los más poblados:

| Grupo | Medicamentos | Descripción |
|---|---|---|
| J01 | 790 | Antibacterianos para uso sistémico |
| M01 | 396 | Antiinflamatorios y antirreumáticos |
| C09 | 349 | Agentes activos sobre el sistema renina-angiotensina |
| A11 | 257 | Vitaminas |
| R06 | 247 | Antihistamínicos para uso sistémico |
| N02 | 237 | Analgésicos |
| A02 | 229 | Agentes contra alteraciones causadas por ácidos |
| N05 | 226 | Psicolépticos |
| C10 | 203 | Agentes modificadores de los lípidos |
| S01 | 198 | Oftalmológicos |
| R03 | 196 | Agentes contra padecimientos obstructivos respiratorios |
| R05 | 194 | Preparados para la tos y el resfriado |
| N06 | 166 | Psicoanalépticos |
| G03 | 153 | Hormonas sexuales y moduladores genitales |
| A10 | 147 | Antidiabéticos |
| C07 | 137 | Betabloqueantes |
| L01 | 136 | Antineoplásicos |
| C08 | 108 | Bloqueantes del canal de calcio |
| D07 | 102 | Dermatológicos con corticosteroides |

La tabla `atc` guarda la jerarquía completa (5 niveles), navegable por `padre`:
`M → M01 → M01A → M01AE → M01AE01` (Ibuprofeno).

```sql
-- Todos los medicamentos de un grupo terapéutico y sus subgrupos
SELECT v.nombre, v.laboratorio, v.atc_codigo, v.atc_descripcion
FROM v_clinico v
WHERE v.atc_codigo LIKE 'C09%';
```

## 3. Principio activo (ATC terminal) — 1270 distintos

Los más frecuentes:

| ATC | Principio activo | Medicamentos |
|---|---|---|
| N02BE01 | Paracetamol | 95 |
| M01AB05 | Diclofenaco | 93 |
| M01AE01 | Ibuprofeno | 81 |
| R06AB54 | Clorfeniramina, asociaciones | 77 |
| C09AA02 | Enalapril | 66 |
| C10AA01 | Simvastatina | 62 |
| R05CB06 | Ambroxol | 60 |
| J01CA01 | Ampicilina | 58 |
| J01MA02 | Ciprofloxacino | 57 |
| J01CA04 | Amoxicilina | 53 |
| R06AX13 | Loratadina | 52 |
| J01FA10 | Azitromicina | 47 |
| C08CA01 | Amlodipino | 45 |

Ideal para agrupar genéricos homólogos (misma molécula, distintos laboratorios):

```sql
SELECT nombre, laboratorio FROM v_clinico WHERE atc_codigo = 'N05BA12'; -- alprazolam
```

## 4. Vía de administración — 48 valores

| Vía | Medicamentos |
|---|---|
| Vía oral | 4791 |
| Uso cutáneo | 511 |
| Vía intravenosa | 448 |
| Vía intramuscular (+ variante con coma final) | 440 + 137 |
| Vía óftalmica | 190 |
| Vía inhalatoria | 100 |
| Vía subcutanea | 100 |
| Vía nasal | 66 |
| Vía vaginal | 65 |
| Vía rectal | 30 |
| Vía transdérmica | 22 |
| Vía bucal | 14 |

Más vías raras: intraarticular, epidural, intratecal, intravítrea, bucofaríngea.

> Nota de calidad: el sitio trae ~8 variantes duplicadas por comas finales
> ("Vía intramuscular," vs "Vía intramuscular"). Normalizables con un `UPDATE`.

## 5. Forma farmacéutica — 107 formas

| Forma | Medicamentos |
|---|---|
| Comprimido | 1468 |
| Comprimido recubierto con película | 963 |
| Solución inyectable | 567 |
| Cápsula dura | 458 |
| Jarabe | 357 |
| Cápsula blanda | 289 |
| Comprimido recubierto | 222 |
| Polvo para solución inyectable | 215 |
| Crema | 178 |
| Suspensión oral | 156 |
| Polvo para suspensión oral | 154 |
| Colirio en solución | 144 |
| Comprimido de liberación prolongada | 130 |
| Gotas orales en solución | 129 |

## 6. Laboratorio titular — 136

Eje original de la carga base. Útil cruzado con ATC:

```sql
-- ¿Cuántos antibióticos sistémicos tiene Calox?
SELECT COUNT(*) FROM v_clinico
WHERE laboratorio = 'Calox' AND atc_codigo LIKE 'J01%';
```

## 7. Seguridad en embarazo y lactancia

Embarazo:

| Valor | Medicamentos |
|---|---|
| Evaluar riesgo/beneficio | 2727 |
| Contraindicado | 2399 |
| Precaución | 491 |
| Compatible | 193 |
| Sin dato | 1247 |

Lactancia:

| Valor | Medicamentos |
|---|---|
| evitar | 4186 |
| precaución | 1361 |
| compatible | 294 |
| Sin dato | 1216 |

```sql
SELECT nombre, laboratorio FROM v_clinico
WHERE embarazo = 'Contraindicado' AND via = 'Vía oral';
```

## 8. Texto clínico (15 secciones, con FTS5)

Cada una de las 1270 monografías (`monografias`) trae: mecanismo de acción,
indicaciones terapéuticas, indicaciones+posología, posología (900 monografías),
modo de administración, contraindicaciones, advertencias y precauciones,
insuficiencia hepática, insuficiencia renal, interacciones (965), embarazo,
lactancia, efectos sobre la conducción, reacciones adversas y
sobredosificación. Buscable por texto completo:

```sql
SELECT atc_codigo, titulo
FROM monografias_fts
WHERE monografias_fts MATCH 'hemorragia';
```

## 9. Navegación alfabética (carga base)

27 letras y 170 prefijos (`letra`, `prefijo`). Útil para índices A–Z;
no es clasificación farmacológica.

---

**Resumen:** los ejes farmacológicos reales son **ATC (5 niveles) +
principio activo + vía + forma + embarazo/lactancia**, todo consultable
vía la vista `v_clinico`.
