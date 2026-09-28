package ve.guiafarmaceutica.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public class SecurityManager {

    /**
     * Llave Pública RSA (X.509 en formato Base64) generada por el administrador.
     */
    public static final String PUBLIC_KEY_BASE64 = 
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsHXBxgXnuPrLSb+A8oHi3B+kFyCigL3YEdaoId2z73VNmBCdEPywr0k7jXRqilHmCsMVtXybrJ/4XRp6b2QIqxLNNjCwMMbyPi4l5dybbIHOASO6zr8ABsoiakSaXPveapDFi7SQysWjfC0jkWgBl9nz7kpAyPXwikwiG7giA+76s84D7FFOIP9GEPJwtZUoFGKkuVSxZpPuwQ8LiEWdey9grIT4h+CyBABx3t0uY1KxxohrvB3vO2Jw3ui3QN8KDm6Oa3x0t/FUJWmQFv5ScbjwBsQGdFq82+EOfTJesICYB3xBwFohQdOMPG5ktiVI0xyf8GhC9fRlDDoJEIFPIQIDAQAB";

    /**
     * Si es true, omite la validación criptográfica (útil para desarrollo/pruebas).
     */
    public static final boolean MODO_DESARROLLO = false;

    public static boolean verificarLicenciaOffline(String licenciaIngresadaBase64, Context context) {
        if (MODO_DESARROLLO) {
            return true;
        }
        if (licenciaIngresadaBase64 == null || licenciaIngresadaBase64.trim().isEmpty()) {
            return false;
        }
        try {
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId == null || androidId.isEmpty()) {
                androidId = "DEFAULT_DEVICE_ID";
            }

            byte[] publicBytes = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(androidId.getBytes("UTF-8"));

            byte[] licenciaBytes = Base64.decode(licenciaIngresadaBase64.trim(), Base64.DEFAULT);
            return signature.verify(licenciaBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void guardarLicenciaValida(Context context, String licencia) {
        SharedPreferences prefs = context.getSharedPreferences("seguridad_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("licencia_activa", licencia).apply();
    }

    public static boolean yaEstaActivado(Context context) {
        if (MODO_DESARROLLO) {
            return true;
        }
        SharedPreferences prefs = context.getSharedPreferences("seguridad_prefs", Context.MODE_PRIVATE);
        String licenciaGuardada = prefs.getString("licencia_activa", "");
        return verificarLicenciaOffline(licenciaGuardada, context);
    }
}
