package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.data.Paciente;

public class PerfilPacienteActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";

    private long pacienteId = -1;
    private Paciente paciente;

    private TextView textHeaderTitle, textHeaderSub;
    private TextInputEditText editNombre, editId, editNac, editEdad, editSexo;
    private TextInputEditText editTelefono, editCorreo, editDireccion, editEmergencia;
    private TextInputEditText editAlergias, editCondiciones, editGrupoSang, editPeso, editAltura, editNotas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_paciente);

        pacienteId = getIntent().getLongExtra(EXTRA_PACIENTE_ID, -1);

        setupUI();
        cargarDatosPaciente();
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        textHeaderTitle = findViewById(R.id.text_perfil_header_title);
        textHeaderSub = findViewById(R.id.text_perfil_header_sub);

        editNombre = findViewById(R.id.edit_perfil_nombre);
        editId = findViewById(R.id.edit_perfil_id);
        editNac = findViewById(R.id.edit_perfil_fecha_nac);
        editEdad = findViewById(R.id.edit_perfil_edad);
        editSexo = findViewById(R.id.edit_perfil_sexo);

        editTelefono = findViewById(R.id.edit_perfil_telefono);
        editCorreo = findViewById(R.id.edit_perfil_correo);
        editDireccion = findViewById(R.id.edit_perfil_direccion);
        editEmergencia = findViewById(R.id.edit_perfil_emergencia);

        editAlergias = findViewById(R.id.edit_perfil_alergias);
        editCondiciones = findViewById(R.id.edit_perfil_condiciones);
        editGrupoSang = findViewById(R.id.edit_perfil_grupo_sang);
        editPeso = findViewById(R.id.edit_perfil_peso);
        editAltura = findViewById(R.id.edit_perfil_altura);
        editNotas = findViewById(R.id.edit_perfil_notas);

        Button btnEditar = findViewById(R.id.btn_editar_ficha);
        btnEditar.setOnClickListener(v -> guardarCambiosFicha());

        findViewById(R.id.btn_perfil_nueva_impresion).setOnClickListener(v -> {
            Intent intent = new Intent(this, CrearImpresionActivity.class);
            if (pacienteId != -1) {
                intent.putExtra("extra_paciente_id", pacienteId);
            }
            startActivity(intent);
        });

        findViewById(R.id.btn_perfil_historial).setOnClickListener(v -> {
            Intent intent = new Intent(this, HistorialImpresionesActivity.class);
            if (pacienteId != -1) {
                intent.putExtra("extra_paciente_id", pacienteId);
            }
            startActivity(intent);
        });
    }

    private void cargarDatosPaciente() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.obtener(this);
            if (pacienteId != -1) {
                paciente = db.pacienteDao().obtenerPorId(pacienteId);
            }
            if (paciente == null) {
                List<Paciente> lista = db.pacienteDao().listarPacientes();
                if (lista != null && !lista.isEmpty()) {
                    paciente = lista.get(0);
                    pacienteId = paciente.id;
                }
            }

            if (paciente != null) {
                runOnUiThread(() -> {
                    textHeaderSub.setText("PACIENTE FICTICIA: " + paciente.nombre_completo.toUpperCase());
                    textHeaderTitle.setText(paciente.nombre_completo + " (" + paciente.identificacion + ")");

                    editNombre.setText(paciente.nombre_completo);
                    editId.setText(paciente.identificacion);
                    editNac.setText(paciente.fecha_nacimiento);
                    editEdad.setText(paciente.edad_calculada);
                    editSexo.setText(paciente.sexo);

                    editTelefono.setText(paciente.telefono);
                    editCorreo.setText(paciente.correo);
                    editDireccion.setText(paciente.direccion);
                    editEmergencia.setText(paciente.contacto_emergencia);

                    editAlergias.setText(paciente.alergias);
                    editCondiciones.setText(paciente.condiciones_relevantes);
                    editGrupoSang.setText(paciente.grupo_sanguineo);
                    editPeso.setText(paciente.peso);
                    editAltura.setText(paciente.altura);
                    editNotas.setText(paciente.notas_clinicas);
                });
            }
        }).start();
    }

    private void guardarCambiosFicha() {
        if (paciente == null) return;

        paciente.nombre_completo = editNombre.getText().toString();
        paciente.identificacion = editId.getText().toString();
        paciente.fecha_nacimiento = editNac.getText().toString();
        paciente.edad_calculada = editEdad.getText().toString();
        paciente.sexo = editSexo.getText().toString();

        paciente.telefono = editTelefono.getText().toString();
        paciente.correo = editCorreo.getText().toString();
        paciente.direccion = editDireccion.getText().toString();
        paciente.contacto_emergencia = editEmergencia.getText().toString();

        paciente.alergias = editAlergias.getText().toString();
        paciente.condiciones_relevantes = editCondiciones.getText().toString();
        paciente.grupo_sanguineo = editGrupoSang.getText().toString();
        paciente.peso = editPeso.getText().toString();
        paciente.altura = editAltura.getText().toString();
        paciente.notas_clinicas = editNotas.getText().toString();

        new Thread(() -> {
            AppDatabase.obtener(this).pacienteDao().actualizar(paciente);
            runOnUiThread(() -> Toast.makeText(this, "Ficha del paciente actualizada exitosamente", Toast.LENGTH_SHORT).show());
        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
