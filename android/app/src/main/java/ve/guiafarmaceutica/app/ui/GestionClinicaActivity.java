package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.data.ImpresionDiagnostica;
import ve.guiafarmaceutica.app.data.Paciente;
import ve.guiafarmaceutica.app.repository.SettingsRepository;
import ve.guiafarmaceutica.app.util.CryptoHelper;
import ve.guiafarmaceutica.app.util.DemoDataHelper;

public class GestionClinicaActivity extends AppCompatActivity {

    private RecyclerView recyclerPacientes;
    private TextView textEmptyPacientes;
    private PacienteRecienteAdapter adapter;
    private final List<Paciente> listaPacientes = new ArrayList<>();

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
        cargarPacientesRecientes();
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        SettingsRepository settings = new SettingsRepository(this);
        TextView textHeaderClinica = findViewById(R.id.text_header_clinica);
        if (textHeaderClinica != null) {
            textHeaderClinica.setText(settings.getMedicoClinica());
        }

        View btnNuevo = findViewById(R.id.btn_nuevo_paciente);
        if (btnNuevo != null) {
            btnNuevo.setOnClickListener(v -> {
                Intent intent = new Intent(this, PerfilPacienteActivity.class);
                intent.putExtra(PerfilPacienteActivity.EXTRA_PACIENTE_ID, -1L);
                startActivity(intent);
            });
        }

        recyclerPacientes = findViewById(R.id.recycler_pacientes_recientes);
        textEmptyPacientes = findViewById(R.id.text_empty_pacientes);

        recyclerPacientes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PacienteRecienteAdapter(listaPacientes, p -> {
            Intent intent = new Intent(this, PerfilPacienteActivity.class);
            intent.putExtra(PerfilPacienteActivity.EXTRA_PACIENTE_ID, p.id);
            startActivity(intent);
        });
        recyclerPacientes.setAdapter(adapter);

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

    private void cargarPacientesRecientes() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.obtener(this);

            // Sincronizar pacientes desde impresiones existentes para asegurar que todos los pacientes del historial aparezcan
            try {
                List<ImpresionDiagnostica> impresiones = db.impresionDao().listarImpresiones();
                if (impresiones != null) {
                    for (ImpresionDiagnostica imp : impresiones) {
                        String nom = CryptoHelper.descifrar(imp.paciente_nombre);
                        String ced = CryptoHelper.descifrar(imp.paciente_cedula);
                        if (nom != null && !nom.isEmpty() && !"María López".equalsIgnoreCase(nom)) {
                            Paciente existente = null;
                            if (ced != null && !ced.isEmpty() && !"DEMO-10482".equalsIgnoreCase(ced)) {
                                existente = db.pacienteDao().obtenerPorIdentificacion(ced);
                            } else {
                                List<Paciente> porNombre = db.pacienteDao().buscarPacientes(nom);
                                if (porNombre != null && !porNombre.isEmpty()) {
                                    existente = porNombre.get(0);
                                }
                            }
                            if (existente == null) {
                                Paciente p = new Paciente();
                                p.nombre_completo = nom;
                                p.identificacion = (ced != null && !ced.isEmpty()) ? ced : ("ID-" + (System.currentTimeMillis() % 100000));
                                p.edad_calculada = imp.paciente_edad != null ? imp.paciente_edad : "S/I";
                                db.pacienteDao().insertar(p);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            List<Paciente> lista = db.pacienteDao().listarPacientes();
            List<Paciente> ultimos5 = new ArrayList<>();
            if (lista != null) {
                int limit = Math.min(5, lista.size());
                for (int i = 0; i < limit; i++) {
                    ultimos5.add(lista.get(i));
                }
            }
            runOnUiThread(() -> {
                listaPacientes.clear();
                listaPacientes.addAll(ultimos5);
                adapter.notifyDataSetChanged();
                if (textEmptyPacientes != null) {
                    textEmptyPacientes.setVisibility(listaPacientes.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }).start();
    }

    private void filtrarPacientes(String query) {
        new Thread(() -> {
            List<Paciente> resultados = AppDatabase.obtener(this).pacienteDao().buscarPacientes(query);
            runOnUiThread(() -> {
                listaPacientes.clear();
                if (resultados != null) {
                    int limit = Math.min(10, resultados.size());
                    for (int i = 0; i < limit; i++) {
                        listaPacientes.add(resultados.get(i));
                    }
                }
                adapter.notifyDataSetChanged();
                if (textEmptyPacientes != null) {
                    textEmptyPacientes.setVisibility(listaPacientes.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }).start();
    }

    static class PacienteRecienteAdapter extends RecyclerView.Adapter<PacienteRecienteAdapter.ViewHolder> {
        private final List<Paciente> pacientes;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(Paciente p);
        }

        PacienteRecienteAdapter(List<Paciente> pacientes, OnItemClickListener listener) {
            this.pacientes = pacientes;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_paciente_reciente, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Paciente p = pacientes.get(position);
            holder.bind(p, listener);
        }

        @Override
        public int getItemCount() {
            return pacientes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView avatar, nombre, detalles;

            ViewHolder(View view) {
                super(view);
                avatar = view.findViewById(R.id.text_avatar_iniciales);
                nombre = view.findViewById(R.id.text_paciente_nombre_item);
                detalles = view.findViewById(R.id.text_paciente_detalles_item);
            }

            void bind(Paciente p, OnItemClickListener listener) {
                nombre.setText(p.nombre_completo != null ? p.nombre_completo : "Sin Nombre");
                String iniciales = "PA";
                if (p.nombre_completo != null && !p.nombre_completo.trim().isEmpty()) {
                    String[] parts = p.nombre_completo.trim().split("\\s+");
                    if (parts.length >= 2) {
                        iniciales = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
                    } else if (parts[0].length() >= 2) {
                        iniciales = parts[0].substring(0, 2).toUpperCase();
                    }
                }
                avatar.setText(iniciales);
                detalles.setText("ID: " + (p.identificacion != null ? p.identificacion : "S/I") + " · Edad: " + (p.edad_calculada != null ? p.edad_calculada : "S/I"));
                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onItemClick(p);
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
