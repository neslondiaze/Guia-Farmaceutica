package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Locale;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.ResultadoCalculo;
import ve.guiafarmaceutica.app.util.PresentacionParser;
import ve.guiafarmaceutica.app.util.ValidadorDosificacion;
import ve.guiafarmaceutica.app.viewmodel.CalculadoraViewModel;

public class CalculadoraActivity extends AppCompatActivity {

    public static final String EXTRA_ATC = "extra_atc";
    public static final String EXTRA_NOMBRE = "extra_nombre";

    private CalculadoraViewModel viewModel;

    private TextView textMedNombre, textMedSub;
    private View layoutPesoContainer, layoutPresentacionContainer;
    private TextInputLayout layoutPeso, layoutDosisMgKg, layoutAltura, layoutVolumenIv, layoutTiempoIv, layoutConcMg, layoutVolMl;
    private TextInputEditText editPeso, editDosisMgKg, editAltura, editVolumenIv, editTiempoIv, editConcMg, editVolMl;
    private MaterialButtonToggleGroup toggleUnidadPeso;
    private MaterialSwitch switchMicrogoteo;
    private Button btnCalcular, btnFormulasClasicas;

    private MaterialCardView cardSemaforo;
    private TextView textResultadoPrincipal, textResultadoSecundario, textDesgloseFormula, textAlertaSemaforo;

