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
import androidx.appcompat.app.AlertDialog;
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
import ve.guiafarmaceutica.app.data.Paciente;
import ve.guiafarmaceutica.app.util.AsistenteAiCore;
import ve.guiafarmaceutica.app.util.CryptoHelper;
import ve.guiafarmaceutica.app.util.ImpresionPdfHelper;

public class CrearImpresionActivity extends AppCompatActivity {

    public static final String EXTRA_MED_NOMBRE = "extra_med_nombre";
    public static final String EXTRA_DOSIS = "extra_dosis";

    private TextInputEditText editPacienteNombre, editPacienteCedula, editPacienteEdad, editPacienteDiagnostico;
    private TextInputEditText editMedNombre, editPresentacion, editConcentracion, editDosis, editFrecuencia, editDuracion, editObservaciones;
    private TextView textEmptyMedicamentosReferencia;
    private View cardFormAgregar;

    private RecyclerView recyclerMedicamentosReferencia;
    private MedicamentoReferenciaAdapter adapter;
    private final List<ImpresionDetalle> listaMedicamentosReferencia = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_impresion);

        String medNombre = getIntent().getStringExtra(EXTRA_MED_NOMBRE);
        String dosisStr = getIntent().getStringExtra(EXTRA_DOSIS);

        setupUI();

        long pacienteId = getIntent().getLongExtra("extra_paciente_id", -1);
        if (pacienteId != -1) {
            new Thread(() -> {
                Paciente p = AppDatabase.obtener(this).pacienteDao().obtenerPorId(pacienteId);
                if (p != null) {
                    runOnUiThread(() -> {
                        if (editPacienteNombre != null) editPacienteNombre.setText(p.nombre_completo);
                        if (editPacienteCedula != null) editPacienteCedula.setText(p.identificacion);
                        if (editPacienteEdad != null) editPacienteEdad.setText(p.edad_calculada);
                    });
                }
            }).start();
        }

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
        editPresentacion = findViewById(R.id.edit_impresion_presentacion);
        editConcentracion = findViewById(R.id.edit_impresion_concentracion);
        editDosis = findViewById(R.id.edit_impresion_dosis);
        editFrecuencia = findViewById(R.id.edit_impresion_frecuencia);
        editDuracion = findViewById(R.id.edit_impresion_duracion);
        editObservaciones = findViewById(R.id.edit_recipe_observaciones);

        textEmptyMedicamentosReferencia = findViewById(R.id.text_empty_medicamentos_referencia);
        recyclerMedicamentosReferencia = findViewById(R.id.recycler_medicamentos_referencia);
        cardFormAgregar = findViewById(R.id.card_form_agregar_med);
        if (cardFormAgregar != null) {
            cardFormAgregar.setVisibility(View.GONE);
        }

        recyclerMedicamentosReferencia.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicamentoReferenciaAdapter();
        recyclerMedicamentosReferencia.setAdapter(adapter);

        Button btnAgregarPlus = findViewById(R.id.btn_agregar_nuevo_med);
        if (btnAgregarPlus != null && cardFormAgregar != null) {
            btnAgregarPlus.setOnClickListener(v -> {
                cardFormAgregar.setVisibility(cardFormAgregar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });
        }

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

        Button btnCompartir = findViewById(R.id.btn_compartir_enviar_impresion);
        if (btnCompartir != null) {
            btnCompartir.setOnClickListener(v -> guardarYCompartirImpresion());
        }
    }

    private void evaluarConIA() {
        String diagnostico = editPacienteDiagnostico.getText().toString().trim();
        if (diagnostico.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese una Impresión Diagnóstica para evaluar con IA", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Evaluando diagnóstico e identificando medicamentos de referencia...", Toast.LENGTH_SHORT).show();

        String edad = editPacienteEdad != null ? editPacienteEdad.getText().toString().trim() : "";
        AsistenteAiCore.evaluarImpresionDiagnostica(this, diagnostico, edad, (sugeridos, resumen) -> {
            if (sugeridos != null && !sugeridos.isEmpty()) {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sugerencias_ai, null);
                RecyclerView recyclerDialog = dialogView.findViewById(R.id.recycler_sugerencias_ai_dialog);
                recyclerDialog.setLayoutManager(new LinearLayoutManager(this));

                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setNegativeButton("Cerrar", null)
                        .create();

                MedicamentoSugeridoAdapter sugAdapter = new MedicamentoSugeridoAdapter(sugeridos, med -> {
                    ImpresionDetalle d = new ImpresionDetalle();
                    d.medicamento_id = med.id;
                    d.medicamento_nombre = med.nombre;
                    d.presentacion = med.forma != null ? med.forma : "";
                    d.concentracion = med.atc_descripcion != null ? med.atc_descripcion : "";
                    d.dosificacion = "";
                    d.frecuencia_instrucciones = "";
                    d.duracion_dias = "";

                    listaMedicamentosReferencia.add(d);
                    adapter.notifyDataSetChanged();
                    textEmptyMedicamentosReferencia.setVisibility(listaMedicamentosReferencia.isEmpty() ? View.VISIBLE : View.GONE);
                    Toast.makeText(this, "Medicamento agregado: " + med.nombre, Toast.LENGTH_SHORT).show();
                });

                recyclerDialog.setAdapter(sugAdapter);
                dialog.show();
            } else {
                Toast.makeText(this, resumen, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void agregarFarmacoActual() {
        String medNombre = editMedNombre.getText().toString().trim();
        if (medNombre.isEmpty()) {
            Toast.makeText(this, "Ingrese el nombre del medicamento de referencia antes de agregarlo", Toast.LENGTH_SHORT).show();
            return;
        }

        String presentacion = editPresentacion.getText().toString().trim();
        String concentracion = editConcentracion.getText().toString().trim();
        String dosis = editDosis.getText().toString().trim();
        String frecuencia = editFrecuencia != null && editFrecuencia.getText() != null ? editFrecuencia.getText().toString().trim() : "";
        String duracion = editDuracion != null && editDuracion.getText() != null ? editDuracion.getText().toString().trim() : "";

        ImpresionDetalle d = new ImpresionDetalle();
        d.medicamento_nombre = medNombre;
        d.presentacion = presentacion;
        d.concentracion = concentracion;
        d.dosificacion = dosis;
        d.frecuencia_instrucciones = frecuencia;
        d.duracion_dias = duracion;

        listaMedicamentosReferencia.add(d);
        adapter.notifyDataSetChanged();
        textEmptyMedicamentosReferencia.setVisibility(listaMedicamentosReferencia.isEmpty() ? View.VISIBLE : View.GONE);

        // Limpiar campos
        editMedNombre.setText("");
        editPresentacion.setText("");
        editConcentracion.setText("");
        editDosis.setText("");
        if (editFrecuencia != null) editFrecuencia.setText("");
        if (editDuracion != null) editDuracion.setText("");
        if (cardFormAgregar != null) {
            cardFormAgregar.setVisibility(View.GONE);
        }
        Toast.makeText(this, "Fármaco agregado a la lista", Toast.LENGTH_SHORT).show();
    }

    private void guardarYCompartirImpresion() {
        String medNombre = editMedNombre.getText().toString().trim();
        if (!medNombre.isEmpty()) {
            agregarFarmacoActual();
        }

        if (listaMedicamentosReferencia.isEmpty()) {
            Toast.makeText(this, "Agregue al menos un medicamento a la impresión diagnóstica", Toast.LENGTH_SHORT).show();
            return;
        }

        String pacienteNombre = editPacienteNombre.getText().toString().trim();
        if (pacienteNombre.isEmpty()) {
            pacienteNombre = "María López";
        }
        String pacienteCedula = editPacienteCedula.getText().toString().trim();
        String pacienteEdad = editPacienteEdad.getText().toString().trim();

        ImpresionDiagnostica impresion = new ImpresionDiagnostica();
        impresion.paciente_nombre = CryptoHelper.cifrar(pacienteNombre);
        impresion.paciente_cedula = CryptoHelper.cifrar(pacienteCedula);
        impresion.paciente_edad = pacienteEdad;
        impresion.diagnostico = CryptoHelper.cifrar(editPacienteDiagnostico.getText().toString().trim());
        impresion.observaciones = editObservaciones.getText().toString().trim();
        impresion.estado = "Emitido";
        impresion.fecha_creacion = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        final String finalPacienteNombre = pacienteNombre;
        final String finalPacienteCedula = pacienteCedula;
        final String finalDiagnostico = editPacienteDiagnostico.getText().toString().trim();

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.obtener(this);

                // Crear o actualizar el Paciente en la tabla 'pacientes' para que aparezca en Gestión de Pacientes
                Paciente paciente = null;
                if (!pacienteCedula.isEmpty()) {
                    paciente = db.pacienteDao().obtenerPorIdentificacion(pacienteCedula);
                }
                if (paciente == null) {
                    paciente = new Paciente();
                }
                paciente.nombre_completo = finalPacienteNombre;
                paciente.identificacion = !pacienteCedula.isEmpty() ? pacienteCedula : ("ID-" + (System.currentTimeMillis() % 100000));
                paciente.edad_calculada = !pacienteEdad.isEmpty() ? pacienteEdad : "S/I";
                long pacienteId = db.pacienteDao().insertar(paciente);

                impresion.paciente_id = pacienteId;
                long impresionId = db.impresionDao().insertarImpresion(impresion);
                impresion.id = impresionId;
                for (ImpresionDetalle d : listaMedicamentosReferencia) {
                    d.impresion_id = impresionId;
                }
                db.impresionDao().insertarDetalles(listaMedicamentosReferencia);

                runOnUiThread(() -> {
                    ImpresionDiagnostica impresionClara = new ImpresionDiagnostica();
                    impresionClara.id = impresionId;
                    impresionClara.paciente_nombre = finalPacienteNombre;
                    impresionClara.paciente_cedula = finalPacienteCedula;
                    impresionClara.paciente_edad = impresion.paciente_edad;
                    impresionClara.diagnostico = finalDiagnostico;
                    impresionClara.observaciones = impresion.observaciones;
                    impresionClara.fecha_creacion = impresion.fecha_creacion;

                    try {
                        File pdfFile = ImpresionPdfHelper.generarPdfRecipeEIndicaciones(this, impresionClara, listaMedicamentosReferencia);
                        Toast.makeText(this, "Récipe e Indicaciones Médicas PDF generados exitosamente", Toast.LENGTH_SHORT).show();
                        compartirPdf(pdfFile);
                    } catch (Exception ex) {
                        Toast.makeText(this, "Error al generar PDF: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al guardar impresión: " + e.getMessage(), Toast.LENGTH_LONG).show());
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

    class MedicamentoReferenciaAdapter extends RecyclerView.Adapter<MedicamentoReferenciaAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_impresion_farmaco, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImpresionDetalle d = listaMedicamentosReferencia.get(position);
            holder.bind(d, position + 1);
        }

        @Override
        public int getItemCount() {
            return listaMedicamentosReferencia.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView numero, nombre, presentacion, concentracion, dosis;
            ImageButton btnEliminar, btnGuia;

            ViewHolder(View view) {
                super(view);
                numero = view.findViewById(R.id.text_item_numero);
                nombre = view.findViewById(R.id.text_item_med_nombre);
                presentacion = view.findViewById(R.id.text_item_presentacion);
                concentracion = view.findViewById(R.id.text_item_concentracion);
                dosis = view.findViewById(R.id.text_item_dosis);
                btnEliminar = view.findViewById(R.id.btn_eliminar_medicamento_referencia);
                btnGuia = view.findViewById(R.id.btn_ver_guia_medicamento);
            }

            void bind(ImpresionDetalle d, int num) {
                if (numero != null) {
                    numero.setText(String.valueOf(num));
                }
                nombre.setText(d.medicamento_nombre);
                presentacion.setText(d.presentacion != null ? d.presentacion : "-");
                concentracion.setText(d.concentracion != null ? d.concentracion : "-");
                dosis.setText(d.dosificacion != null ? d.dosificacion : "-");
                btnEliminar.setOnClickListener(v -> {
                    int adapterPos = getBindingAdapterPosition();
                    if (adapterPos != RecyclerView.NO_POSITION && adapterPos < listaMedicamentosReferencia.size()) {
                        listaMedicamentosReferencia.remove(adapterPos);
                        notifyItemRemoved(adapterPos);
                        notifyItemRangeChanged(adapterPos, listaMedicamentosReferencia.size());
                        textEmptyMedicamentosReferencia.setVisibility(listaMedicamentosReferencia.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
                if (btnGuia != null) {
                    btnGuia.setOnClickListener(v -> {
                        Intent intent = new Intent(v.getContext(), FichaActivity.class);
                        if (d.medicamento_id != null && !d.medicamento_id.isEmpty()) {
                            intent.putExtra(FichaActivity.EXTRA_ID, d.medicamento_id);
                        }
                        intent.putExtra(FichaActivity.EXTRA_NOMBRE, d.medicamento_nombre);
                        v.getContext().startActivity(intent);
                    });
                }
            }
        }
    }

    class MedicamentoSugeridoAdapter extends RecyclerView.Adapter<MedicamentoSugeridoAdapter.ViewHolder> {
        private final List<FichaClinica> listaSugeridos;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onAgregar(FichaClinica med);
        }

        MedicamentoSugeridoAdapter(List<FichaClinica> listaSugeridos, OnItemClickListener listener) {
            this.listaSugeridos = listaSugeridos;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sugerencia_ai, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FichaClinica f = listaSugeridos.get(position);
            holder.nombre.setText(f.nombre);
            holder.presentacion.setText(f.forma != null ? f.forma : "Forma farmacéutica estándar");
            holder.btnAgregar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAgregar(f);
                }
            });
        }

        @Override
        public int getItemCount() {
            return listaSugeridos.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nombre, presentacion;
            Button btnAgregar;

            ViewHolder(View view) {
                super(view);
                nombre = view.findViewById(R.id.text_sugerencia_nombre);
                presentacion = view.findViewById(R.id.text_sugerencia_presentacion);
                btnAgregar = view.findViewById(R.id.btn_sugerencia_agregar);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
