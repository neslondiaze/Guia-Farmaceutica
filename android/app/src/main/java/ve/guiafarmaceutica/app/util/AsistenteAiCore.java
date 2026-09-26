package ve.guiafarmaceutica.app.util;

import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ve.guiafarmaceutica.app.data.FichaClinica;
import ve.guiafarmaceutica.app.repository.MedicamentoRepository;

public class AsistenteAiCore {

    public interface CallbackEvaluacionAi {
        void alCompletar(List<FichaClinica> medicamentosSugeridos, String diagnosticoResumen);
    }

    /**
     * Evalúa la Impresión Diagnóstica ingresada por el médico (RAG On-Device) con análisis semántico y reglas pediátricas.
     */
    public static void evaluarImpresionDiagnostica(Context context, String impresionDiagnostica, String edadPacienteStr, CallbackEvaluacionAi callback) {
        if (impresionDiagnostica == null || impresionDiagnostica.trim().isEmpty()) {
            callback.alCompletar(new ArrayList<>(), "Ingrese un cuadro clínico para evaluación.");
            return;
        }

        boolean isPediatric = parsearEdadEsPediatrica(edadPacienteStr);
        String texto = impresionDiagnostica.toLowerCase();
        MedicamentoRepository repo = new MedicamentoRepository(context);

        // Extraer palabras clave clínicas semánticas
        List<String> palabrasClave = extraerTerminosClave(texto);
        if (palabrasClave.isEmpty()) {
            palabrasClave.add(impresionDiagnostica.trim());
        }

        String terminoBusqueda = palabrasClave.get(0);

        repo.buscar(terminoBusqueda, 20, 0, datos -> {
            if (datos != null && !datos.isEmpty()) {
                List<FichaClinica> procesados = filtrarYOrdenarPorRelevanciaYEdad(datos, isPediatric);
                String mensaje = isPediatric 
                        ? "IA (Pediátrico < 12 años): Sugerencias priorizadas (jarabes/suspensiones/gotas) para: " + terminoBusqueda 
                        : "Sugerencias de referencia según catálogo local para: " + terminoBusqueda;
                callback.alCompletar(procesados, mensaje);
            } else {
                if (palabrasClave.size() > 1) {
                    repo.buscar(palabrasClave.get(1), 20, 0, datos2 -> {
                        if (datos2 != null && !datos2.isEmpty()) {
                            List<FichaClinica> procesados2 = filtrarYOrdenarPorRelevanciaYEdad(datos2, isPediatric);
                            callback.alCompletar(procesados2, "Sugerencias de referencia según catálogo local.");
                        } else {
                            callback.alCompletar(new ArrayList<>(), "No se encontraron coincidencias directas en la Guía Farmacéutica local. Favor revisar la guía de medicamentos.");
                        }
                    });
                } else {
                    callback.alCompletar(new ArrayList<>(), "No se encontraron coincidencias directas en la Guía Farmacéutica local. Favor revisar la guía de medicamentos.");
                }
            }
        });
    }

    private static boolean parsearEdadEsPediatrica(String edadStr) {
        if (edadStr == null || edadStr.trim().isEmpty()) {
            return false;
        }
        try {
            String limpio = edadStr.replaceAll("[^0-9]", "");
            if (!limpio.isEmpty()) {
                int edad = Integer.parseInt(limpio);
                if (edadStr.toLowerCase().contains("mes") || edad < 12) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static List<FichaClinica> filtrarYOrdenarPorRelevanciaYEdad(List<FichaClinica> lista, boolean isPediatric) {
        if (!isPediatric || lista == null) {
            return lista;
        }
        List<FichaClinica> pediatricos = new ArrayList<>();
        List<FichaClinica> otros = new ArrayList<>();

        for (FichaClinica f : lista) {
            String forma = f.forma != null ? f.forma.toLowerCase() : "";
            String nombre = f.nombre != null ? f.nombre.toLowerCase() : "";
            if (forma.contains("suspens") || forma.contains("jarabe") || forma.contains("gota") || forma.contains("soluci") || forma.contains("oral") ||
                nombre.contains("suspens") || nombre.contains("jarabe") || nombre.contains("gota") || nombre.contains("pediatric")) {
                pediatricos.add(f);
            } else {
                otros.add(f);
            }
        }
        pediatricos.addAll(otros);
        return pediatricos;
    }

    private static List<String> extraerTerminosClave(String texto) {
        List<String> terminos = new ArrayList<>();
        if (texto.contains("gastroenterit") || texto.contains("diarr") || texto.contains("deshidrat") || texto.contains("vomit") || texto.contains("vómit")) {
            terminos.add("suero");
            terminos.add("oral");
            terminos.add("racecadotrilo");
        }
        if (texto.contains("cefalea") || texto.contains("migrañ") || texto.contains("cabeza")) {
            terminos.add("acetaminofen");
            terminos.add("ibuprofeno");
        }
        if (texto.contains("amigdalit") || texto.contains("infecc") || texto.contains("bacteri")) {
            terminos.add("amoxicilina");
            terminos.add("ampicilina");
        }
        if (texto.contains("fiebre") || texto.contains("febril") || texto.contains("dolor")) {
            terminos.add("acetaminofen");
            terminos.add("ibuprofeno");
        }
        if (texto.contains("tos") || texto.contains("bronqui")) {
            terminos.add("ambroxol");
            terminos.add("jarabe");
        }
        if (texto.contains("alerg") || texto.contains("rinit")) {
            terminos.add("loratadina");
            terminos.add("cetirizina");
        }
        if (texto.contains("gastrit") || texto.contains("acidez") || texto.contains("reflujo")) {
            terminos.add("omeprazol");
            terminos.add("pantoprazol");
        }

        if (terminos.isEmpty()) {
            String[] palabras = texto.split("\\s+");
            for (String p : palabras) {
                if (p.length() > 3 && !Arrays.asList("para", "con", "que", "esta", "paciente", "presenta").contains(p)) {
                    terminos.add(p);
                }
            }
        }
        return terminos;
    }
}
