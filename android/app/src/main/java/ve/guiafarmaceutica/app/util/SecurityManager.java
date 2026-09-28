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
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2CA7Md729GXSRXQl9q1pA06x3us7bX+EXp2d4YilRVvpQlQIiALyo94WKVu7iOXD7yda+1L+nbghUUSdrPnoB4cPVFbsPBPstmjYfVi6QiLmQZjWKm5S6Z9r1yByNHocqV6PftBLrLGvlSATZvI73GtSMAcSUYme3wj0A1jNSLlpaP0zLVfBaJOCl3NjBW1bGgCFmAJgzzPMkl4ozTXukz8MwJxTqQw8YJR1ancCkiK5pDYjVQb92CCLJsxLklAdwMybZdOp/t9RoBp0iUBo7GTm/Dns5QtrSQ5gvMsyTlRSW4Ww/AOuhoU7Mk42KmT86Ut/CCISHfxOFxdnxtTlsQIDAQAB";

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
