package ve.guiafarmaceutica.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

import ve.guiafarmaceutica.app.data.Atc;
import ve.guiafarmaceutica.app.data.CategoryItem;
import ve.guiafarmaceutica.app.data.ConteoGrupo;
import ve.guiafarmaceutica.app.data.FichaClinica;
import ve.guiafarmaceutica.app.data.ResumenForma;
import ve.guiafarmaceutica.app.data.ResumenLaboratorio;
import ve.guiafarmaceutica.app.data.ResumenLetra;
import ve.guiafarmaceutica.app.data.ResumenVia;
import ve.guiafarmaceutica.app.repository.MedicamentoRepository;

public class CategoryViewModel extends AndroidViewModel {

    public static final String TYPE_ANATOMICA = "cat_anatomica";
    public static final String TYPE_GRUPO = "cat_grupo";
    public static final String TYPE_PRINCIPIO = "cat_principio";
    public static final String TYPE_VIA = "cat_via";
    public static final String TYPE_FORMA = "cat_forma";
    public static final String TYPE_LABORATORIO = "cat_laboratorio";
    public static final String TYPE_SEGURIDAD = "cat_seguridad";
    public static final String TYPE_MONOGRAFIA = "cat_monografia";
    public static final String TYPE_ALFABETICO = "cat_alfabetico";
    public static final String TYPE_AYUDA = "cat_ayuda";

    private final MedicamentoRepository repository;
    private final MutableLiveData<List<CategoryItem>> items = new MutableLiveData<>();
    private final MutableLiveData<List<FichaClinica>> medicamentos = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new MedicamentoRepository(application);
    }

    public LiveData<List<CategoryItem>> getItems() {
        return items;
    }

    public LiveData<List<FichaClinica>> getMedicamentos() {
        return medicamentos;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public void cargarCategoria(String type) {
        cargando.setValue(true);
        medicamentos.setValue(new ArrayList<>());
        
        if (TYPE_ANATOMICA.equalsIgnoreCase(type)) {
            repository.clasesAnatomicas(datos -> {
                List<CategoryItem> list = new ArrayList<>();
                for (Atc atc : datos) {
                    list.add(new CategoryItem(atc.codigo, atc.codigo + " - " + atc.descripcion, "Clase Anatómica"));
                }
                items.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_GRUPO.equalsIgnoreCase(type)) {
            repository.topGruposTerapeuticos(100, datos -> {
                List<CategoryItem> list = new ArrayList<>();
                for (ConteoGrupo g : datos) {
                    list.add(new CategoryItem(g.grupo, "Grupo ATC: " + g.grupo, g.meds + " medicamentos"));
                }
                items.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_PRINCIPIO.equalsIgnoreCase(type)) {
            repository.principiosActivos(datos -> {
                List<CategoryItem> list = new ArrayList<>();
                for (Atc atc : datos) {
                    list.add(new CategoryItem(atc.codigo, atc.descripcion, "Código ATC: " + atc.codigo));
                }
                items.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_VIA.equalsIgnoreCase(type)) {
            repository.resumenVias(datos -> {
                List<CategoryItem> list = new ArrayList<>();
                for (ResumenVia via : datos) {
                    list.add(new CategoryItem(via.via, via.via, via.medicamentos + " presentaciones"));
                }
                items.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_FORMA.equalsIgnoreCase(type)) {
            repository.resumenFormas(datos -> {
                List<CategoryItem> list = new ArrayList<>();
                for (ResumenForma forma : datos) {
                    list.add(new CategoryItem(forma.forma, forma.forma, forma.medicamentos + " medicamentos"));
                }
                items.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_LABORATORIO.equalsIgnoreCase(type)) {
            repository.resumenLaboratorios(datos -> {
                List<CategoryItem> list = new ArrayList<>();
                for (ResumenLaboratorio lab : datos) {
                    list.add(new CategoryItem(lab.laboratorio, lab.laboratorio, lab.medicamentos + " productos registrados"));
                }
                items.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_SEGURIDAD.equalsIgnoreCase(type)) {
            List<CategoryItem> list = new ArrayList<>();
            list.add(new CategoryItem("Contraindicado", "Contraindicado en Embarazo", "Alto riesgo farmacológico"));
            list.add(new CategoryItem("Evaluar riesgo/beneficio", "Evaluar Riesgo / Beneficio", "Uso bajo supervisión médica"));
            list.add(new CategoryItem("Precaución", "Precaución en Lactancia / Embarazo", "Ver monografía antes de prescribir"));
            items.setValue(list);
            cargando.setValue(false);
        } else if (TYPE_ALFABETICO.equalsIgnoreCase(type)) {
            repository.resumenLetras(datos -> {
                List<CategoryItem> list = new ArrayList<>();
                for (ResumenLetra letra : datos) {
                    list.add(new CategoryItem(letra.letra, "Letra " + letra.letra, letra.medicamentos + " medicamentos"));
                }
                items.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_MONOGRAFIA.equalsIgnoreCase(type)) {
            repository.principiosActivos(datos -> {
                List<CategoryItem> list = new ArrayList<>();
                for (Atc atc : datos) {
                    list.add(new CategoryItem(atc.codigo, "Monografía: " + atc.descripcion, "ATC: " + atc.codigo));
                }
                items.setValue(list);
                cargando.setValue(false);
            });
        } else {
            items.setValue(new ArrayList<>());
            cargando.setValue(false);
        }
    }

    public void cargarMedicamentosFiltrados(String type, String filterId) {
        cargando.setValue(true);
        int limite = 100;
        int offset = 0;

        if (TYPE_ANATOMICA.equalsIgnoreCase(type) || TYPE_GRUPO.equalsIgnoreCase(type) || TYPE_PRINCIPIO.equalsIgnoreCase(type) || TYPE_MONOGRAFIA.equalsIgnoreCase(type)) {
            repository.filtrarPorAtc(filterId, limite, offset, list -> {
                medicamentos.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_VIA.equalsIgnoreCase(type)) {
            repository.filtrarPorVia(filterId, limite, offset, list -> {
                medicamentos.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_FORMA.equalsIgnoreCase(type)) {
            repository.filtrarPorForma(filterId, limite, offset, list -> {
                medicamentos.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_LABORATORIO.equalsIgnoreCase(type)) {
            repository.filtrarPorLaboratorio(filterId, limite, offset, list -> {
                medicamentos.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_ALFABETICO.equalsIgnoreCase(type)) {
            repository.filtrarPorLetra(filterId, limite, offset, list -> {
                medicamentos.setValue(list);
                cargando.setValue(false);
            });
        } else if (TYPE_SEGURIDAD.equalsIgnoreCase(type)) {
            repository.filtrarPorEmbarazo(filterId, limite, offset, list -> {
                medicamentos.setValue(list);
                cargando.setValue(false);
            });
        } else {
            cargando.setValue(false);
        }
    }
}
