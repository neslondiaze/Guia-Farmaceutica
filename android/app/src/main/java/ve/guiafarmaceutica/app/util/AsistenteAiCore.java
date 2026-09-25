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
     * Evalúa la Impresión Diagnóstica ingresada por el médico (RAG On-Device).
     */
    public static void evaluarImpresionDiagnostica(Context context, String impresionDiagnostica, CallbackEvaluacionAi callback) {
        if (impresionDiagnostica == null || impresionDiagnostica.trim().isEmpty()) {
            callback.alCompletar(new ArrayList<>(), "Ingrese un cuadro clínico para evaluación.");
            return;
        }

        String texto = impresionDiagnostica.toLowerCase();
        MedicamentoRepository repo = new MedicamentoRepository(context);

        // Extraer palabras clave clínicas
        List<String> palabrasClave = extraerTerminosClave(texto);
        if (palabrasClave.isEmpty()) {
            palabrasClave.add(impresionDiagnostica.trim());
        }

        String terminoBusqueda = palabrasClave.get(0);

        repo.buscar(terminoBusqueda, 10, 0, datos -> {
            if (datos != null && !datos.isEmpty()) {
                callback.alCompletar(datos, "Sugerencias de referencia según catálogo local para: " + terminoBusqueda);
            } else {
                // Probar con segundo término si existe
                if (palabrasClave.size() > 1) {
                    repo.buscar(palabrasClave.get(1), 10, 0, datos2 -> {
                        callback.alCompletar(datos2, "Sugerencias de referencia según catálogo local.");
                    });
                } else {
                    callback.alCompletar(new ArrayList<>(), "No se encontraron coincidencias directas en la Guía Farmacéutica local.");
                }
            }
        });
    }

    private static List<String> extraerTerminosClave(String texto) {
        List<String> terminos = new ArrayList<>();
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
