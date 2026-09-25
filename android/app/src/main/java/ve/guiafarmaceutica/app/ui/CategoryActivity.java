package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.CategoryItem;
import ve.guiafarmaceutica.app.data.FichaClinica;
import ve.guiafarmaceutica.app.viewmodel.CategoryViewModel;

public class CategoryActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "category_type";
    public static final String EXTRA_TITLE = "category_title";

    private CategoryViewModel viewModel;
    private CategoryItemAdapter groupAdapter;
    private MedicamentoAdapter medAdapter;

    private TextView textTitle, textSubtitle, textEmpty;
    private ProgressBar progressBar;
    private RecyclerView recyclerGroups, recyclerMedicamentos;
    private View containerAyuda;

    private String categoryType;
    private String currentTitle;
    private boolean mostrandoMedicamentos = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        categoryType = getIntent().getStringExtra(EXTRA_TYPE);
        currentTitle = getIntent().getStringExtra(EXTRA_TITLE);
        if (currentTitle == null) {
            currentTitle = "Categorías";
        }

        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        setupUI();
        observeViewModel();

        if (CategoryViewModel.TYPE_AYUDA.equalsIgnoreCase(categoryType)) {
            containerAyuda.setVisibility(View.VISIBLE);
            recyclerGroups.setVisibility(View.GONE);
            recyclerMedicamentos.setVisibility(View.GONE);
            textEmpty.setVisibility(View.GONE);
        } else {
            viewModel.cargarCategoria(categoryType);
        }
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        textTitle = findViewById(R.id.text_category_title);
        textSubtitle = findViewById(R.id.text_category_subtitle);
        textEmpty = findViewById(R.id.text_empty);
        progressBar = findViewById(R.id.progress_loading);

        textTitle.setText(currentTitle);
        textSubtitle.setText("Explorar por " + currentTitle);

        recyclerGroups = findViewById(R.id.recycler_groups);
        recyclerMedicamentos = findViewById(R.id.recycler_medicamentos);
        containerAyuda = findViewById(R.id.container_ayuda);

        groupAdapter = new CategoryItemAdapter(new CategoryItemAdapter.OnCategoryItemClickListener() {
            @Override
            public void onItemClick(CategoryItem item) {
                explorarFiltro(item);
            }
        });

        recyclerGroups.setLayoutManager(new LinearLayoutManager(this));
        recyclerGroups.setAdapter(groupAdapter);

        medAdapter = new MedicamentoAdapter(new MedicamentoAdapter.OnMedicamentoClickListener() {
            @Override
            public void onClick(FichaClinica medicamento) {
                abrirFicha(medicamento);
            }

            @Override
            public void onMonografiaClick(FichaClinica medicamento) {
                abrirFicha(medicamento);
            }

            @Override
            public void onCalcularClick(FichaClinica medicamento) {
                abrirCalculadora(medicamento);
            }
        });

        recyclerMedicamentos.setLayoutManager(new LinearLayoutManager(this));
        recyclerMedicamentos.setAdapter(medAdapter);
    }

    private void observeViewModel() {
        viewModel.getItems().observe(this, items -> {
            if (!mostrandoMedicamentos) {
                groupAdapter.setItems(items);
                textEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getMedicamentos().observe(this, medicamentos -> {
            if (mostrandoMedicamentos) {
                medAdapter.submitList(medicamentos);
                textEmpty.setVisibility(medicamentos.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getCargando().observe(this, cargando -> {
            progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        });
    }

    private void explorarFiltro(CategoryItem item) {
        mostrandoMedicamentos = true;
        textTitle.setText(item.titulo);
        textSubtitle.setText(item.subtitulo);

        recyclerGroups.setVisibility(View.GONE);
        recyclerMedicamentos.setVisibility(View.VISIBLE);

        viewModel.cargarMedicamentosFiltrados(categoryType, item.id);
    }

    private void abrirFicha(FichaClinica med) {
        Intent intent = new Intent(this, FichaActivity.class);
        intent.putExtra(FichaActivity.EXTRA_ID, med.id);
        intent.putExtra(FichaActivity.EXTRA_NOMBRE, med.nombre);
        intent.putExtra(FichaActivity.EXTRA_LAB, med.laboratorio);
        intent.putExtra(FichaActivity.EXTRA_ATC, med.atc_codigo);
        startActivity(intent);
    }

    private void abrirCalculadora(FichaClinica med) {
        Intent intent = new Intent(this, CalculadoraActivity.class);
        intent.putExtra(CalculadoraActivity.EXTRA_ATC, med.atc_codigo);
        intent.putExtra(CalculadoraActivity.EXTRA_NOMBRE, med.nombre);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (mostrandoMedicamentos) {
            mostrandoMedicamentos = false;
            recyclerMedicamentos.setVisibility(View.GONE);
            recyclerGroups.setVisibility(View.VISIBLE);
            textTitle.setText(currentTitle);
            textSubtitle.setText("Explorar por " + currentTitle);
            viewModel.cargarCategoria(categoryType);
            return;
        }
        super.onBackPressed();
    }
}
