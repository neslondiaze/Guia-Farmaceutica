package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.data.FichaClinica;
import ve.guiafarmaceutica.app.data.ImpresionDiagnostica;
import ve.guiafarmaceutica.app.data.ImpresionDetalle;
import ve.guiafarmaceutica.app.util.AsistenteAiCore;
import ve.guiafarmaceutica.app.util.CryptoHelper;
import ve.guiafarmaceutica.app.util.ImpresionPdfHelper;

public class CrearImpresionActivity extends AppCompatActivity {

    public static final String EXTRA_MED_NOMBRE = "extra_med_nombre";
    public static final String EXTRA_DOSIS = "extra_dosis";

    private TextInputEditText editPacienteNombre, editPacienteCedula, editPacienteEdad, editPacienteDiagnostico;
    private TextInputEditText editMedNombre, editDosis, editFrecuencia, editDuracion, editObservaciones;
    private TextView textEmptyPrescripciones;

    private RecyclerView recyclerPrescripciones;
    private PrescripcionAdapter adapter;
    private final List<ImpresionDetalle> listaPrescripciones = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_impresion);

        String medNombre = getIntent().getStringExtra(EXTRA_MED_NOMBRE);
        String dosisStr = getIntent().getStringExtra(EXTRA_DOSIS);

        setupUI();

        if (medNombre != null && !medNombre.isEmpty()) {
            editMedNombre.setText(medNombre);
        }
        if (dosisStr != null && !dosisStr.isEmpty()) {
            editDosis.setText(dosisStr);
        }
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        editPacienteNombre = findViewById(R.id.edit_paciente_nombre);
        editPacienteCedula = findViewById(R.id.edit_paciente_cedula);
        editPacienteEdad = findViewById(R.id.edit_paciente_edad);
        editPacienteDiagnostico = findViewById(R.id.edit_paciente_diagnostico);

        editMedNombre = findViewById(R.id.edit_impresion_med_nombre);
        editDosis = findViewById(R.id.edit_impresion_dosis);
        editFrecuencia = findViewById(R.id.edit_impresion_frecuencia);
        editDuracion = findViewById(R.id.edit_impresion_duracion);
        editObservaciones = findViewById(R.id.edit_impresion_observaciones);

        textEmptyPrescripciones = findViewById(R.id.text_empty_prescripciones);
        recyclerPrescripciones = findViewById(R.id.recycler_prescripciones);

        recyclerPrescripciones.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PrescripcionAdapter();
        recyclerPrescripciones.setAdapter(adapter);

        Button btnEvaluarAi = findViewById(R.id.btn_evaluar_diagnostico_ai);
        btnEvaluarAi.setOnClickListener(v -> evaluarConIA());

        Button btnAbrirCalc = findViewById(R.id.btn_abrir_calculadora_impresion);
        btnAbrirCalc.setOnClickListener(v -> {
            String med = editMedNombre.getText().toString().trim();
            Intent calcIntent = new Intent(this, CalculadoraActivity.class);
            calcIntent.putExtra(CalculadoraActivity.EXTRA_NOMBRE, med);
            startActivity(calcIntent);
        });

        Button btnAgregarFarmaco = findViewById(R.id.btn_agregar_item_impresion);
        btnAgregarFarmaco.setOnClickListener(v -> agregarFarmacoActual());

        Button btnGenerarPdf = findViewById(R.id.btn_generar_pdf_impresion);
        btnGenerarPdf.setOnClickListener(v -> generarImpresion());
    }

    private void evaluarConIA() {
        String diagnostico = editPacienteDiagnostico.getText().toString().trim();
        if (diagnostico.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese una Impresión Diagnóstica para evaluar con IA", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Evaluando diagnóstico e identificando medicamentos de referencia...", Toast.LENGTH_SHORT).show();

        AsistenteAiCore.evaluarImpresionDiagnostica(this, diagnostico, (sugeridos, resumen) -> {
            if (sugeridos != null && !sugeridos.isEmpty()) {
                for (FichaClinica med : sugeridos) {
                    ImpresionDetalle d = new ImpresionDetalle();
                    d.medicamento_nombre = med.nombre;
                    d.dosificacion = "Según indicación médica";
                    d.frecuencia_instrucciones = "cada 8 a 12 horas";
                    d.duracion_dias = "5 a 7 días";
                    listaPrescripciones.add(d);
                }
                adapter.notifyDataSetChanged();
                textEmptyPrescripciones.setVisibility(listaPrescripciones.isEmpty() ? View.VISIBLE : View.GONE);
                Toast.makeText(this, "IA: Se agregaron " + sugeridos.size() + " medicamentos de referencia.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "IA: " + resumen, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void agregarFarmacoActual() {
        String medNombre = editMedNombre.getText().toString().trim();
        if (medNombre.isEmpty()) {
            Toast.makeText(this, "Ingrese el nombre del medicamento de referencia antes de agregarlo", Toast.LENGTH_SHORT).show();
            return;
        }

        String dosis = editDosis.getText().toString().trim();
        String frec = editFrecuencia.getText().toString().trim();
        String dur = editDuracion.getText().toString().trim();

        ImpresionDetalle d = new ImpresionDetalle();
        d.medicamento_nombre = medNombre;
        d.dosificacion = dosis.isEmpty() ? "Dosis según indicación" : dosis;
        d.frecuencia_instrucciones = frec.isEmpty() ? "cada 8 horas" : frec;
        d.duracion_dias = dur.isEmpty() ? "5 días" : dur;

        listaPrescripciones.add(d);
        adapter.notifyDataSetChanged();
        textEmptyPrescripciones.setVisibility(listaPrescripciones.isEmpty() ? View.VISIBLE : View.GONE);

        // Limpiar campos para el siguiente fármaco
        editMedNombre.setText("");
        editDosis.setText("");
        editFrecuencia.setText("");
        editDuracion.setText("");
        Toast.makeText(this, "Fármaco de referencia agregado", Toast.LENGTH_SHORT).show();
    }

    private void generarImpresion() {
        // Si hay texto en los campos de adición, agregarlo antes de emitir
        String medNombre = editMedNombre.getText().toString().trim();
        if (!medNombre.isEmpty()) {
            agregarFarmacoActual();
        }

        if (listaPrescripciones.isEmpty()) {
            Toast.makeText(this, "Agregue al menos un medicamento de referencia a la impresión", Toast.LENGTH_SHORT).show();
            return;
        }

        String pacienteNombre = editPacienteNombre.getText().toString().trim();
        if (pacienteNombre.isEmpty()) {
            pacienteNombre = "Paciente Consulta";
        }

        ImpresionDiagnostica impresion = new ImpresionDiagnostica();
        impresion.paciente_nombre = CryptoHelper.cifrar(pacienteNombre);
        impresion.paciente_cedula = CryptoHelper.cifrar(editPacienteCedula.getText().toString().trim());
        impresion.paciente_edad = editPacienteEdad.getText().toString().trim();
        impresion.diagnostico = CryptoHelper.cifrar(editPacienteDiagnostico.getText().toString().trim());
        impresion.observaciones = editObservaciones.getText().toString().trim();
        impresion.fecha_creacion = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        final String finalPacienteNombre = pacienteNombre;
        final String finalPacienteCedula = editPacienteCedula.getText().toString().trim();
        final String finalDiagnostico = editPacienteDiagnostico.getText().toString().trim();

        new Thread(() -> {
            try {
                long impresionId = AppDatabase.obtener(this).impresionDao().insertarImpresion(impresion);
                impresion.id = impresionId;
                for (ImpresionDetalle d : listaPrescripciones) {
                    d.impresion_id = impresionId;
                }
                AppDatabase.obtener(this).impresionDao().insertarDetalles(listaPrescripciones);

                ImpresionDiagnostica impresionClara = new ImpresionDiagnostica();
                impresionClara.id = impresionId;
                impresionClara.paciente_nombre = finalPacienteNombre;
                impresionClara.paciente_cedula = finalPacienteCedula;
                impresionClara.paciente_edad = impresion.paciente_edad;
                impresionClara.diagnostico = finalDiagnostico;
                impresionClara.observaciones = impresion.observaciones;
                impresionClara.fecha_creacion = impresion.fecha_creacion;

                File pdfFile = ImpresionPdfHelper.generarPdfImpresion(this, impresionClara, listaPrescripciones);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Ficha de Impresión Diagnóstica PDF generada exitosamente", Toast.LENGTH_SHORT).show();
                    compartirPdf(pdfFile);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void compartirPdf(File pdfFile) {
        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Compartir o Imprimir Impresión Diagnóstica PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Informe guardado en caché: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    class PrescripcionAdapter extends RecyclerView.Adapter<PrescripcionAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_impresion_farmaco, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImpresionDetalle d = listaPrescripciones.get(position);
            holder.bind(d, position);
        }

        @Override
        public int getItemCount() {
            return listaPrescripciones.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nombre, indicaciones;
            ImageButton btnEliminar;

            ViewHolder(View view) {
                super(view);
                nombre = view.findViewById(R.id.text_item_med_nombre);
                indicaciones = view.findViewById(R.id.text_item_indicaciones);
                btnEliminar = view.findViewById(R.id.btn_eliminar_item_prescripcion);
            }

            void bind(ImpresionDetalle d, int pos) {
                nombre.setText((pos + 1) + ". " + d.medicamento_nombre);
                indicaciones.setText("Dosis: " + d.dosificacion + " " + d.frecuencia_instrucciones + " (Durante " + d.duracion_dias + ")");
                btnEliminar.setOnClickListener(v -> {
                    listaPrescripciones.remove(pos);
                    notifyDataSetChanged();
                    textEmptyPrescripciones.setVisibility(listaPrescripciones.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
