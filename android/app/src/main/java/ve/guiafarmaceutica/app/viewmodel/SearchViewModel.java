package ve.guiafarmaceutica.app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import ve.guiafarmaceutica.app.data.FichaClinica;
import ve.guiafarmaceutica.app.repository.MedicamentoRepository;

public class SearchViewModel extends AndroidViewModel {

    private final MedicamentoRepository repository;
    private final MutableLiveData<List<FichaClinica>> resultados = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnableBusqueda;

    public SearchViewModel(@NonNull Application application) {
        super(application);
        this.repository = new MedicamentoRepository(application);
    }

    public LiveData<List<FichaClinica>> getResultados() {
        return resultados;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public void buscar(String consulta) {
        if (runnableBusqueda != null) {
            handler.removeCallbacks(runnableBusqueda);
        }

        if (consulta == null || consulta.trim().isEmpty()) {
            resultados.setValue(new ArrayList<>());
            return;
        }

        cargando.setValue(true);
        runnableBusqueda = () -> repository.buscar(consulta, 50, 0, results -> {
            resultados.setValue(results);
            cargando.setValue(false);
        });

        handler.postDelayed(runnableBusqueda, 300);
    }

    public void cargarCatalogoInicial() {
        cargando.setValue(true);
        repository.listar(50, 0, results -> {
            resultados.setValue(results);
            cargando.setValue(false);
        });
    }
}
