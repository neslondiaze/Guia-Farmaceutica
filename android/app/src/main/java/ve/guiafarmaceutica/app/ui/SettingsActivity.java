package ve.guiafarmaceutica.app.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.repository.SettingsRepository;

public class SettingsActivity extends AppCompatActivity {

    private SettingsRepository repository;

    private TextInputEditText editNombre, editEspecialidad, editMpps, editClinica, editDireccion, editTelefono, editEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        repository = new SettingsRepository(this);

        setupUI();
        cargarDatos();
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        editNombre = findViewById(R.id.edit_medico_nombre);
        editEspecialidad = findViewById(R.id.edit_medico_especialidad);
        editMpps = findViewById(R.id.edit_medico_mpps);
        editClinica = findViewById(R.id.edit_medico_clinica);
        editDireccion = findViewById(R.id.edit_medico_direccion);
        editTelefono = findViewById(R.id.edit_medico_telefono);
        editEmail = findViewById(R.id.edit_medico_email);

        Button btnGuardar = findViewById(R.id.btn_guardar_ajustes);
        btnGuardar.setOnClickListener(v -> guardarDatos());

        Button btnEliminarTodo = findViewById(R.id.btn_eliminar_todo);
        if (btnEliminarTodo != null) {
            btnEliminarTodo.setOnClickListener(v -> confirmarEliminacionDatos());
        }
    }

    private void cargarDatos() {
        editNombre.setText(repository.getMedicoNombre());
        editEspecialidad.setText(repository.getMedicoEspecialidad());
        editMpps.setText(repository.getMedicoMpps());
        editClinica.setText(repository.getMedicoClinica());
        editDireccion.setText(repository.getMedicoDireccion());
        editTelefono.setText(repository.getMedicoTelefono());
        editEmail.setText(repository.getMedicoEmail());
    }

    private void guardarDatos() {
        repository.setMedicoNombre(editNombre.getText().toString());
        repository.setMedicoEspecialidad(editEspecialidad.getText().toString());
        repository.setMedicoMpps(editMpps.getText().toString());
        repository.setMedicoClinica(editClinica.getText().toString());
        repository.setMedicoDireccion(editDireccion.getText().toString());
        repository.setMedicoTelefono(editTelefono.getText().toString());
        repository.setMedicoEmail(editEmail.getText().toString());

        Toast.makeText(this, "Perfil profesional guardado para membretes de récipes", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmarEliminacionDatos() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar Todos Mis Datos")
                .setMessage("¿Está seguro de eliminar permanentemente su perfil profesional, todo el historial de récipes y los archivos temporales almacenados localmente? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar Todo", (dialog, which) -> eliminarTodosLosDatos())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarTodosLosDatos() {
        new Thread(() -> {
            try {
                // 1. Borrar SharedPreferences
                getSharedPreferences("guia_farmaceutica_settings", MODE_PRIVATE).edit().clear().apply();

                // 2. Borrar tablas de impresiones y clínicas
                AppDatabase.obtener(this).clearAllTables();

                // 3. Borrar archivos PDF en caché
                File pdfDir = new File(getCacheDir(), "impresiones");
                if (pdfDir.exists() && pdfDir.isDirectory()) {
                    for (File f : pdfDir.listFiles()) {
                        f.delete();
                    }
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Todos sus datos han sido eliminados del dispositivo.", Toast.LENGTH_LONG).show();
                    cargarDatos();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al eliminar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
