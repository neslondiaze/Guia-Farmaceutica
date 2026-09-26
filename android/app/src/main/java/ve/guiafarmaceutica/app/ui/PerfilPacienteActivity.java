package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.data.ImagenComplementaria;
import ve.guiafarmaceutica.app.data.Paciente;

public class PerfilPacienteActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";

    private long pacienteId = -1;
    private Paciente paciente;

    private TextView textHeaderTitle, textHeaderSub;
    private TextInputEditText editNombre, editId, editNac, editEdad;
    private AutoCompleteTextView editSexo;
    private TextInputEditText editTelefono, editCorreo, editDireccion, editEmergencia;
    private TextInputEditText editAlergias, editCondiciones, editGrupoSang, editPeso, editAltura, editNotas;

    private RecyclerView recyclerImagenes;
    private TextView textEmptyImagenes;
    private ImagenComplementariaAdapter adapterImagenes;
    private final List<ImagenComplementaria> listaImagenes = new ArrayList<>();

    private Uri tempCameraUri;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    procesarImagenSeleccionada(uri);
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            exito -> {
                if (exito && tempCameraUri != null) {
                    procesarImagenSeleccionada(tempCameraUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_paciente);

        pacienteId = getIntent().getLongExtra(EXTRA_PACIENTE_ID, -1);

        setupUI();
        cargarDatosPaciente();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarImagenesComplementarias();
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

        if (editSexo != null) {
            String[] opcionesSexo = new String[]{"FEMENINO", "MASCULINO", "OTR@"};
            ArrayAdapter<String> adapterSexo = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, opcionesSexo);
            editSexo.setAdapter(adapterSexo);
        }

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

        recyclerImagenes = findViewById(R.id.recycler_imagenes_complementarias);
        textEmptyImagenes = findViewById(R.id.text_empty_imagenes_comp);

        if (recyclerImagenes != null) {
            recyclerImagenes.setLayoutManager(new LinearLayoutManager(this));
            adapterImagenes = new ImagenComplementariaAdapter(listaImagenes, new ImagenComplementariaAdapter.OnItemClickListener() {
                @Override
                public void onEliminar(ImagenComplementaria img) {
                    eliminarImagenComplementaria(img);
                }

                @Override
                public void onClick(ImagenComplementaria img) {
                    mostrarImagenAmpliada(img);
                }
            });
            recyclerImagenes.setAdapter(adapterImagenes);
        }

        View btnAgregarImg = findViewById(R.id.btn_agregar_imagen_comp);
        if (btnAgregarImg != null) {
            btnAgregarImg.setOnClickListener(v -> abrirOpcionesImagen());
        }

        findViewById(R.id.btn_perfil_nueva_impresion).setOnClickListener(v -> {
            Intent intent = new Intent(this, CrearImpresionActivity.class);
            if (pacienteId != -1) {
                intent.putExtra("extra_paciente_id", pacienteId);
            }
            startActivity(intent);
        });

        findViewById(R.id.btn_perfil_historial).setOnClickListener(v -> guardarCambiosFicha());
    }

    private void mostrarImagenAmpliada(ImagenComplementaria img) {
        if (img.ruta_imagen == null) {
            Toast.makeText(this, "Ruta de imagen no disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(img.ruta_imagen);
        if (!file.exists()) {
            Toast.makeText(this, "El archivo de imagen no existe en el almacenamiento", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ver_imagen, null);
        ImageView imageView = dialogView.findViewById(R.id.img_dialog_ampliada);
        Glide.with(this).load(file).into(imageView);

        new AlertDialog.Builder(this)
                .setTitle(img.descripcion_datos != null ? img.descripcion_datos : "Estudio Complementario")
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void cargarDatosPaciente() {
        if (pacienteId == -1) {
            runOnUiThread(() -> {
                textHeaderTitle.setText("Registrar Nuevo Paciente");
                textHeaderSub.setText("EXPEDIENTE CLÍNICO");
            });
            return;
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.obtener(this);
            paciente = db.pacienteDao().obtenerPorId(pacienteId);

            if (paciente != null) {
                runOnUiThread(() -> {
                    textHeaderSub.setText("EXPEDIENTE CLÍNICO: " + paciente.nombre_completo.toUpperCase());
                    textHeaderTitle.setText(paciente.nombre_completo + " (" + paciente.identificacion + ")");

                    editNombre.setText(paciente.nombre_completo);
                    editId.setText(paciente.identificacion);
                    editNac.setText(paciente.fecha_nacimiento);
                    editEdad.setText(paciente.edad_calculada);
                    if (editSexo != null && paciente.sexo != null) {
                        editSexo.setText(paciente.sexo, false);
                    }

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

    private void cargarImagenesComplementarias() {
        if (pacienteId == -1) {
            return;
        }
        new Thread(() -> {
            List<ImagenComplementaria> lista = AppDatabase.obtener(this).imagenComplementariaDao().listarPorPaciente(pacienteId);
            runOnUiThread(() -> {
                listaImagenes.clear();
                if (lista != null) {
                    listaImagenes.addAll(lista);
                }
                if (adapterImagenes != null) {
                    adapterImagenes.notifyDataSetChanged();
                }
                if (textEmptyImagenes != null) {
                    textEmptyImagenes.setVisibility(listaImagenes.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }).start();
    }

    private void abrirOpcionesImagen() {
        if (pacienteId == -1) {
            Toast.makeText(this, "Guarde primero la ficha del paciente para adjuntar imágenes", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] opciones = new CharSequence[]{"📷 Tomar Foto (Cámara)", "🖼️ Elegir de Galería"};
        new AlertDialog.Builder(this)
                .setTitle("Agregar Imagen / Estudio")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        lanzarCamara();
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void lanzarCamara() {
        try {
            File photoDir = new File(getFilesDir(), "imagenes_pacientes");
            if (!photoDir.exists()) photoDir.mkdirs();
            File tempFile = new File(photoDir, "temp_cam_" + System.currentTimeMillis() + ".jpg");
            tempCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile);
            cameraLauncher.launch(tempCameraUri);
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir la cámara: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void procesarImagenSeleccionada(Uri uri) {
        EditText input = new EditText(this);
        input.setHint("Ej. Radiografía de tórax, Examen de sangre, Lesión dérmica...");
        input.setPadding(30, 20, 30, 20);

        new AlertDialog.Builder(this)
                .setTitle("Descripción del Estudio o Imagen")
                .setMessage("Ingrese notas o datos complementarios para esta imagen:")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nota = input.getText().toString().trim();
                    if (nota.isEmpty()) {
                        nota = "Estudio complementario";
                    }
                    guardarImagenEnBd(uri, nota);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarImagenEnBd(Uri uri, String descripcion) {
        new Thread(() -> {
            try {
                File dir = new File(getFilesDir(), "imagenes_pacientes");
                if (!dir.exists()) dir.mkdirs();

                File destFile = new File(dir, "img_" + System.currentTimeMillis() + ".jpg");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }

                ImagenComplementaria img = new ImagenComplementaria();
                img.paciente_id = pacienteId;
                img.ruta_imagen = destFile.getAbsolutePath();
                img.descripcion_datos = descripcion;
                img.fecha_registro = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

                AppDatabase.obtener(this).imagenComplementariaDao().insertar(img);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Imagen complementaria guardada exitosamente", Toast.LENGTH_SHORT).show();
                    cargarImagenesComplementarias();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al guardar imagen: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void eliminarImagenComplementaria(ImagenComplementaria img) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Imagen")
                .setMessage("¿Desea eliminar esta imagen complementaria de la ficha?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    new Thread(() -> {
                        AppDatabase.obtener(this).imagenComplementariaDao().eliminar(img.id);
                        if (img.ruta_imagen != null) {
                            try {
                                new File(img.ruta_imagen).delete();
                            } catch (Exception ignored) {}
                        }
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Imagen eliminada", Toast.LENGTH_SHORT).show();
                            cargarImagenesComplementarias();
                        });
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarCambiosFicha() {
        String nombre = editNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ingrese el nombre del paciente", Toast.LENGTH_SHORT).show();
            return;
        }

        if (paciente == null) {
            paciente = new Paciente();
        }

        paciente.nombre_completo = nombre;
        paciente.identificacion = editId.getText().toString().trim();
        paciente.fecha_nacimiento = editNac.getText().toString().trim();
        paciente.edad_calculada = editEdad.getText().toString().trim();
        paciente.sexo = editSexo != null ? editSexo.getText().toString().trim() : "";

        paciente.telefono = editTelefono.getText().toString().trim();
        paciente.correo = editCorreo.getText().toString().trim();
        paciente.direccion = editDireccion.getText().toString().trim();
        paciente.contacto_emergencia = editEmergencia.getText().toString().trim();

        paciente.alergias = editAlergias.getText().toString().trim();
        paciente.condiciones_relevantes = editCondiciones.getText().toString().trim();
        paciente.grupo_sanguineo = editGrupoSang.getText().toString().trim();
        paciente.peso = editPeso.getText().toString().trim();
        paciente.altura = editAltura.getText().toString().trim();
        paciente.notas_clinicas = editNotas.getText().toString().trim();

        new Thread(() -> {
            long idGuardado = AppDatabase.obtener(this).pacienteDao().insertar(paciente);
            paciente.id = idGuardado;
            pacienteId = idGuardado;
            runOnUiThread(() -> {
                Toast.makeText(this, "Ficha del paciente guardada exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    static class ImagenComplementariaAdapter extends RecyclerView.Adapter<ImagenComplementariaAdapter.ViewHolder> {
        private final List<ImagenComplementaria> lista;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onEliminar(ImagenComplementaria img);
            void onClick(ImagenComplementaria img);
        }

        ImagenComplementariaAdapter(List<ImagenComplementaria> lista, OnItemClickListener listener) {
            this.lista = lista;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_imagen_complementaria, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImagenComplementaria item = lista.get(position);
            holder.textDescripcion.setText(item.descripcion_datos != null ? item.descripcion_datos : "Estudio sin nota");
            holder.textFecha.setText("Fecha: " + (item.fecha_registro != null ? item.fecha_registro : "-"));

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(item);
            });

            holder.btnEliminar.setOnClickListener(v -> {
                if (listener != null) listener.onEliminar(item);
            });
        }

        @Override
        public int getItemCount() {
            return lista.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textDescripcion, textFecha;
            ImageButton btnEliminar;

            ViewHolder(View view) {
                super(view);
                textDescripcion = view.findViewById(R.id.text_imagen_descripcion);
                textFecha = view.findViewById(R.id.text_imagen_fecha);
                btnEliminar = view.findViewById(R.id.btn_eliminar_imagen_comp);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
