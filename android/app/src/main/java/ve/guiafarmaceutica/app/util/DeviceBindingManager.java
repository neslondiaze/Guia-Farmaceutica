package ve.guiafarmaceutica.app.util;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import java.text.Normalizer;
import java.util.Locale;

public class DeviceBindingManager {

    /**
     * Obtiene la firma física única del teléfono actual basada en ANDROID_ID y modelo de hardware.
     */
    public static String obtenerFirmaDispositivo(Context context) {
        String androidId = "";
        try {
            androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception ignored) {}

        if (androidId == null || androidId.isEmpty()) {
            androidId = "DEFAULT_DEVICE_ID";
        }

        String model = Build.MODEL != null ? Build.MODEL : "";
        String brand = Build.BRAND != null ? Build.BRAND : "";

        return (androidId + "_" + brand + "_" + model).toUpperCase(Locale.getDefault());
    }

    /**
     * Obtiene un identificador corto del dispositivo (ANDROID_ID o Modelo).
     */
    public static String obtenerImeiOIdentificador(Context context) {
        try {
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId != null && !androidId.isEmpty()) {
                return androidId.toUpperCase(Locale.getDefault());
            }
        } catch (Exception ignored) {}
        return "ID-" + Build.MODEL.toUpperCase(Locale.getDefault());
    }

    /**
     * Comprueba si el teléfono actual coincide con los IMEI/IDs pre-autorizados en LicenseConfig.
     */
    public static boolean esDispositivoAutorizado(Context context) {
        if (LicenseConfig.MODO_DESARROLLO) {
            return true;
        }

        String firmaActual = obtenerFirmaDispositivo(context);
        String imeiActual = obtenerImeiOIdentificador(context);

        if (LicenseConfig.IMEIS_AUTORIZADOS == null || LicenseConfig.IMEIS_AUTORIZADOS.length == 0) {
            return true;
        }

        for (String autorizado : LicenseConfig.IMEIS_AUTORIZADOS) {
            if (autorizado == null || autorizado.trim().isEmpty()) continue;
            String autLimpio = normalizar(autorizado);
            if (normalizar(firmaActual).contains(autLimpio) || normalizar(imeiActual).contains(autLimpio) || autLimpio.contains(normalizar(imeiActual))) {
                return true;
            }
        }

        return false;
    }

    private static String normalizar(String text) {
        if (text == null) return "";
        String nfd = Normalizer.normalize(text.toLowerCase(Locale.getDefault()), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").trim();
    }
}
