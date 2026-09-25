package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.data.ImpresionDiagnostica;
import ve.guiafarmaceutica.app.data.ImpresionDetalle;
import ve.guiafarmaceutica.app.util.CryptoHelper;
import ve.guiafarmaceutica.app.util.ImpresionPdfHelper;

public class HistorialImpresionesActivity extends AppCompatActivity {

    private RecyclerView recyclerImpresiones;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private ImpresionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_impresiones);

        setupUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarImpresiones();
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        progressBar = findViewById(R.id.progress_loading);
        textEmpty = findViewById(R.id.text_empty);
        recyclerImpresiones = findViewById(R.id.recycler_impresiones);

        recyclerImpresiones.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ImpresionAdapter();
        recyclerImpresiones.setAdapter(adapter);

        ExtendedFloatingActionButton fabNuevo = findViewById(R.id.fab_nueva_impresion);
        fabNuevo.setOnClickListener(v -> startActivity(new Intent(this, CrearImpresionActivity.class)));
    }

    private void cargarImpresiones() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<ImpresionDiagnostica> impresiones = AppDatabase.obtener(this).impresionDao().listarImpresiones();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                adapter.setImpresiones(impresiones);
                textEmpty.setVisibility(impresiones.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void verPdfImpresion(ImpresionDiagnostica impresion) {
        new Thread(() -> {
            try {
                List<ImpresionDetalle> detalles = AppDatabase.obtener(this).impresionDao().obtenerDetalles(impresion.id);

                ImpresionDiagnostica impresionClara = new ImpresionDiagnostica();
                impresionClara.id = impresion.id;
                impresionClara.paciente_nombre = CryptoHelper.descifrar(impresion.paciente_nombre);
                impresionClara.paciente_cedula = CryptoHelper.descifrar(impresion.paciente_cedula);
                impresionClara.paciente_edad = impresion.paciente_edad;
                impresionClara.diagnostico = CryptoHelper.descifrar(impresion.diagnostico);
                impresionClara.observaciones = impresion.observaciones;
                impresionClara.fecha_creacion = impresion.fecha_creacion;

                File pdfFile = ImpresionPdfHelper.generarPdfImpresion(this, impresionClara, detalles);
                runOnUiThread(() -> {
                    Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Ver / Compartir Impresión Diagnóstica PDF"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void eliminarImpresion(ImpresionDiagnostica impresion) {
        new Thread(() -> {
            AppDatabase.obtener(this).impresionDao().eliminarDetalles(impresion.id);
            AppDatabase.obtener(this).impresionDao().eliminarImpresion(impresion.id);
            runOnUiThread(() -> {
                Toast.makeText(this, "Impresión diagnóstica eliminada", Toast.LENGTH_SHORT).show();
                cargarImpresiones();
            });
        }).start();
    }

    class ImpresionAdapter extends RecyclerView.Adapter<ImpresionAdapter.ViewHolder> {
        private final List<ImpresionDiagnostica> list = new ArrayList<>();

        void setImpresiones(List<ImpresionDiagnostica> newList) {
            list.clear();
            if (newList != null) {
                list.addAll(newList);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_impresion, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImpresionDiagnostica imp = list.get(position);
            holder.bind(imp);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView paciente, fecha, diagnostico;
            Button btnPdf, btnEliminar;

            ViewHolder(View view) {
                super(view);
                paciente = view.findViewById(R.id.text_impresion_paciente);
                fecha = view.findViewById(R.id.text_impresion_fecha);
                diagnostico = view.findViewById(R.id.text_impresion_diagnostico);
                btnPdf = view.findViewById(R.id.btn_ver_pdf_impresion);
                btnEliminar = view.findViewById(R.id.btn_eliminar_impresion);
            }

            void bind(ImpresionDiagnostica imp) {
                String nomClaro = CryptoHelper.descifrar(imp.paciente_nombre);
                String diagClaro = CryptoHelper.descifrar(imp.diagnostico);

                paciente.setText(nomClaro);
                fecha.setText(imp.fecha_creacion);
                diagnostico.setText("Impresión: " + (diagClaro != null && !diagClaro.isEmpty() ? diagClaro : "Consulta General"));

                btnPdf.setOnClickListener(v -> verPdfImpresion(imp));
                btnEliminar.setOnClickListener(v -> eliminarImpresion(imp));
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
