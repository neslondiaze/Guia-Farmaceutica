package ve.guiafarmaceutica.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.util.SecurityManager;

public class ActivationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);

        String androidId = "";
        try {
            androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception ignored) {}
        if (androidId == null || androidId.isEmpty()) {
            androidId = "DEFAULT_DEVICE_ID";
        }

        TextView textId = findViewById(R.id.text_android_id);
        if (textId != null) {
            textId.setText(androidId);
        }

        Button btnCopiar = findViewById(R.id.btn_copiar_id);
        if (btnCopiar != null) {
            String finalAndroidId = androidId;
            btnCopiar.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("ANDROID_ID", finalAndroidId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show();
            });
        }

        TextInputEditText editLicencia = findViewById(R.id.edit_licencia_input);
        Button btnActivar = findViewById(R.id.btn_activar_licencia);

        if (btnActivar != null && editLicencia != null) {
            btnActivar.setOnClickListener(v -> {
                String licencia = editLicencia.getText() != null ? editLicencia.getText().toString().trim() : "";
                if (licencia.isEmpty()) {
                    Toast.makeText(this, "Por favor ingrese el código de licencia", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (SecurityManager.verificarLicenciaOffline(licencia, this)) {
                    SecurityManager.guardarLicenciaValida(this, licencia);
                    Toast.makeText(this, "¡Licencia activada exitosamente!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "Código de licencia inválido para este dispositivo", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}
