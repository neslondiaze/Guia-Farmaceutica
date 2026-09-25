package ve.guiafarmaceutica.app.util;

public class DosificacionHelper {

    /**
     * Determina si un medicamento admite cálculo cuantitativo de dosificación (mg/kg, IV, etc.)
     * según su vía de administración y forma farmacéutica.
     * Retorna false para formas/vías tópicas u oftálmicas/óticas locales donde no aplica la calculadora.
     */
    public static boolean esDosificable(String via, String forma, String nombre) {
        String viaLower = via != null ? via.toLowerCase() : "";
        String formaLower = forma != null ? forma.toLowerCase() : "";
        String nombreLower = nombre != null ? nombre.toLowerCase() : "";
        String combinado = viaLower + " " + formaLower + " " + nombreLower;

        // Vías y formas farmacéuticas tópicas o dermatológicas locales
        if (combinado.contains("tópica") || combinado.contains("topica") ||
            combinado.contains("oftálmica") || combinado.contains("oftalmica") ||
            combinado.contains("ótica") || combinado.contains("otica") ||
            combinado.contains("nasal") || combinado.contains("cutánea") || combinado.contains("cutanea") ||
            combinado.contains("crema") || combinado.contains("pomada") ||
            combinado.contains("ungüento") || combinado.contains("unguento") ||
            combinado.contains("colirio") || combinado.contains("champú") ||
            combinado.contains("champu") || combinado.contains("loción") ||
            combinado.contains("locion") || combinado.contains("espuma") ||
            combinado.contains("gel tópico") || combinado.contains("gel dermatológico") ||
            combinado.contains("pasta dental") || combinado.contains("parche")) {
            return false;
        }

        return true;
    }

    public static boolean esDosificable(String via, String forma) {
        return esDosificable(via, forma, "");
    }
}
