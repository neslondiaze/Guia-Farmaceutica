package ve.guiafarmaceutica.app.repository;

import android.content.Context;
import android.content.SharedPreferences;
import ve.guiafarmaceutica.app.util.CryptoHelper;

public class SettingsRepository {

    private static final String PREF_NAME = "guia_farmaceutica_settings";

    public static final String KEY_MEDICO_NOMBRE = "medico_nombre";
    public static final String KEY_MEDICO_ESPECIALIDAD = "medico_especialidad";
    public static final String KEY_MEDICO_MPPS = "medico_mpps";
    public static final String KEY_MEDICO_CLINICA = "medico_clinica";
    public static final String KEY_MEDICO_DIRECCION = "medico_direccion";
    public static final String KEY_MEDICO_TELEFONO = "medico_telefono";
    public static final String KEY_MEDICO_EMAIL = "medico_email";
    public static final String KEY_TEMA_MODO = "tema_modo"; // 0: sistema, 1: claro, 2: oscuro

    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getMedicoNombre() {
        String val = prefs.getString(KEY_MEDICO_NOMBRE, null);
        return val != null ? CryptoHelper.descifrar(val) : "Dr. Profesional de la Salud";
    }

    public void setMedicoNombre(String valor) {
        prefs.edit().putString(KEY_MEDICO_NOMBRE, CryptoHelper.cifrar(valor)).apply();
    }

    public String getMedicoEspecialidad() {
        String val = prefs.getString(KEY_MEDICO_ESPECIALIDAD, null);
        return val != null ? CryptoHelper.descifrar(val) : "Medicina General";
    }

    public void setMedicoEspecialidad(String valor) {
        prefs.edit().putString(KEY_MEDICO_ESPECIALIDAD, CryptoHelper.cifrar(valor)).apply();
    }

    public String getMedicoMpps() {
        String val = prefs.getString(KEY_MEDICO_MPPS, null);
        return val != null ? CryptoHelper.descifrar(val) : "MPPS: 000000 / Col: 0000";
    }

    public void setMedicoMpps(String valor) {
        prefs.edit().putString(KEY_MEDICO_MPPS, CryptoHelper.cifrar(valor)).apply();
    }

    public String getMedicoClinica() {
        String val = prefs.getString(KEY_MEDICO_CLINICA, null);
        return val != null ? CryptoHelper.descifrar(val) : "Centro Médico Clínico";
    }

    public void setMedicoClinica(String valor) {
        prefs.edit().putString(KEY_MEDICO_CLINICA, CryptoHelper.cifrar(valor)).apply();
    }

    public String getMedicoDireccion() {
        String val = prefs.getString(KEY_MEDICO_DIRECCION, null);
        return val != null ? CryptoHelper.descifrar(val) : "San Carlos, Cojedes, Venezuela";
    }

    public void setMedicoDireccion(String valor) {
        prefs.edit().putString(KEY_MEDICO_DIRECCION, CryptoHelper.cifrar(valor)).apply();
    }

    public String getMedicoTelefono() {
        String val = prefs.getString(KEY_MEDICO_TELEFONO, null);
        return val != null ? CryptoHelper.descifrar(val) : "0412-0000000";
    }

    public void setMedicoTelefono(String valor) {
        prefs.edit().putString(KEY_MEDICO_TELEFONO, CryptoHelper.cifrar(valor)).apply();
    }

    public String getMedicoEmail() {
        String val = prefs.getString(KEY_MEDICO_EMAIL, null);
        return val != null ? CryptoHelper.descifrar(val) : "doctor@salud.ve";
    }

    public void setMedicoEmail(String valor) {
        prefs.edit().putString(KEY_MEDICO_EMAIL, CryptoHelper.cifrar(valor)).apply();
    }

    public int getTemaModo() {
        return prefs.getInt(KEY_TEMA_MODO, 0);
    }

    public void setTemaModo(int modo) {
        prefs.edit().putInt(KEY_TEMA_MODO, modo).apply();
    }
}
