package ve.guiafarmaceutica.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import ve.guiafarmaceutica.app.data.BanderaSeguridad;
import ve.guiafarmaceutica.app.data.Monografia;
import ve.guiafarmaceutica.app.repository.MedicamentoRepository;

public class FichaViewModel extends AndroidViewModel {

    private final MedicamentoRepository repository;
    private final MutableLiveData<Monografia> monografia = new MutableLiveData<>();
    private final MutableLiveData<BanderaSeguridad> banderas = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);

    public FichaViewModel(@NonNull Application application) {
        super(application);
        this.repository = new MedicamentoRepository(application);
    }

    public LiveData<Monografia> getMonografia() {
        return monografia;
    }

    public LiveData<BanderaSeguridad> getBanderas() {
        return banderas;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public void cargarDatos(String medicamientoId, String atcCodigo) {
        cargando.setValue(true);
        
        // Cargar banderas de seguridad
        repository.banderas(medicamientoId, results -> {
            if (!results.isEmpty()) {
                banderas.setValue(results.get(0));
            }
        });

        // Cargar monografía
        repository.monografia(atcCodigo, results -> {
            if (!results.isEmpty()) {
                monografia.setValue(results.get(0));
            } else {
                monografia.setValue(null);
            }
            cargando.setValue(false);
        });
    }
}
