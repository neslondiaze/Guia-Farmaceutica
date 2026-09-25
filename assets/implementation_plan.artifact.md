# Plan de Refactorización: Módulo de Impresión Diagnóstica e Integración con IA On-Device (Gemini Nano / Google AICore)

Este plan establece la **eliminación completa del concepto de "Récipe Médico"** (sustituyéndolo por **"Impresión Diagnóstica"** en el código Java, Room, layouts y UI) y la **integración de la Inteligencia Artificial nativa de Android (Google AICore / Gemini Nano / Function Calling RAG)** según las especificaciones del documento oficial `Impresión Diagnostica.odt`.

---

## 🏛️ Arquitectura de la Solución e Integración con IA On-Device

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                   MÓDULO DE IMPRESIÓN DIAGNÓSTICA (UI)                      │
│  El médico ingresa la Impresión Diagnóstica en texto libre                    │
│  Ej: "Paciente masculino de 5 años con amigdalitis aguda febril de 38.5°C" │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
                                      ▼
             ┌──────────────────────────────────────────────────┐
             │  IA NATIVA ON-DEVICE (Google AICore / Gemini)    │
             │  Procesamiento 100% Local, Privado y Offline     │
             │  Extrae palabras clave y síntomas clínicos       │
             │  Ej: ["fiebre", "amigdalitis", "infeccion"]      │
             └────────────────────────┬─────────────────────────┘
                                      │
                                      ▼
             ┌──────────────────────────────────────────────────┐
             │ BÚSQUEDA Y MAPEO FTS5 EN GUÍA FARMACÉUTICA BD    │
             │ Consulta local FTS5 en `guia_farmaceutica.db`    │
             │ Filtra medicamentos comerciales de referencia    │
             │ calcula dosificaciones pediátricas / adulto      │
             └────────────────────────┬─────────────────────────┘
                                      │
                                      ▼
             ┌──────────────────────────────────────────────────┐
             │ FICHA Y PDF DE IMPRESIÓN DIAGNÓSTICA             │
             │ Presenta el informe estructurado con             │
             │ los medicamentos de referencia asociados.        │
             │ Genera PDF oficial de la Impresión Diagnóstica. │
             └──────────────────────────────────────────────────┘
```

---

## 🛠️ Proposed Changes

### 1. Refactorización Completa: Eliminación de "Récipe Médico" -> "Impresión Diagnóstica"

#### [MODIFY] [MainActivity.java](file:///home/nedp/Desarrollo/Proyectos/Guia-Farmaceutica/android/app/src/main/java/ve/guiafarmaceutica/app/ui/MainActivity.java)
- Cambiar el nombre de la tarjeta en la grilla principal de `"Récipes Médicos"` a **`"Impresión Diagnóstica"`**.

#### [MODIFY] Entidades, DAOs y Base de Datos Room (`data/`)
- Borrar todo lo referente a "Recipe" y renombrar las clases y tablas:
  - `Recipe.java` -> `ImpresionDiagnostica.java` (tabla `impresiones_diagnosticas`).
  - `RecipeDetalle.java` -> `ImpresionDetalle.java` (tabla `impresion_detalles`).
  - `RecipeDao.java` -> `ImpresionDao.java`.
  - Actualizar `AppDatabase.java` a versión 4 para registrar las entidades renombradas.

#### [MODIFY] Pantallas y Layouts (`ui/` y `res/layout/`)
- Renombrar actividades y layouts eliminando la palabra "Recipe":
  - `CrearRecipeActivity.java` -> `CrearImpresionActivity.java` (`activity_crear_impresion.xml`).
  - `HistorialRecipesActivity.java` -> `HistorialImpresionesActivity.java` (`activity_historial_impresiones.xml`).
  - `RecipePdfHelper.java` -> `ImpresionPdfHelper.java`.
  - `item_recipe.xml` -> `item_impresion.xml`.
  - `item_recipe_prescripcion.xml` -> `item_impresion_fármaco.xml`.
- Actualizar textos en la interfaz: sustituir "Emisión de Récipe Médico" por **"Evaluación e Impresión Diagnóstica"** y "Medicamentos Prescritos" por **"Medicamentos de Referencia Diagnóstica"**.

---

### 2. Módulo de IA On-Device (Google AICore / Gemini Nano RAG) (`util/` y `ai/`)

#### [NEW] [AsistenteAiCore.java](file:///home/nedp/Desarrollo/Proyectos/Guia-Farmaceutica/android/app/src/main/java/ve/guiafarmaceutica/app/ai/AsistenteAiCore.java)
- Implementar la arquitectura definida en `Impresión Diagnostica.odt`:
  - **Fase A (Extracción de Síntomas/Palabras Clave):** Invocación al modelo nativo `gemini-nano` vía `com.google.ai.edge.aicore` para extraer síntomas clave de la impresión diagnóstica.
  - **Fase B (Mapeo FTS5 con la Guía Farmacéutica local):** Consulta FTS5 en `guia_farmaceutica.db` usando la sintaxis `MATCH` con los términos extraídos.
  - **Fase C (Ensamblado del Reporte de Referencia):** Gemini Nano redacta la sugerencia médica de referencia cruzando la impresión con los medicamentos reales autorizados en Venezuela.
  - **Fallback Inteligente:** Si el hardware del teléfono no soporta AICore/Gemini Nano, el asistente conmuta automáticamente a una búsqueda semántica FTS5 directa contra las monografías oficiales.

#### [MODIFY] [CrearImpresionActivity.java](file:///home/nedp/Desarrollo/Proyectos/Guia-Farmaceutica/android/app/src/main/java/ve/guiafarmaceutica/app/ui/CrearImpresionActivity.java) y [activity_crear_impresion.xml](file:///home/nedp/Desarrollo/Proyectos/Guia-Farmaceutica/android/app/src/main/res/layout/activity_crear_impresion.xml)
- Añadir el botón **"✨ Evaluar Diagnóstico con IA y Sugerir Medicamentos"** al lado del campo de Impresión Diagnóstica.
- Al pulsarlo, invoca `AsistenteAiCore`, extrae las opciones de referencia del catálogo local y las añade al listado del informe diagnóstico.
- Incluir la etiqueta de seguridad obligatoria: *"Sugerencia generada por IA en base al catálogo local. Requiere verificación del profesional de la salud"*.

---

## 🧪 Plan de Verificación

### Automated Tests
- Compilar con `gradle_build("app:assembleDebug")` para asegurar cero errores de compilación tras la refactorización de nombres y entidades.

### Manual Verification
1. Abrir la app y verificar que en la cuadrícula principal la tarjeta se llama **"Impresión Diagnóstica"** y que no queda ninguna mención a "Récipe Médico".
2. Abrir **Impresión Diagnóstica** e ingresar una impresión clínica (ej: *"Paciente con Amigdalitis Aguda y fiebre alta"*).
3. Pulsar **"✨ Evaluar Diagnóstico con IA y Sugerir Medicamentos"**:
   - Verificar que la IA extrae los síntomas y busca en la base de datos local los medicamentos de referencia disponibles.
4. Generar el PDF de **Ficha de Impresión Diagnóstica** y comprobar que se imprime con el membrete institucional y la tabla de medicamentos de referencia.
