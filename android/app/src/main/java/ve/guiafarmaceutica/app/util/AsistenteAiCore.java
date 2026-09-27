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

        buscarRecursoClave(repo, palabrasClave, 0, isPediatric, impresionDiagnostica.trim(), callback);
    }

    private static void buscarRecursoClave(MedicamentoRepository repo, List<String> palabrasClave, int index, boolean isPediatric, String diagnosticoOriginal, CallbackEvaluacionAi callback) {
        if (index >= palabrasClave.size()) {
            callback.alCompletar(new ArrayList<>(), "No se encontraron coincidencias directas en la Guía Farmacéutica local para: " + diagnosticoOriginal + ". Favor revisar la guía de medicamentos.");
            return;
        }

        String termino = palabrasClave.get(index);
        repo.buscar(termino, 20, 0, datos -> {
            if (datos != null && !datos.isEmpty()) {
                List<FichaClinica> procesados = filtrarYOrdenarPorRelevanciaYEdad(datos, isPediatric);
                String mensaje = isPediatric 
                        ? "IA (Pediátrico < 12 años): Sugerencias priorizadas (jarabes/suspensiones/gotas) para " + diagnosticoOriginal
                        : "Sugerencias de referencia según catálogo local para " + diagnosticoOriginal + " (" + termino + ")";
                callback.alCompletar(procesados, mensaje);
            } else {
                buscarRecursoClave(repo, palabrasClave, index + 1, isPediatric, diagnosticoOriginal, callback);
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

        if (texto.contains("diabet") || texto.contains("insulin") || texto.contains("gluc")) {
            terminos.add("insulina");
            terminos.add("metformina");
            terminos.add("glibenclamida");
            terminos.add("sitagliptina");
        }
        if (texto.contains("hipertens") || texto.contains("presion") || texto.contains("tension") || texto.contains("arterial")) {
            terminos.add("losartan");
            terminos.add("enalapril");
            terminos.add("amlodipina");
            terminos.add("valsartan");
        }
        if (texto.contains("gastroenterit") || texto.contains("diarr") || texto.contains("deshidrat") || texto.contains("vomit") || texto.contains("vómit")) {
            terminos.add("suero");
            terminos.add("racecadotrilo");
            terminos.add("enterogermina");
        }
        if (texto.contains("cefalea") || texto.contains("migrañ") || texto.contains("cabeza")) {
            terminos.add("acetaminofen");
            terminos.add("ibuprofeno");
            terminos.add("ketoprofeno");
        }
        if (texto.contains("amigdalit") || texto.contains("infecc") || texto.contains("bacteri") || texto.contains("neumon")) {
            terminos.add("amoxicilina");
            terminos.add("azitromicina");
            terminos.add("ampicilina");
        }
        if (texto.contains("fiebre") || texto.contains("febril") || texto.contains("dolor")) {
            terminos.add("acetaminofen");
            terminos.add("ibuprofeno");
            terminos.add("diclofenac");
        }
        if (texto.contains("tos") || texto.contains("bronqui") || texto.contains("asma")) {
            terminos.add("salbutamol");
            terminos.add("ambroxol");
            terminos.add("budesonida");
        }
        if (texto.contains("alerg") || texto.contains("rinit") || texto.contains("urtic")) {
            terminos.add("loratadina");
            terminos.add("cetirizina");
            terminos.add("desloratadina");
        }
        if (texto.contains("gastrit") || texto.contains("acidez") || texto.contains("reflujo") || texto.contains("ulcer")) {
            terminos.add("omeprazol");
            terminos.add("pantoprazol");
            terminos.add("esomeprazol");
        }
        if (texto.contains("urinari") || texto.contains("itu") || texto.contains("cistiti")) {
            terminos.add("ciprofloxacina");
            terminos.add("nitrofurantoina");
        }
        if (texto.contains("mico") || texto.contains("hongo") || texto.contains("candida") || texto.contains("fungi")) {
            terminos.add("fluconazol");
            terminos.add("ketoconazol");
        }
        if (texto.contains("lipida") || texto.contains("colesterol") || texto.contains("triglic")) {
            terminos.add("atorvastatina");
            terminos.add("rosuvastatina");
        }

        if (terminos.isEmpty()) {
            String[] palabras = texto.split("\\s+");
            for (String p : palabras) {
                if (p.length() > 3 && !Arrays.asList("para", "con", "que", "esta", "paciente", "presenta", "tipo").contains(p)) {
                    terminos.add(p);
                }
            }
        }
        return terminos;
    }
}
