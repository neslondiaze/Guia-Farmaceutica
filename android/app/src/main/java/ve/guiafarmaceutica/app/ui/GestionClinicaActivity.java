package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.data.Paciente;
import ve.guiafarmaceutica.app.util.DemoDataHelper;

public class GestionClinicaActivity extends AppCompatActivity {

    private TextView textPacienteNombre, textPacienteSub;
    private View cardPacienteMaria;
    private long pacienteMariaId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_clinica);

        DemoDataHelper.asegurarDatosDemo(this);

        setupUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarPacienteReciente();
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        textPacienteNombre = findViewById(R.id.text_paciente_nombre_dash);
        textPacienteSub = findViewById(R.id.text_paciente_sub_dash);
        cardPacienteMaria = findViewById(R.id.card_paciente_maria);

        findViewById(R.id.card_action_impresion).setOnClickListener(v -> {
            Intent intent = new Intent(this, CrearImpresionActivity.class);
            if (pacienteMariaId != -1) {
                intent.putExtra("extra_paciente_id", pacienteMariaId);
            }
            startActivity(intent);
        });

        findViewById(R.id.card_action_indicacion).setOnClickListener(v -> {
            Intent intent = new Intent(this, CrearIndicacionActivity.class);
            if (pacienteMariaId != -1) {
                intent.putExtra("extra_paciente_id", pacienteMariaId);
            }
            startActivity(intent);
        });

        findViewById(R.id.card_action_historial).setOnClickListener(v -> {
            startActivity(new Intent(this, HistorialImpresionesActivity.class));
        });

        View.OnClickListener abrirFicha = v -> {
            if (pacienteMariaId != -1) {
                Intent intent = new Intent(this, PerfilPacienteActivity.class);
                intent.putExtra(PerfilPacienteActivity.EXTRA_PACIENTE_ID, pacienteMariaId);
                startActivity(intent);
            }
        };

        findViewById(R.id.btn_ver_ficha_maria).setOnClickListener(abrirFicha);
        cardPacienteMaria.setOnClickListener(abrirFicha);

        EditText editBuscar = findViewById(R.id.edit_buscar_paciente);
        editBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarPacientes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void cargarPacienteReciente() {
        new Thread(() -> {
            List<Paciente> lista = AppDatabase.obtener(this).pacienteDao().listarPacientes();
            if (lista != null && !lista.isEmpty()) {
                Paciente maria = lista.get(0);
                pacienteMariaId = maria.id;
                runOnUiThread(() -> {
                    textPacienteNombre.setText(maria.nombre_completo);
                    textPacienteSub.setText("ID " + maria.identificacion + " · Atención ficticia");
                });
            }
        }).start();
    }

    private void filtrarPacientes(String query) {
        new Thread(() -> {
            List<Paciente> resultados = AppDatabase.obtener(this).pacienteDao().buscarPacientes(query);
            runOnUiThread(() -> {
                if (resultados != null && !resultados.isEmpty()) {
                    Paciente p = resultados.get(0);
                    pacienteMariaId = p.id;
                    textPacienteNombre.setText(p.nombre_completo);
                    textPacienteSub.setText("ID " + p.identificacion + " · Atención ficticia");
                    cardPacienteMaria.setVisibility(View.VISIBLE);
                } else {
                    cardPacienteMaria.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
