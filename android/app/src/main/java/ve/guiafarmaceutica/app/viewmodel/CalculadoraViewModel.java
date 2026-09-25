package ve.guiafarmaceutica.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ve.guiafarmaceutica.app.data.Monografia;
import ve.guiafarmaceutica.app.data.ReglaDosificacion;
import ve.guiafarmaceutica.app.data.ResultadoCalculo;
import ve.guiafarmaceutica.app.repository.MedicamentoRepository;
import ve.guiafarmaceutica.app.util.PosologiaParser;
import ve.guiafarmaceutica.app.util.ValidadorDosificacion;

public class CalculadoraViewModel extends AndroidViewModel {

    private final MedicamentoRepository repository;
    private final MutableLiveData<ReglaDosificacion> regla = new MutableLiveData<>();
    private final MutableLiveData<ResultadoCalculo> resultadoPeso = new MutableLiveData<>();
    private final MutableLiveData<Double> resultadoBSA = new MutableLiveData<>();
    private final MutableLiveData<Double> resultadoGoteo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);

    public CalculadoraViewModel(@NonNull Application application) {
        super(application);
        repository = new MedicamentoRepository(application);
    }

    public LiveData<ReglaDosificacion> getRegla() {
        return regla;
    }

    public LiveData<ResultadoCalculo> getResultadoPeso() {
        return resultadoPeso;
    }

    public LiveData<Double> getResultadoBSA() {
        return resultadoBSA;
    }

    public LiveData<Double> getResultadoGoteo() {
        return resultadoGoteo;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public void cargarRegla(String atcCodigo) {
        if (atcCodigo == null || atcCodigo.isEmpty()) {
            return;
        }
        cargando.setValue(true);
        repository.reglaDosificacion(atcCodigo, datos -> {
            if (datos != null && !datos.isEmpty() && datos.get(0).dosis_recomendada_mg_kg != null) {
                regla.setValue(datos.get(0));
                cargando.setValue(false);
            } else {
                // Nivel 2: Consultar Monografía oficial por ATC
                ReglaDosificacion base = (datos != null && !datos.isEmpty()) ? datos.get(0) : new ReglaDosificacion();
                base.atc_codigo = atcCodigo;

                repository.monografia(atcCodigo, listaMono -> {
                    if (listaMono != null && !listaMono.isEmpty()) {
                        Monografia m = listaMono.get(0);
                        String posologíaText = m.indicaciones_posologia != null ? m.indicaciones_posologia : m.posologia;
                        Double dosisExtraida = PosologiaParser.extraerDosisMgKg(posologíaText);
                        if (dosisExtraida != null) {
                            base.dosis_recomendada_mg_kg = dosisExtraida;
                        }
                    }
                    regla.setValue(base);
                    cargando.setValue(false);
                });
            }
        });
    }

    public void limpiarResultados() {
        resultadoPeso.setValue(null);
        resultadoBSA.setValue(null);
        resultadoGoteo.setValue(null);
    }

    public void calcularPorPeso(double pesoKg, double dosisMgKg) {
        ReglaDosificacion r = regla.getValue();
        ResultadoCalculo res = ValidadorDosificacion.calcularPorPeso(pesoKg, dosisMgKg, r);
        resultadoPeso.setValue(res);
    }

    public void calcularPorPeso(double pesoKg, double dosisMgKg, double concMg, double volMl) {
        ReglaDosificacion r = regla.getValue();
        ResultadoCalculo res = ValidadorDosificacion.calcularPorPeso(pesoKg, dosisMgKg, concMg, volMl, r);
        resultadoPeso.setValue(res);
    }

    public void calcularBSA(double pesoKg, double alturaCm) {
        double bsa = ValidadorDosificacion.calcularBSA(pesoKg, alturaCm);
        resultadoBSA.setValue(bsa);
    }

    public void calcularGoteoIv(double volumenMl, double tiempoHoras, boolean esMicrogoteo) {
        double goteo = ValidadorDosificacion.calcularGoteoIv(volumenMl, tiempoHoras, esMicrogoteo);
        resultadoGoteo.setValue(goteo);
    }
}
