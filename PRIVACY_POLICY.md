# Política de Privacidad de Guía Farmacéutica Venezuela

**Última actualización:** 22 de septiembre de 2026.

Esta Política de Privacidad describe cómo **Guía Farmacéutica Venezuela** (en adelante, "la Aplicación"), como herramienta independiente de soporte clínico para profesionales de la salud, recopila, utiliza, almacena y protege la información al utilizar la aplicación móvil.

---

## 1. Descargo de Responsabilidad Médico (Medical Disclaimer)

**Guía Farmacéutica Venezuela** es una herramienta de soporte de decisiones e información clínica orientada a profesionales sanitarios (médicos, farmacéuticos y enfermeros).

- **No es un dispositivo médico regulado** ni sustituye, reemplaza o complementa el juicio clínico, diagnóstico o tratamiento de un profesional de la salud.
- Las funciones de cálculo matemático de dosis farmacológicas, goteo e infusión intravenosa y superficie corporal (BSA) se basan en literatura médica paramétrica pública (AEMPS/Vademécum).
- El usuario profesional es el único responsable de verificar de forma independiente la exactitud de los cálculos antes de cualquier prescripción o administración médica.

---

## 2. Información que la Aplicación Procesa

Para ofrecer las funciones de historial de récipes (recetas), cálculo de dosis y perfiles de membrete, la Aplicación procesa localmente los siguientes datos:

1. **Datos del Profesional Prescriptor:**
   - Nombre y apellido, especialidad, identificación profesional/licencia médica (como número MPPS / Colegiado), teléfono corporativo y dirección de correo electrónico.
2. **Datos Sensibles del Paciente (PHI):**
   - Nombre y apellido, documento de identidad (cédula), peso corporal, edad, diagnóstico clínico y observaciones médicas relacionadas con la prescripción.

---

## 3. Almacenamiento 100% Local y Protección Criptográfica

La privacidad y confidencialidad de sus datos es nuestra máxima prioridad:

- **Almacenamiento 100% Local (Sin Servidores Remotos):** Todos los datos personales y médicos introducidos se almacenan exclusivamente en el almacenamiento interno privado del dispositivo del usuario. El Desarrollador **no cuenta con servidores externos, bases de datos remotas ni servicios en la nube**.
- **Cifrado Criptográfico por Hardware (AES-256 GCM):** Los datos identificativos y clínicos del paciente se encriptan utilizando el sistema de seguridad por hardware **Android Keystore System** con algoritmos avanzados **AES-256 en modo GCM**. Las claves criptográficas quedan aisladas en el hardware seguro del teléfono, haciéndolas inaccesibles para el sistema operativo u otras aplicaciones.
- **Inhabilitación de Copias de Seguridad Inseguras:** La Aplicación tiene desactivadas de forma explícita las copias de seguridad del sistema operativo (`android:allowBackup="false"`). Esto impide que la base de datos con información sensible sea extraída mediante comandos ADB o respaldos automáticos en la nube sin cifrar.
- **Destrucción de Archivos Temporales:** Los archivos PDF autogenerados de los récipes médicos se guardan en un directorio de caché privado y efímero para ser compartidos o impresos, pudiendo ser eliminados del almacenamiento.

---

## 4. Retención y Eliminación de los Datos (Derechos del Usuario)

Dado que los datos residen únicamente dentro del teléfono del usuario, el control total le pertenece al usuario:

- **Eliminación desde la Aplicación:** El usuario puede eliminar individualmente cualquier récipe o borrar permanentemente su perfil y todo el historial de prescripciones mediante el botón **"Eliminar Todos Mis Datos del Dispositivo"** en la pantalla de Ajustes.
- **Eliminación Absoluta:** Si desinstala la Aplicación o borra los datos de almacenamiento desde los ajustes del sistema Android, toda la base de datos cifrada y los historiales médicos se destruirán de forma definitiva e irrecuperable.

---

## 5. Cumplimiento de las Políticas de Google Play

De acuerdo con las normativas sobre Aplicaciones de Salud de Google Play, esta Aplicación:

- No utiliza los datos médicos o de perfilado con fines publicitarios, de marketing ni comerciales.
- No recopila identificadores de hardware persistentes (como el IMEI del teléfono).
- Garantiza el principio de minimización de datos al procesar únicamente las variables estrictamente necesarias para ejecutar las fórmulas matemáticas de dosificación y membrete.

---

## 6. Contacto

Si tiene preguntas o inquietudes sobre las prácticas de privacidad y seguridad local de esta Aplicación, puede ponerse en contacto con el Desarrollador independiente a través de:

- **Correo electrónico:** `contacto@guiafarmaceutica.ve`
