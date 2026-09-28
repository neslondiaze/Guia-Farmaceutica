package ve.guiafarmaceutica.app.util;

/**
 * Configuración central de Licenciamiento Pre-Autorizado por IMEI/ID de Dispositivo.
 * Para autorizar a un comprador específico, coloque aquí su IMEI o ID de dispositivo
 * antes de compilar y enviarle su APK personalizada.
 */
public class LicenseConfig {

    /**
     * Si es true, omite la validación de hardware (útil para desarrollo/pruebas).
     * Si es false, exige que la firma física del teléfono coincida con los IMEI/IDs autorizados.
     */
    public static final boolean MODO_DESARROLLO = false;

    /**
     * IMEI o ID de hardware del comprador autorizado para esta versión entregada.
     * Ejemplo: "86668412026", "DEMO-HARDWARE-ID", o lista separada por comas.
     */
    public static final String[] IMEIS_AUTORIZADOS = new String[]{
    /**    "86668412026",
        "0846525292003145",
        "1010018024020984",
        "emulator-5554",
        "DEMO-HARDWARE-ID", */
            "354037440957862"
        /**    "068D30D287E15460" */
    };
}
