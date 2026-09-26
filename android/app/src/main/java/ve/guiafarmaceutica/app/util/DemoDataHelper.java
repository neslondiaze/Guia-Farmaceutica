package ve.guiafarmaceutica.app.util;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.data.ImpresionDiagnostica;
import ve.guiafarmaceutica.app.data.ImpresionDetalle;
import ve.guiafarmaceutica.app.data.Paciente;

public class DemoDataHelper {

    public static void asegurarDatosDemo(Context context) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.obtener(context);
                List<Paciente> lista = db.pacienteDao().listarPacientes();
                if (lista == null || lista.isEmpty()) {
                    // 1. Paciente 1: María López
                    Paciente p1 = new Paciente();
                    p1.nombre_completo = "María López";
                    p1.identificacion = "DEMO-10482";
                    p1.fecha_nacimiento = "06/12/1988";
                    p1.edad_calculada = "38 años";
                    p1.sexo = "FEMENINO";
                    p1.telefono = "+58 412 555 0126";
                    p1.correo = "maria.lopez@example.test";
                    p1.direccion = "Av. Principal de El Cafetal, Caracas";
                    p1.contacto_emergencia = "Ana López - +58 414 555 0127";
                    p1.alergias = "Penicilina (Alergia moderada)";
                    p1.condiciones_relevantes = "Hipertensión arterial controlada";
                    p1.grupo_sanguineo = "O+";
                    p1.peso = "62 kg";
                    p1.altura = "165 cm";
                    p1.notas_clinicas = "Paciente bajo tratamiento regular con IECA.";
                    long p1Id = db.pacienteDao().insertar(p1);

                    // 2. Paciente 2: Carlos Pérez
                    Paciente p2 = new Paciente();
                    p2.nombre_completo = "Carlos Pérez";
                    p2.identificacion = "DEMO-10483";
                    p2.fecha_nacimiento = "15/04/1975";
                    p2.edad_calculada = "51 años";
                    p2.sexo = "MASCULINO";
                    p2.telefono = "+58 416 555 0234";
                    p2.correo = "carlos.perez@example.test";
                    p2.direccion = "Urb. La Campiña, Caracas";
                    p2.contacto_emergencia = "Elena Pérez - +58 412 555 0235";
                    p2.alergias = "Ninguna conocida";
                    p2.condiciones_relevantes = "Diabetes Mellitus Tipo 2";
                    p2.grupo_sanguineo = "A+";
                    p2.peso = "78 kg";
                    p2.altura = "174 cm";
                    p2.notas_clinicas = "Control glucémico semestral.";
                    db.pacienteDao().insertar(p2);

                    // 3. Paciente 3: Ana Gómez
                    Paciente p3 = new Paciente();
                    p3.nombre_completo = "Ana Gómez";
                    p3.identificacion = "DEMO-10484";
                    p3.fecha_nacimiento = "22/09/1992";
                    p3.edad_calculada = "33 años";
                    p3.sexo = "FEMENINO";
                    p3.telefono = "+58 414 555 0345";
                    p3.correo = "ana.gomez@example.test";
                    p3.direccion = "Av. Las Delicias, Maracay";
                    p3.contacto_emergencia = "Pedro Gómez - +58 416 555 0346";
                    p3.alergias = "AINEs / Aspirina";
                    p3.condiciones_relevantes = "Asma bronquial leve";
                    p3.grupo_sanguineo = "B+";
                    p3.peso = "58 kg";
                    p3.altura = "160 cm";
                    p3.notas_clinicas = "Uso ocasional de broncodilatador inhalado.";
                    db.pacienteDao().insertar(p3);

                    // 4. Paciente 4: José Rodríguez
                    Paciente p4 = new Paciente();
                    p4.nombre_completo = "José Rodríguez";
                    p4.identificacion = "DEMO-10485";
                    p4.fecha_nacimiento = "03/11/1960";
                    p4.edad_calculada = "65 años";
                    p4.sexo = "MASCULINO";
                    p4.telefono = "+58 412 555 0456";
                    p4.correo = "jose.rodriguez@example.test";
                    p4.direccion = "Los Chaguaramos, Caracas";
                    p4.contacto_emergencia = "María Rodríguez - +58 414 555 0457";
                    p4.alergias = "Sulfas";
                    p4.condiciones_relevantes = "Dislipidemia mixta";
                    p4.grupo_sanguineo = "AB+";
                    p4.peso = "82 kg";
                    p4.altura = "170 cm";
                    p4.notas_clinicas = "Perfil lipídico en seguimiento.";
                    db.pacienteDao().insertar(p4);

                    // 5. Paciente 5: Carmen Silva
                    Paciente p5 = new Paciente();
                    p5.nombre_completo = "Carmen Silva";
                    p5.identificacion = "DEMO-10486";
                    p5.fecha_nacimiento = "18/02/2000";
                    p5.edad_calculada = "26 años";
                    p5.sexo = "FEMENINO";
                    p5.telefono = "+58 416 555 0567";
                    p5.correo = "carmen.silva@example.test";
                    p5.direccion = "Av. Bolívar, Valencia";
                    p5.contacto_emergencia = "Luis Silva - +58 412 555 0568";
                    p5.alergias = "Ninguna";
                    p5.condiciones_relevantes = "Sana / Sin antecedentes";
                    p5.grupo_sanguineo = "O-";
                    p5.peso = "55 kg";
                    p5.altura = "163 cm";
                    p5.notas_clinicas = "Control rutinario preventivo.";
                    db.pacienteDao().insertar(p5);

                    // Impresión Demostrativa para María López
                    ImpresionDiagnostica impresion = new ImpresionDiagnostica();
                    impresion.paciente_id = p1Id;
                    impresion.paciente_nombre = "María López";
                    impresion.paciente_cedula = "DEMO-10482";
                    impresion.paciente_edad = "38 años";
                    impresion.paciente_peso = "62 kg";
                    impresion.diagnostico = "Faringoamigdalitis aguda bacteriana";
                    impresion.fecha_vencimiento = "10/23/2026";
                    impresion.renovaciones = 0;
                    impresion.estado = "Emitido";
                    impresion.observaciones = "Caso clínico de demostración asistido por IA on-device.";
                    impresion.fecha_creacion = "16/09/2026";

                    long impresionId = db.impresionDao().insertarImpresion(impresion);

                    List<ImpresionDetalle> detalles = new ArrayList<>();
                    ImpresionDetalle d1 = new ImpresionDetalle();
                    d1.impresion_id = impresionId;
                    d1.medicamento_nombre = "Amoxicilina + Ácido Clavulánico 875/125 mg";
                    d1.presentacion = "Comprimidos recubiertos";
                    d1.concentracion = "875 mg / 125 mg";
                    d1.dosificacion = "1 comprimido vía oral cada 12 horas";
                    d1.via_administracion = "Oral";
                    d1.frecuencia_instrucciones = "Tomar junto a las comidas principales";
                    d1.duracion_dias = "7 días";
                    d1.cantidad_despachar = "1 caja (14 comprimidos)";
                    d1.instrucciones_paciente = "Cumplir horario estricto. Mantener hidratación adecuada.";
                    detalles.add(d1);

                    db.impresionDao().insertarDetalles(detalles);
                }
            } catch (Exception e) {
                // Silencioso para asegurar arranque limpio
            }
        }).start();
    }
}
