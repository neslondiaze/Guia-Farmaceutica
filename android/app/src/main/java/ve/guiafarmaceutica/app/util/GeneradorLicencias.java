package ve.guiafarmaceutica.app.util;

import android.util.Base64;
import java.security.PrivateKey;
import java.security.Signature;

public class GeneradorLicencias {

    public static String generarFirmaLicencia(String androidId, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(androidId.getBytes("UTF-8"));
            byte[] firmaMatematica = signature.sign();
            return Base64.encodeToString(firmaMatematica, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
