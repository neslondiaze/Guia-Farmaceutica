package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.data.IndicacionNoFarmacologica;
import ve.guiafarmaceutica.app.data.Paciente;
import ve.guiafarmaceutica.app.util.ImpresionPdfHelper;

public class CrearIndicacionActivity extends AppCompatActivity {

    private long pacienteId = -1;
    private Paciente paciente;

    private TextView textHeaderSub, textHeaderTitle;
    private TextInputEditText editActividad, editAlimentacion, editHidratacion, editCuidados, editEstudios, editAlarma, editFechaSeg, editNotas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_indicacion);

        pacienteId = getIntent().getLongExtra("extra_paciente_id", -1);

        setupUI();
        cargarPaciente();
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        textHeaderSub = findViewById(R.id.text_indicacion_header_sub);
        textHeaderTitle = findViewById(R.id.text_indicacion_header_title);

        editActividad = findViewById(R.id.edit_indicacion_actividad);
        editAlimentacion = findViewById(R.id.edit_indicacion_alimentacion);
        editHidratacion = findViewById(R.id.edit_indicacion_hidratacion);
        editCuidados = findViewById(R.id.edit_indicacion_cuidados);
        editEstudios = findViewById(R.id.edit_indicacion_estudios);
        editAlarma = findViewById(R.id.edit_indicacion_alarma);
        editFechaSeg = findViewById(R.id.edit_indicacion_fecha_seg);
        editNotas = findViewById(R.id.edit_indicacion_notas);

        Button btnBorrador = findViewById(R.id.btn_guardar_borrador_indicacion);
        btnBorrador.setOnClickListener(v -> guardarIndicacion("Borrador"));

        Button btnVistaPrevia = findViewById(R.id.btn_vista_previa_indicacion);
        btnVistaPrevia.setOnClickListener(v -> guardarIndicacion("Emitido"));
    }

    private void cargarPaciente() {
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
                    textHeaderTitle.setText("Indicaciones médicas");
                });
            }
        }).start();
    }

    private void guardarIndicacion(String estado) {
        IndicacionNoFarmacologica ind = new IndicacionNoFarmacologica();
        ind.paciente_id = pacienteId;
        ind.paciente_nombre = paciente != null ? paciente.nombre_completo : "Paciente Demo";
        ind.actividad_reposo = editActividad.getText().toString().trim();
        ind.alimentacion = editAlimentacion.getText().toString().trim();
        ind.hidratacion = editHidratacion.getText().toString().trim();
        ind.cuidados_generales = editCuidados.getText().toString().trim();
        ind.estudios_solicitados = editEstudios.getText().toString().trim();
        ind.senales_alarma = editAlarma.getText().toString().trim();
        ind.fecha_seguimiento = editFechaSeg.getText().toString().trim();
        ind.notas_adicionales = editNotas.getText().toString().trim();
        ind.estado = estado;
        ind.fecha_creacion = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        new Thread(() -> {
            try {
                long indId = AppDatabase.obtener(this).indicacionDao().insertar(ind);
                ind.id = indId;

                runOnUiThread(() -> {
                    if ("Emitido".equalsIgnoreCase(estado)) {
                        Toast.makeText(this, "Indicación emitida exitosamente", Toast.LENGTH_SHORT).show();
                        generarYCompartirPdf(ind);
                    } else {
                        Toast.makeText(this, "Borrador de indicación guardado exitosamente", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al guardar indicación: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void generarYCompartirPdf(IndicacionNoFarmacologica ind) {
        new Thread(() -> {
            try {
                File pdfFile = ImpresionPdfHelper.generarPdfIndicacion(this, ind, paciente);
                runOnUiThread(() -> {
                    Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Ver / Compartir Indicaciones PDF"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
