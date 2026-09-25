package ve.guiafarmaceutica.app.util;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.data.ImpresionDiagnostica;
import ve.guiafarmaceutica.app.data.ImpresionDetalle;
import ve.guiafarmaceutica.app.data.IndicacionNoFarmacologica;
import ve.guiafarmaceutica.app.data.Paciente;

public class DemoDataHelper {

    public static void asegurarDatosDemo(Context context) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.obtener(context);
                List<Paciente> lista = db.pacienteDao().listarPacientes();
                if (lista == null || lista.isEmpty()) {
                    // 1. Crear Paciente Demo María López
                    Paciente maria = new Paciente();
                    maria.nombre_completo = "María López";
                    maria.identificacion = "DEMO-10482";
                    maria.fecha_nacimiento = "06/12/1988";
                    maria.edad_calculada = "38 años";
                    maria.sexo = "Femenino";
                    maria.telefono = "+00 555 010 2026";
                    maria.correo = "maria.lopez.demo@example.test";
                    maria.direccion = "Av. Demostración 123";
                    maria.contacto_emergencia = "Ana López - +00 555 010 2027";
                    maria.alergias = "Alergia ficticia registrada (Penicilina / AINEs)";
                    maria.condiciones_relevantes = "Antecedente ficticio";
                    maria.grupo_sanguineo = "O+";
                    maria.peso = "62 kg";
                    maria.altura = "165 cm";
                    maria.notas_clinicas = "Notas ficticias para demostración. No usar en atención real.";

                    long mariaId = db.pacienteDao().insertar(maria);

                    // 2. Crear Impresión Diagnóstica Demostrativa (Emitida)
                    ImpresionDiagnostica impresion = new ImpresionDiagnostica();
                    impresion.paciente_id = mariaId;
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

                    // 3. Crear Indicación Demostrativa (Borrador)
                    IndicacionNoFarmacologica indicacion = new IndicacionNoFarmacologica();
                    indicacion.paciente_id = mariaId;
                    indicacion.paciente_nombre = "María López";
                    indicacion.actividad_reposo = "Texto demostrativo de reposo activo de 8 horas.";
                    indicacion.alimentacion = "Dieta blanda hipotónica ficticia.";
                    indicacion.hidratacion = "Abundante agua oral 2L/día.";
                    indicacion.cuidados_generales = "Cuidados demostrativos ficticios.";
                    indicacion.estudios_solicitados = "Estudio ficticio de laboratorio de rutina.";
                    indicacion.senales_alarma = "Texto demostrativo, sin orientación clínica real.";
                    indicacion.fecha_seguimiento = "10/05/2026";
                    indicacion.notas_adicionales = "Notas ficticias de control.";
                    indicacion.estado = "Borrador";
                    indicacion.fecha_creacion = "05/09/2026";

                    db.indicacionDao().insertar(indicacion);
                }
            } catch (Exception e) {
                // Silencioso para asegurar arranque limpio
            }
        }).start();
    }
}
