package ve.guiafarmaceutica.app.ui;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.Monografia;
import ve.guiafarmaceutica.app.viewmodel.FichaViewModel;

public class FichaActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "medicamento_id";
    public static final String EXTRA_NOMBRE = "medicamento_nombre";
    public static final String EXTRA_LAB = "medicamento_lab";
    public static final String EXTRA_ATC = "atc_codigo";

    private FichaViewModel viewModel;
    private LinearLayout seccionesContainer;
    private ProgressBar progressBar;
    private TextView textNoMonografia;
    private TextView textEmbarazo, textLactancia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ficha);

        String id = getIntent().getStringExtra(EXTRA_ID);
        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE);
        String lab = getIntent().getStringExtra(EXTRA_LAB);
        String atc = getIntent().getStringExtra(EXTRA_ATC);

        setupBackButton();

        ((TextView) findViewById(R.id.ficha_nombre)).setText(nombre);
        ((TextView) findViewById(R.id.ficha_laboratorio)).setText(lab);

        seccionesContainer = findViewById(R.id.secciones_container);
        progressBar = findViewById(R.id.ficha_progress);
        textNoMonografia = findViewById(R.id.text_no_monografia);
        textEmbarazo = findViewById(R.id.text_seguridad_embarazo);
        textLactancia = findViewById(R.id.text_seguridad_lactancia);

        viewModel = new ViewModelProvider(this).get(FichaViewModel.class);
        observeViewModel();

        viewModel.cargarDatos(id, atc);
    }

    private void setupBackButton() {
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
    }

    private void observeViewModel() {
        viewModel.getMonografia().observe(this, mono -> {
            if (mono != null) {
                renderMonografia(mono);
            } else {
                textNoMonografia.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getBanderas().observe(this, banderas -> {
            if (banderas != null) {
                actualizarBanderas(banderas.embarazo, banderas.lactancia);
            }
        });

        viewModel.getCargando().observe(this, cargando -> {
            progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        });
    }

    private void actualizarBanderas(String embarazo, String lactancia) {
        textEmbarazo.setText(embarazo != null ? embarazo : "Sin dato");
        textLactancia.setText(lactancia != null ? lactancia : "Sin dato");

        int colorAlerta = ContextCompat.getColor(this, R.color.alerta_embarazo);
        int colorAviso = ContextCompat.getColor(this, R.color.aviso_lactancia);
        int colorOk = ContextCompat.getColor(this, R.color.verde_farmacia);

        if ("Contraindicado".equalsIgnoreCase(embarazo)) {
            textEmbarazo.setTextColor(colorAlerta);
        } else if ("Precaución".equalsIgnoreCase(embarazo) || "Evaluar riesgo/beneficio".equalsIgnoreCase(embarazo)) {
            textEmbarazo.setTextColor(colorAviso);
        } else {
            textEmbarazo.setTextColor(colorOk);
        }

        if ("evitar".equalsIgnoreCase(lactancia)) {
            textLactancia.setTextColor(colorAlerta);
        } else if ("precaución".equalsIgnoreCase(lactancia)) {
            textLactancia.setTextColor(colorAviso);
        } else {
            textLactancia.setTextColor(colorOk);
        }
    }

    private void renderMonografia(Monografia m) {
        textNoMonografia.setVisibility(View.GONE);

        // Limpiar secciones previas si las hubiera (en caso de recarga)
        // El container ahora tiene: 0: med_box, 1: card_seguridad, 2: progress, 3: no_mono
        int childCount = seccionesContainer.getChildCount();
        if (childCount > 4) {
            seccionesContainer.removeViews(4, childCount - 4);
        }

        addSeccion("Mecanismo de acción", m.mecanismo_accion);
        addSeccion("Indicaciones terapéuticas", m.indicaciones_terapeuticas);
        addSeccion("Posología", m.posologia);
        addSeccion("Modo de administración", m.modo_administracion);
        addSeccion("Contraindicaciones", m.contraindicaciones);
        addSeccion("Advertencias y precauciones", m.advertencias_precauciones);
        addSeccion("Insuficiencia hepática", m.insuficiencia_hepatica);
        addSeccion("Insuficiencia renal", m.insuficiencia_renal);
        addSeccion("Interacciones", m.interacciones);
        addSeccion("Embarazo", m.embarazo_texto);
        addSeccion("Lactancia", m.lactancia_texto);
        addSeccion("Efectos sobre la conducción", m.conduccion);
        addSeccion("Reacciones adversas", m.reacciones_adversas);
        addSeccion("Sobredosificación", m.sobredosificacion);
    }

    private void addSeccion(String titulo, String contenido) {
        if (contenido == null || contenido.trim().isEmpty() || "null".equalsIgnoreCase(contenido)) {
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.item_ficha_seccion, seccionesContainer, false);
        ((TextView) view.findViewById(R.id.seccion_titulo)).setText(titulo.toUpperCase());

        // La BD tiene restos de HTML, los saneamos
        String limpio = contenido.replace("&nbsp;", " ").replace("<exp>", " ").replace("</exp>", " ");
        ((TextView) view.findViewById(R.id.seccion_contenido)).setText(Html.fromHtml(limpio, Html.FROM_HTML_MODE_COMPACT));

        seccionesContainer.addView(view);
    }
}
