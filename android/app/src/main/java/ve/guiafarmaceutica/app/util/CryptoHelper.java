package ve.guiafarmaceutica.app.util;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CryptoHelper {

    private static final String KEY_ALIAS = "guia_farmaceutica_master_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static SecretKey obtenerClaveMaestra() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec keyGenSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build();
            keyGenerator.init(keyGenSpec);
            return keyGenerator.generateKey();
        }

        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    /**
     * Cifra un texto claro con AES-256 GCM y retorna una cadena en Base64.
     */
    public static String cifrar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        try {
            SecretKey secretKey = obtenerClaveMaestra();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] textoCifradoBytes = cipher.doFinal(texto.getBytes("UTF-8"));

            byte[] combinado = new byte[iv.length + textoCifradoBytes.length];
            System.arraycopy(iv, 0, combinado, 0, iv.length);
            System.arraycopy(textoCifradoBytes, 0, combinado, iv.length, textoCifradoBytes.length);

            return Base64.encodeToString(combinado, Base64.NO_WRAP);
        } catch (Exception e) {
            return texto;
        }
    }

    /**
     * Descifra una cadena cifrada en Base64.
     */
    public static String descifrar(String textoCifradoBase64) {
        if (textoCifradoBase64 == null || textoCifradoBase64.isEmpty()) {
            return textoCifradoBase64;
        }
        try {
            byte[] combinado = Base64.decode(textoCifradoBase64, Base64.NO_WRAP);
            if (combinado.length < 12) {
                return textoCifradoBase64; // Si no está en formato cifrado
            }

            byte[] iv = new byte[12]; // GCM IV de 12 bytes
            System.arraycopy(combinado, 0, iv, 0, 12);

            byte[] textoCifradoBytes = new byte[combinado.length - 12];
            System.arraycopy(combinado, 12, textoCifradoBytes, 0, textoCifradoBytes.length);

            SecretKey secretKey = obtenerClaveMaestra();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] textoClaroBytes = cipher.doFinal(textoCifradoBytes);
            return new String(textoClaroBytes, "UTF-8");
        } catch (Exception e) {
            return textoCifradoBase64; // Si era texto plano previo
        }
    }
}
