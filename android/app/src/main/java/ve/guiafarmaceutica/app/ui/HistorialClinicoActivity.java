package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.data.ImpresionDiagnostica;
import ve.guiafarmaceutica.app.data.ImpresionDetalle;
import ve.guiafarmaceutica.app.data.IndicacionNoFarmacologica;
import ve.guiafarmaceutica.app.data.Paciente;
import ve.guiafarmaceutica.app.util.CryptoHelper;
import ve.guiafarmaceutica.app.util.ImpresionPdfHelper;

public class HistorialClinicoActivity extends AppCompatActivity {

    private long pacienteId = -1;
    private Paciente paciente;

    private ProgressBar progressBar;
    private TextView textEmpty;
    private RecyclerView recyclerView;
    private HistorialAdapter adapter;

    private String estadoFiltro = "Todos los estados";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_clinico);

        pacienteId = getIntent().getLongExtra("extra_paciente_id", -1);

        setupUI();
        cargarDatosPaciente();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarHistorial();
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        progressBar = findViewById(R.id.progress_loading);
        textEmpty = findViewById(R.id.text_empty_historial);
        recyclerView = findViewById(R.id.recycler_historial_clinico);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistorialAdapter();
        recyclerView.setAdapter(adapter);

        AutoCompleteTextView spinnerEstado = findViewById(R.id.spinner_filtro_estado);
        String[] estados = new String[]{"Todos los estados", "Emitido", "Borrador"};
        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, estados);
        spinnerEstado.setAdapter(estadoAdapter);

        spinnerEstado.setOnItemClickListener((parent, view, position, id) -> {
            estadoFiltro = estados[position];
            cargarHistorial();
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
                    TextView textHeaderSub = findViewById(R.id.text_historial_header_sub);
                    textHeaderSub.setText("FICHA DE " + paciente.nombre_completo.toUpperCase());
                });
            }
        }).start();
    }

    private void cargarHistorial() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            AppDatabase db = AppDatabase.obtener(this);
            List<HistorialItem> items = new ArrayList<>();

            // 1. Cargar Impresiones Diagnósticas
            List<ImpresionDiagnostica> impresiones = db.impresionDao().listarImpresiones();
            if (impresiones != null) {
                for (ImpresionDiagnostica imp : impresiones) {
                    if (pacienteId != -1 && imp.paciente_id != pacienteId) {
                        continue;
                    }
                    String est = imp.estado != null ? imp.estado : "Emitido";
                    if ("Todos los estados".equalsIgnoreCase(estadoFiltro) || est.equalsIgnoreCase(estadoFiltro)) {
                        HistorialItem item = new HistorialItem();
                        item.esImpresion = true;
                        item.impresion = imp;
                        String diag = CryptoHelper.descifrar(imp.diagnostico);
                        item.titulo = (diag != null && !diag.isEmpty()) ? diag : "Impresión Diagnóstica";
                        item.subtitulo = imp.fecha_creacion + " · " + est;
                        item.estado = est;
                        items.add(item);
                    }
                }
            }

            // 2. Cargar Indicaciones
            List<IndicacionNoFarmacologica> indicaciones = db.indicacionDao().listarIndicaciones();
            if (indicaciones != null) {
                for (IndicacionNoFarmacologica ind : indicaciones) {
                    if (pacienteId != -1 && ind.paciente_id != pacienteId) {
                        continue;
                    }
                    String est = ind.estado != null ? ind.estado : "Borrador";
                    if ("Todos los estados".equalsIgnoreCase(estadoFiltro) || est.equalsIgnoreCase(estadoFiltro)) {
                        HistorialItem item = new HistorialItem();
                        item.esImpresion = false;
                        item.indicacion = ind;
                        item.titulo = "Indicaciones no farmacológicas";
                        item.subtitulo = ind.fecha_creacion + " · " + est;
                        item.estado = est;
                        items.add(item);
                    }
                }
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                adapter.setItems(items);
                textEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void abrirDetalle(HistorialItem item) {
        new Thread(() -> {
            try {
                File pdfFile;
                if (item.esImpresion) {
                    List<ImpresionDetalle> detalles = AppDatabase.obtener(this).impresionDao().obtenerDetalles(item.impresion.id);
                    ImpresionDiagnostica impresionClara = new ImpresionDiagnostica();
                    impresionClara.id = item.impresion.id;
                    impresionClara.paciente_nombre = CryptoHelper.descifrar(item.impresion.paciente_nombre);
                    impresionClara.paciente_cedula = CryptoHelper.descifrar(item.impresion.paciente_cedula);
                    impresionClara.paciente_edad = item.impresion.paciente_edad;
                    impresionClara.diagnostico = CryptoHelper.descifrar(item.impresion.diagnostico);
                    impresionClara.observaciones = item.impresion.observaciones;
                    impresionClara.fecha_creacion = item.impresion.fecha_creacion;

                    pdfFile = ImpresionPdfHelper.generarPdfImpresion(this, impresionClara, detalles);
                } else {
                    pdfFile = ImpresionPdfHelper.generarPdfIndicacion(this, item.indicacion, paciente);
                }

                runOnUiThread(() -> {
                    Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Ver / Compartir Documento PDF"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al abrir documento: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    static class HistorialItem {
        boolean esImpresion;
        ImpresionDiagnostica impresion;
        IndicacionNoFarmacologica indicacion;
        String titulo;
        String subtitulo;
        String estado;
    }

    class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {
        private final List<HistorialItem> list = new ArrayList<>();

        void setItems(List<HistorialItem> newList) {
            list.clear();
            if (newList != null) {
                list.addAll(newList);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial_clinico, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistorialItem item = list.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iconTipo;
            TextView titulo, subtitulo, badgeEstado;

            ViewHolder(View view) {
                super(view);
                iconTipo = view.findViewById(R.id.icon_historial_tipo);
                titulo = view.findViewById(R.id.text_historial_titulo);
                subtitulo = view.findViewById(R.id.text_historial_subtitulo);
                badgeEstado = view.findViewById(R.id.badge_historial_estado);
            }

            void bind(HistorialItem item) {
                titulo.setText(item.titulo);
                subtitulo.setText(item.subtitulo);
                badgeEstado.setText(item.estado);

                iconTipo.setImageResource(R.drawable.ic_cat_monography);

                if ("Emitido".equalsIgnoreCase(item.estado)) {
                    badgeEstado.setTextColor(Color.parseColor("#15803D"));
                } else {
                    badgeEstado.setTextColor(Color.parseColor("#B45309"));
                }

                itemView.setOnClickListener(v -> abrirDetalle(item));
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
