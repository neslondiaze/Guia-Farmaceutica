# Guía de Configuración y Llenado para la Google Play Console

Esta guía instructiva contiene las respuestas exactas y pasos requeridos para superar la revisión automatizada y humana de la **Google Play Console** para la APK **Guía Farmacéutica Venezuela**.

---

## 📋 1. Formulario de "Seguridad de los Datos" (Data Safety)

En la Play Console, ve a **Contenido de la app** > **Seguridad de los datos**.

### Paso 1: Preguntas Iniciales
1. **¿Tu app recopila o comparte alguno de los tipos de datos de usuario requeridos?**
   - Selecciona: **SÍ**
   - *Razón:* Aunque los datos se queden guardados localmente en SQLite/Room y no vayan a la nube, Google considera la escritura/procesamiento interno dentro del dispositivo como "Recopilación" (Collection).
2. **¿Todos los datos de usuario que recopila tu app se cifran en tránsito?**
   - Selecciona: **SÍ**
   - *Razón:* La app utiliza cifrado por hardware AES-256 GCM (`CryptoHelper`) con Android Keystore System.
3. **¿Proporcionas alguna forma para que los usuarios soliciten que se eliminen sus datos?**
   - Selecciona: **SÍ**
   - *Razón:* La pantalla de Ajustes incluye el botón *"Eliminar Todos Mis Datos del Dispositivo"*.

---

### Paso 2: Selección de Tipos de Datos a Declarar
Marca únicamente las siguientes casillas:
1. **Información personal:**
   - [x] **Nombre:** (Por el nombre del médico prescriptor y del paciente).
   - [x] **Dirección de correo electrónico:** (Por el correo de contacto del prescriptor).
   - [x] **Número de teléfono:** (Por el teléfono de contacto del prescriptor).
   - [x] **Identificadores personales:** (Por el documento de identidad / cédula del paciente).
2. **Salud y actividad física:**
   - [x] **Datos de salud:** (Por el peso, edad, diagnósticos y prescripciones clínicas del paciente).

*(Deja todas las demás categorías como Ubicación, Fotos o Finanzas desmarcadas).*

---

### Paso 3: Configuración Detallada de Cada Campo Seleccionado
Para **todos y cada uno** de los campos marcados (Nombre, Correo, Teléfono, Cédula y Datos de Salud):
- **¿Este dato se recopila, se comparte o ambas cosas?** -> Selecciona: **Recopilado** (Collected). *(NO selecciones "Compartido" / Shared).*
- **¿Este dato se procesa de forma efímera?** -> Selecciona: **NO** (permanece en la BD Room cifrada localmente hasta que el usuario decida borrarlo).
- **¿Este dato es obligatorio para tu app o los usuarios pueden elegir si se recopila?** -> Selecciona: **La recopilación de datos es obligatoria**.
- **¿Por qué se recopilan estos datos?** -> Selecciona únicamente: **Funcionalidad de la app** (App functionality).

---

## 🏥 2. Declaración de "Aplicaciones de Salud" (Health Apps)

En la Play Console, ve a **Política y programas** > **Contenido de la app** > **Aplicaciones de salud**.

1. Selecciona la categoría: **Herramientas de referencia para profesionales de la salud y soporte de decisiones clínicas**.
2. En el campo de texto explicativo sobre el origen y filiación de la app, escribe:
   > *"La aplicación ha sido desarrollada de forma independiente y paramétrica. El sistema está diseñado para que cada profesional de la salud configure de manera estrictamente local sus propias credenciales oficiales, incluyendo su nombre y su número de registro de licencia médica nacional (como el MPPS en el caso de usuarios en Venezuela), con el único fin de que estos datos se impriman automáticamente en el encabezado de los récipes médicos en formato PDF generados por la app."*

---

## 🔑 3. Sección "Acceso a Apps" (Instrucciones para el Revisor de Google)

En la Play Console, ve a **Contenido de la app** > **Acceso a apps**.

1. Selecciona: **Todas las funciones están disponibles sin restricciones de acceso** (o si agregas un PIN, selecciona *"Alguna o todas las funciones están restringidas"* y proporciona credenciales de prueba).
2. En Notas adicionales escribe:
   > *"Para probar la calculadora de dosis y la generación de récipes médicos en PDF, puede configurar el perfil del prescriptor utilizando datos de prueba como: Nombre: Dr. Revisor Prueba / Licencia MPPS: 12345."*