    private int tabSeleccionado = 0; // 0: Peso, 1: IV, 2: BSA
    private boolean esPesoEnLibras = false;
    private double dosisRecomendadaPediatrica = 15.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculadora);

        String atc = getIntent().getStringExtra(EXTRA_ATC);
        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE);

        viewModel = new ViewModelProvider(this).get(CalculadoraViewModel.class);

        setupUI();
        observeViewModel();

        if (nombre != null && !nombre.isEmpty()) {
            textMedNombre.setText(nombre);
            textMedSub.setText("Ajuste de dosis para " + (atc != null ? atc : ""));

            // Auto-llenado de Concentración (mg) y Volumen (ml) desde el nombre comercial
            PresentacionParser.DatosPresentacion dp =
                    PresentacionParser.extraer(nombre);
            if (dp.encontrado) {
                if (editConcMg != null) editConcMg.setText(String.format(Locale.US, "%.0f", dp.concentracionMg));
                if (editVolMl != null) editVolMl.setText(String.format(Locale.US, "%.0f", dp.volumenMl));
            }
        }

        if (atc != null && !atc.isEmpty()) {
            viewModel.cargarRegla(atc);
        }
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        textMedNombre = findViewById(R.id.calc_med_nombre);
        textMedSub = findViewById(R.id.calc_med_sub);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        layoutPesoContainer = findViewById(R.id.layout_peso_container);
        layoutPresentacionContainer = findViewById(R.id.layout_presentacion_container);
        layoutPeso = findViewById(R.id.layout_peso);
        layoutDosisMgKg = findViewById(R.id.layout_dosis_mg_kg);
        layoutConcMg = findViewById(R.id.layout_conc_mg);
        layoutVolMl = findViewById(R.id.layout_vol_ml);
        layoutAltura = findViewById(R.id.layout_altura);
        layoutVolumenIv = findViewById(R.id.layout_volumen_iv);
        layoutTiempoIv = findViewById(R.id.layout_tiempo_iv);

        editPeso = findViewById(R.id.edit_peso);
        editDosisMgKg = findViewById(R.id.edit_dosis_mg_kg);
        editConcMg = findViewById(R.id.edit_conc_mg);
        editVolMl = findViewById(R.id.edit_vol_ml);
        editAltura = findViewById(R.id.edit_altura);
        editVolumenIv = findViewById(R.id.edit_volumen_iv);
        editTiempoIv = findViewById(R.id.edit_tiempo_iv);

        editPeso.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double pVal = Double.parseDouble(s.toString());
                    double pKg = esPesoEnLibras ? ValidadorDosificacion.librasAKilogramos(pVal) : pVal;
                    if (pKg >= 50.0) {
                        layoutDosisMgKg.setHint("Dosis fija de adulto (mg / toma)");
                        String actualDosis = editDosisMgKg.getText().toString();
                        if (actualDosis.isEmpty() || Double.parseDouble(actualDosis) < 50.0) {
                            editDosisMgKg.setText("500");
                        }
                    } else {
                        layoutDosisMgKg.setHint("Dosis indicada (mg/kg/toma)");
                        String actualDosis = editDosisMgKg.getText().toString();
                        if (actualDosis.isEmpty() || Double.parseDouble(actualDosis) >= 50.0) {
                            editDosisMgKg.setText(String.format(Locale.US, "%.1f", dosisRecomendadaPediatrica));
                        }
                    }
                } catch (Exception ignored) {
                    layoutDosisMgKg.setHint("Dosis indicada (mg/kg/toma)");
                    String actualDosis = editDosisMgKg.getText().toString();
                    if (actualDosis.isEmpty() || Double.parseDouble(actualDosis) >= 50.0) {
                        editDosisMgKg.setText(String.format(Locale.US, "%.1f", dosisRecomendadaPediatrica));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        toggleUnidadPeso = findViewById(R.id.toggle_unidad_peso);
        if (toggleUnidadPeso != null) {
            toggleUnidadPeso.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    esPesoEnLibras = (checkedId == R.id.btn_unit_lb);
                }
            });
        }

        switchMicrogoteo = findViewById(R.id.switch_microgoteo);
        btnCalcular = findViewById(R.id.btn_calcular);

        btnFormulasClasicas = findViewById(R.id.btn_formulas_clasicas);
        if (btnFormulasClasicas != null) {
            btnFormulasClasicas.setOnClickListener(v -> mostrarDialogFormulasHistoricas());
        }

        cardSemaforo = findViewById(R.id.card_resultado_semaforo);
        textResultadoPrincipal = findViewById(R.id.text_resultado_principal);
        textResultadoSecundario = findViewById(R.id.text_resultado_secundario);
        textDesgloseFormula = findViewById(R.id.text_desglose_formula);
        textAlertaSemaforo = findViewById(R.id.text_alerta_semaforo);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabSeleccionado = tab.getPosition();
                actualizarModoCalculo();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnCalcular.setOnClickListener(v -> ejecutarCalculo());

        View btnCrearImpresion = findViewById(R.id.btn_crear_impresion_calc);
        if (btnCrearImpresion != null) {
            btnCrearImpresion.setOnClickListener(v -> {
                Intent impressionIntent = new Intent(this, CrearImpresionActivity.class);
                impressionIntent.putExtra(CrearImpresionActivity.EXTRA_MED_NOMBRE, textMedNombre.getText().toString());
                impressionIntent.putExtra(CrearImpresionActivity.EXTRA_DOSIS, textResultadoPrincipal.getText().toString() + " (" + textResultadoSecundario.getText().toString() + ")");
                startActivity(impressionIntent);
            });
        }
    }

    private void actualizarModoCalculo() {
        if (viewModel != null) {
            viewModel.limpiarResultados();
        }
        if (cardSemaforo != null) {
            cardSemaforo.setVisibility(View.GONE);
        }

        if (tabSeleccionado == 0) {
            // Peso
            if (layoutPesoContainer != null) layoutPesoContainer.setVisibility(View.VISIBLE);
            if (layoutPresentacionContainer != null) layoutPresentacionContainer.setVisibility(View.VISIBLE);
            layoutDosisMgKg.setVisibility(View.VISIBLE);
            if (btnFormulasClasicas != null) btnFormulasClasicas.setVisibility(View.VISIBLE);

            layoutAltura.setVisibility(View.GONE);
            layoutVolumenIv.setVisibility(View.GONE);
            layoutTiempoIv.setVisibility(View.GONE);
            switchMicrogoteo.setVisibility(View.GONE);
        } else if (tabSeleccionado == 1) {
            // Goteo IV
            if (layoutPesoContainer != null) layoutPesoContainer.setVisibility(View.GONE);
            if (layoutPresentacionContainer != null) layoutPresentacionContainer.setVisibility(View.GONE);
            layoutDosisMgKg.setVisibility(View.GONE);
            if (btnFormulasClasicas != null) btnFormulasClasicas.setVisibility(View.GONE);

            layoutAltura.setVisibility(View.GONE);
            layoutVolumenIv.setVisibility(View.VISIBLE);
            layoutTiempoIv.setVisibility(View.VISIBLE);
            switchMicrogoteo.setVisibility(View.VISIBLE);
        } else {
            // BSA
            if (layoutPesoContainer != null) layoutPesoContainer.setVisibility(View.VISIBLE);
            if (layoutPresentacionContainer != null) layoutPresentacionContainer.setVisibility(View.GONE);
            layoutDosisMgKg.setVisibility(View.GONE);
            if (btnFormulasClasicas != null) btnFormulasClasicas.setVisibility(View.GONE);

            layoutAltura.setVisibility(View.VISIBLE);
            layoutVolumenIv.setVisibility(View.GONE);
            layoutTiempoIv.setVisibility(View.GONE);
            switchMicrogoteo.setVisibility(View.GONE);
        }
    }

    private void observeViewModel() {
        viewModel.getRegla().observe(this, regla -> {
            if (regla != null) {
                if (regla.dosis_recomendada_mg_kg != null && editDosisMgKg != null) {
                    editDosisMgKg.setText(String.format(Locale.US, "%.1f", regla.dosis_recomendada_mg_kg));
                    if (textMedSub != null && regla.atc_codigo != null && !regla.atc_codigo.isEmpty()) {
                        textMedSub.setText(String.format(Locale.US, "Dosis recomendada oficial: %.1f mg/kg (Monografía AEMPS %s)",
                                regla.dosis_recomendada_mg_kg, regla.atc_codigo));
                    }
                }
                if (regla.concentracion_mg != null && editConcMg != null) {
                    editConcMg.setText(String.format(Locale.US, "%.0f", regla.concentracion_mg));
                }
                if (regla.volumen_ml != null && editVolMl != null) {
                    editVolMl.setText(String.format(Locale.US, "%.0f", regla.volumen_ml));
                }
            }
        });

        viewModel.getResultadoPeso().observe(this, res -> {
            if (res == null) {
                cardSemaforo.setVisibility(View.GONE);
                return;
            }
            cardSemaforo.setVisibility(View.VISIBLE);
            textResultadoPrincipal.setText(String.format(Locale.US, "%.1f mg / toma", res.dosisMg));
            
            double gotas = ValidadorDosificacion.mlAGotas(res.volumenMl);
            textResultadoSecundario.setText(String.format(Locale.US, "Volumen: %.2f ml (equivale a %.0f gotas)", res.volumenMl, gotas));
            textAlertaSemaforo.setText(res.mensajeAlerta);

            // Desglose paso a paso de la fórmula
            try {
                double pesoEntrada = Double.parseDouble(editPeso.getText().toString());
                double pesoKg = esPesoEnLibras ? ValidadorDosificacion.librasAKilogramos(pesoEntrada) : pesoEntrada;
                double dosis = Double.parseDouble(editDosisMgKg.getText().toString());
                double conc = Double.parseDouble(editConcMg.getText().toString());
                double vol = Double.parseDouble(editVolMl.getText().toString());

                String desglose;
                if (pesoKg >= 50.0) {
                    desglose = String.format(Locale.US,
                            "🏷️ Rango Adulto/Adolescente (≥50 kg):\n" +
                            "1. Dosis Bruta por Peso = %.1f kg × %.1f mg/kg = %.1f mg (Ajustada a Tope Adulto: %.1f mg)\n" +
                            "2. Regla de Tres (ml) = (%.1f mg × %.1f ml) / %.1f mg = %.2f ml",
                            pesoKg, dosis, (pesoKg * dosis), res.dosisMg,
                            res.dosisMg, vol, conc, res.volumenMl);
                } else {
                    desglose = String.format(Locale.US,
                            "🏷️ Rango Pediátrico (<50 kg):\n" +
                            "1. Dosis Total (mg) = %.1f kg × %.1f mg/kg = %.1f mg\n" +
                            "2. Regla de Tres (ml) = (%.1f mg × %.1f ml) / %.1f mg = %.2f ml",
                            pesoKg, dosis, res.dosisMg,
                            res.dosisMg, vol, conc, res.volumenMl);
                }

                if (textDesgloseFormula != null) {
                    textDesgloseFormula.setText(desglose);
                    textDesgloseFormula.setVisibility(View.VISIBLE);
                }
            } catch (Exception ignored) {
                if (textDesgloseFormula != null) textDesgloseFormula.setVisibility(View.GONE);
            }

            if (res.estado == ResultadoCalculo.EstadoSemaforo.VERDE) {
                cardSemaforo.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                cardSemaforo.setStrokeColor(Color.parseColor("#2E7D32"));
                textAlertaSemaforo.setTextColor(Color.parseColor("#2E7D32"));
            } else if (res.estado == ResultadoCalculo.EstadoSemaforo.NARANJA) {
                cardSemaforo.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
                cardSemaforo.setStrokeColor(Color.parseColor("#EF6C00"));
                textAlertaSemaforo.setTextColor(Color.parseColor("#EF6C00"));
            } else {
                cardSemaforo.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
                cardSemaforo.setStrokeColor(Color.parseColor("#C62828"));
                textAlertaSemaforo.setTextColor(Color.parseColor("#C62828"));
            }
        });

        viewModel.getResultadoBSA().observe(this, bsa -> {
            if (bsa == null) {
                cardSemaforo.setVisibility(View.GONE);
                return;
            }
            if (textDesgloseFormula != null) textDesgloseFormula.setVisibility(View.GONE);
            cardSemaforo.setVisibility(View.VISIBLE);
            textResultadoPrincipal.setText(String.format(Locale.US, "%.2f m²", bsa));
            textResultadoSecundario.setText("Superficie Corporal (BSA Mosteller)");
            textAlertaSemaforo.setText("Superficie corporal calculada para ajuste quimioterápico/crítico.");
            cardSemaforo.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            cardSemaforo.setStrokeColor(Color.parseColor("#2E7D32"));
            textAlertaSemaforo.setTextColor(Color.parseColor("#2E7D32"));
        });

        viewModel.getResultadoGoteo().observe(this, goteo -> {
            if (goteo == null) {
                cardSemaforo.setVisibility(View.GONE);
                return;
            }
            if (textDesgloseFormula != null) textDesgloseFormula.setVisibility(View.GONE);
            cardSemaforo.setVisibility(View.VISIBLE);
            boolean esMicro = switchMicrogoteo.isChecked();
            textResultadoPrincipal.setText(String.format(Locale.US, "%.1f %s/min", goteo, esMicro ? "microgotas" : "gotas"));
            textResultadoSecundario.setText("Velocidad de Infusión Intravenosa");
            textAlertaSemaforo.setText("Velocidad calculada para infusión continua.");
            cardSemaforo.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            cardSemaforo.setStrokeColor(Color.parseColor("#2E7D32"));
            textAlertaSemaforo.setTextColor(Color.parseColor("#2E7D32"));
        });
    }

    private void ocultarTeclado() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void ejecutarCalculo() {
        ocultarTeclado();
        try {
            if (tabSeleccionado == 0) {
                double pesoEntrada = Double.parseDouble(editPeso.getText().toString());
                double pesoKg = esPesoEnLibras ? ValidadorDosificacion.librasAKilogramos(pesoEntrada) : pesoEntrada;
                double dosis = Double.parseDouble(editDosisMgKg.getText().toString());
                double conc = Double.parseDouble(editConcMg.getText().toString());
                double vol = Double.parseDouble(editVolMl.getText().toString());

                viewModel.calcularPorPeso(pesoKg, dosis, conc, vol);
            } else if (tabSeleccionado == 1) {
                double vol = Double.parseDouble(editVolumenIv.getText().toString());
                double tiempo = Double.parseDouble(editTiempoIv.getText().toString());
                viewModel.calcularGoteoIv(vol, tiempo, switchMicrogoteo.isChecked());
            } else {
                double pesoEntrada = Double.parseDouble(editPeso.getText().toString());
                double pesoKg = esPesoEnLibras ? ValidadorDosificacion.librasAKilogramos(pesoEntrada) : pesoEntrada;
                double altura = Double.parseDouble(editAltura.getText().toString());
                viewModel.calcularBSA(pesoKg, altura);
            }
        } catch (Exception e) {
            cardSemaforo.setVisibility(View.VISIBLE);
            if (textDesgloseFormula != null) textDesgloseFormula.setVisibility(View.GONE);
            textResultadoPrincipal.setText("Error en datos");
            textResultadoSecundario.setText("Sintaxis de entrada");
            textAlertaSemaforo.setText("Por favor ingrese valores numéricos válidos en los campos.");
            cardSemaforo.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            cardSemaforo.setStrokeColor(Color.parseColor("#EF6C00"));
            textAlertaSemaforo.setTextColor(Color.parseColor("#EF6C00"));
        }
    }

    private void mostrarDialogFormulasHistoricas() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_formulas_historicas, null);

        TextInputEditText editDosisAdulto = view.findViewById(R.id.dialog_dosis_adulto);
        TextInputEditText editFriedMeses = view.findViewById(R.id.dialog_fried_meses);
        TextView textFriedRes = view.findViewById(R.id.dialog_fried_resultado);

        TextInputEditText editYoungAnos = view.findViewById(R.id.dialog_young_anos);
        TextView textYoungRes = view.findViewById(R.id.dialog_young_resultado);

        TextInputEditText editClarkLibras = view.findViewById(R.id.dialog_clark_libras);
        TextView textClarkRes = view.findViewById(R.id.dialog_clark_resultado);

        Runnable recancularFormulas = () -> {
            try {
                double dosisAdulto = Double.parseDouble(editDosisAdulto.getText().toString());

                double meses = Double.parseDouble(editFriedMeses.getText().toString());
                double friedMg = ValidadorDosificacion.reglaDeFried(meses, dosisAdulto);
                textFriedRes.setText(String.format(Locale.US, "%.1f mg", friedMg));

                double anos = Double.parseDouble(editYoungAnos.getText().toString());
                double youngMg = ValidadorDosificacion.reglaDeYoung(anos, dosisAdulto);
                textYoungRes.setText(String.format(Locale.US, "%.1f mg", youngMg));

                double lbs = Double.parseDouble(editClarkLibras.getText().toString());
                double clarkMg = ValidadorDosificacion.reglaDeClark(lbs, dosisAdulto);
                textClarkRes.setText(String.format(Locale.US, "%.1f mg", clarkMg));
            } catch (Exception ignored) {}
        };

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recancularFormulas.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editDosisAdulto.addTextChangedListener(watcher);
        editFriedMeses.addTextChangedListener(watcher);
        editYoungAnos.addTextChangedListener(watcher);
        editClarkLibras.addTextChangedListener(watcher);

        recancularFormulas.run();

        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

