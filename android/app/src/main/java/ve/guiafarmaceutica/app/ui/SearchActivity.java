package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.FichaClinica;
import ve.guiafarmaceutica.app.viewmodel.SearchViewModel;

public class SearchActivity extends AppCompatActivity {

    private SearchViewModel viewModel;
    private MedicamentoAdapter adapter;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        setupUI();
        observeViewModel();
        viewModel.cargarCatalogoInicial();
    }

    private void setupUI() {
        SearchBar searchBar = findViewById(R.id.search_bar);
        SearchView searchView = findViewById(R.id.search_view);
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());
        progressBar = findViewById(R.id.progress_loading);
        textEmpty = findViewById(R.id.text_empty);

        RecyclerView recyclerResultados = findViewById(R.id.recycler_resultados);
        RecyclerView recyclerCatalogo = findViewById(R.id.recycler_catalogo);

        adapter = new MedicamentoAdapter(new MedicamentoAdapter.OnMedicamentoClickListener() {
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

        recyclerResultados.setLayoutManager(new LinearLayoutManager(this));
        recyclerResultados.setAdapter(adapter);

        recyclerCatalogo.setLayoutManager(new LinearLayoutManager(this));
        recyclerCatalogo.setAdapter(adapter);

        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            viewModel.buscar(searchView.getText().toString());
            return false;
        });

        searchView.addTransitionListener((view, previousState, newState) -> {
            if (newState == SearchView.TransitionState.HIDDEN) {
                viewModel.cargarCatalogoInicial();
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            } else if (newState == SearchView.TransitionState.SHOWN) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        });

        // Búsqueda en tiempo real
        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.buscar(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
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

    private void observeViewModel() {
        viewModel.getResultados().observe(this, medicamentos -> {
            adapter.submitList(medicamentos);
            textEmpty.setVisibility(medicamentos.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getCargando().observe(this, cargando -> {
            progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
