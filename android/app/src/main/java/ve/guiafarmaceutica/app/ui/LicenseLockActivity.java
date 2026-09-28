package ve.guiafarmaceutica.app.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.util.DeviceBindingManager;

public class LicenseLockActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_lock);

        TextView textId = findViewById(R.id.text_id_dispositivo_lock);
        if (textId != null) {
            textId.setText(DeviceBindingManager.obtenerImeiOIdentificador(this));
        }

        Button btnCerrar = findViewById(R.id.btn_cerrar_lock);
        if (btnCerrar != null) {
            btnCerrar.setOnClickListener(v -> finishAffinity());
        }
    }
}
