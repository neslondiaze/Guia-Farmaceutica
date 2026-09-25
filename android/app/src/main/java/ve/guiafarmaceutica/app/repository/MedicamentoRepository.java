package ve.guiafarmaceutica.app.repository;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;

import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.data.Atc;
import ve.guiafarmaceutica.app.data.BanderaSeguridad;
import ve.guiafarmaceutica.app.data.ConteoGrupo;
import ve.guiafarmaceutica.app.data.FichaClinica;
import ve.guiafarmaceutica.app.data.MedicamentoDao;
import ve.guiafarmaceutica.app.data.Monografia;
import ve.guiafarmaceutica.app.data.MonografiaBusqueda;
import ve.guiafarmaceutica.app.data.MonografiaDao;
import ve.guiafarmaceutica.app.data.ReglaDosificacion;
import ve.guiafarmaceutica.app.data.ResumenForma;
import ve.guiafarmaceutica.app.data.ResumenLaboratorio;
import ve.guiafarmaceutica.app.data.ResumenLetra;
import ve.guiafarmaceutica.app.data.ResumenVia;

/**
 * Fuente única de verdad para medicamentos y clasificación (Fase 1).
 *
 * <p>Envuelve los DAOs de Room ({@link MedicamentoDao}, {@link MonografiaDao})
 * y ejecuta todo fuera del hilo principal (executor propio de un hilo; la
 * Fase 2 lo expondrá con ViewModel/LiveData).</p>
 *
 * <p>El escapado FTS replica {@code _consulta_fts} de
 * {@code crear_bd_guia.py}: tokens entrecomillados y último token como
 * prefijo con {@code *}, de modo que {@code ibu} matchea {@code Ibuprofeno}
 * y {@code solucion} matchea {@code Solución} (tokenizador
 * {@code unicode61 remove_diacritics=2}).</p>
 */
public class MedicamentoRepository {

    /** Callback genérico de resultados asíncronos. */
    public interface Resultado<T> {
        void alCompletar(List<T> datos);
    }

    private final Context contexto;
    private final Executor ejecutor = Executors.newSingleThreadExecutor();

    public MedicamentoRepository(Context contexto) {
        this.contexto = contexto.getApplicationContext();
    }

    /** Primera página del catálogo (7057 filas en total). */
    public void listar(int limite, int offset, Resultado<FichaClinica> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qListar(limite, offset);
        ejecutarLista(() -> bd().medicamentoDao().listar(consulta), callback);
    }

    /** Ficha completa por código nacional (lista de 0 o 1 elementos). */
    public void porId(String id, Resultado<FichaClinica> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qPorId(id);
        ejecutarLista(() -> {
            FichaClinica ficha = bd().medicamentoDao().porId(consulta);
            List<FichaClinica> salida = new ArrayList<>();
            if (ficha != null) {
                salida.add(ficha);
            }
            return salida;
        }, callback);
    }

    /** Búsqueda FTS5 con ranking por relevancia; degrada a LIKE sin FTS5. */
    public void buscar(String textoLibre, int limite, int offset,
                       Resultado<FichaClinica> callback) {
        String consultaFts = escaparFts(textoLibre);
        SupportSQLiteQuery consultaFtsQuery = consultaFts == null
                ? null
                : MedicamentoDao.qBuscarFts(consultaFts, limite, offset);
        SupportSQLiteQuery consultaLike = MedicamentoDao.qBuscarLike(
                patronLike(textoLibre), limite, offset);
        ejecutarLista(() -> {
            if (consultaFtsQuery == null) {
                return new ArrayList<>();
            }
            try {
                return bd().medicamentoDao().buscarFts(consultaFtsQuery);
            } catch (SQLiteException sinFts) {
                // Android 7-10: el framework no trae FTS5. Búsqueda por LIKE.
                return bd().medicamentoDao().buscarFts(consultaLike);
            }
        }, callback);
    }

    /** Genéricos homólogos: mismo ATC (principio activo), excluyendo la ficha. */
    public void homologos(String atc, String excluirId, Resultado<FichaClinica> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qHomologos(atc, excluirId);
        ejecutarLista(() -> bd().medicamentoDao().homologos(consulta), callback);
    }

    /** Raíces de la jerarquía ATC (Nivel 1). */
    public void clasesAnatomicas(Resultado<Atc> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qClasesAnatomicas();
        ejecutarLista(() -> bd().medicamentoDao().clasesAnatomicas(consulta), callback);
    }

    /** Grupos terapéuticos (Niveles 2 y 3). */
    public void gruposTerapeuticos(Resultado<Atc> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qGruposTerapeuticos();
        ejecutarLista(() -> bd().medicamentoDao().hijosAtc(consulta), callback);
    }

    /** Principios activos (Nivel 5 / Terminales ATC). */
    public void principiosActivos(Resultado<Atc> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qPrincipiosActivos();
        ejecutarLista(() -> bd().medicamentoDao().hijosAtc(consulta), callback);
    }

    /** Hijos directos de un nodo ATC. */
    public void hijosAtc(String padreCodigo, Resultado<Atc> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qHijosAtc(padreCodigo);
        ejecutarLista(() -> bd().medicamentoDao().hijosAtc(consulta), callback);
    }

    /** Filtrado por prefijo ATC. */
    public void filtrarPorAtc(String prefix, int limite, int offset, Resultado<FichaClinica> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qPorAtcPrefix(prefix, limite, offset);
        ejecutarLista(() -> bd().medicamentoDao().listar(consulta), callback);
    }

    /** Filtrado por Vía de Administración. */
    public void filtrarPorVia(String via, int limite, int offset, Resultado<FichaClinica> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qPorVia(via, limite, offset);
        ejecutarLista(() -> bd().medicamentoDao().listar(consulta), callback);
    }

    /** Filtrado por Forma Farmacéutica. */
    public void filtrarPorForma(String forma, int limite, int offset, Resultado<FichaClinica> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qPorForma(forma, limite, offset);
        ejecutarLista(() -> bd().medicamentoDao().listar(consulta), callback);
    }

    /** Filtrado por Laboratorio Fabricante. */
    public void filtrarPorLaboratorio(String lab, int limite, int offset, Resultado<FichaClinica> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qPorLaboratorio(lab, limite, offset);
        ejecutarLista(() -> bd().medicamentoDao().listar(consulta), callback);
    }

    /** Filtrado por Inicial Alfabética. */
    public void filtrarPorLetra(String letra, int limite, int offset, Resultado<FichaClinica> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qPorLetra(letra, limite, offset);
        ejecutarLista(() -> bd().medicamentoDao().listar(consulta), callback);
    }

    /** Filtrado por Clasificación de Embarazo. */
    public void filtrarPorEmbarazo(String embarazo, int limite, int offset, Resultado<FichaClinica> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qPorEmbarazo(embarazo, limite, offset);
        ejecutarLista(() -> bd().medicamentoDao().listar(consulta), callback);
    }

    /** Conteo por clase anatómica (J=1040, A=1039, …). */
    public void porClaseAnatomica(Resultado<ConteoGrupo> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qPorClaseAnatomica();
        ejecutarLista(() -> bd().medicamentoDao().porClaseAnatomica(consulta), callback);
    }

    /** Top de grupos terapéuticos (J01=790, M01=396, …). */
    public void topGruposTerapeuticos(int limite, Resultado<ConteoGrupo> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qTopGruposTerapeuticos(limite);
        ejecutarLista(() -> bd().medicamentoDao().topGruposTerapeuticos(consulta), callback);
    }

    /** Catálogo de vías de administración (48) para los filtros. */
    public void resumenVias(Resultado<ResumenVia> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qResumenVias();
        ejecutarLista(() -> bd().medicamentoDao().resumenVias(consulta), callback);
    }

    /** Catálogo de formas farmacéuticas (107) para los filtros. */
    public void resumenFormas(Resultado<ResumenForma> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qResumenFormas();
        ejecutarLista(() -> bd().medicamentoDao().resumenFormas(consulta), callback);
    }

    /** Laboratorios titulares (136) para los filtros. */
    public void resumenLaboratorios(Resultado<ResumenLaboratorio> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qResumenLaboratorios();
        ejecutarLista(() -> bd().medicamentoDao().resumenLaboratorios(consulta), callback);
    }

    /** Letras del índice alfabético (27) para los filtros. */
    public void resumenLetras(Resultado<ResumenLetra> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qResumenLetras();
        ejecutarLista(() -> bd().medicamentoDao().resumenLetras(consulta), callback);
    }

    /** Monografía completa de un principio activo (lista de 0 o 1 elementos). */
    public void monografia(String atc, Resultado<Monografia> callback) {
        SupportSQLiteQuery consulta = MonografiaDao.qPorAtc(atc);
        ejecutarLista(() -> {
            Monografia mono = bd().monografiaDao().porAtc(consulta);
            List<Monografia> salida = new ArrayList<>();
            if (mono != null) {
                salida.add(mono);
            }
            return salida;
        }, callback);
    }

    /** Búsqueda FTS5 en monografías; degrada a LIKE sin FTS5 (Android 7–10). */
    public void buscarMonografias(String textoLibre, int limite,
                                  Resultado<MonografiaBusqueda> callback) {
        String consultaFts = escaparFts(textoLibre);
        SupportSQLiteQuery consultaFtsQuery = consultaFts == null
                ? null
                : MonografiaDao.qBuscarFts(consultaFts, limite);
        SupportSQLiteQuery consultaLike = MonografiaDao.qBuscarLike(
                patronLike(textoLibre), limite);
        ejecutarLista(() -> {
            if (consultaFtsQuery == null) {
                return new ArrayList<>();
            }
            try {
                return bd().monografiaDao().buscarFts(consultaFtsQuery);
            } catch (SQLiteException sinFts) {
                return bd().monografiaDao().buscarFts(consultaLike);
            }
        }, callback);
    }

    /** Banderas de embarazo/lactancia de un medicamento (lista de 0 o 1). */
    public void banderas(String medicamentoId, Resultado<BanderaSeguridad> callback) {
        SupportSQLiteQuery consulta = MonografiaDao.qBanderas(medicamentoId);
        ejecutarLista(() -> {
            BanderaSeguridad bandera = bd().monografiaDao().banderas(consulta);
            List<BanderaSeguridad> salida = new ArrayList<>();
            if (bandera != null) {
                salida.add(bandera);
            }
            return salida;
        }, callback);
    }

    /** Regla de dosificación de un medicamento según su código ATC. */
    public void reglaDosificacion(String atcCodigo, Resultado<ReglaDosificacion> callback) {
        SupportSQLiteQuery consulta = MedicamentoDao.qReglaDosificacion(atcCodigo);
        ejecutarLista(() -> {
            ReglaDosificacion regla = bd().medicamentoDao().reglaDosificacion(consulta);
            List<ReglaDosificacion> salida = new ArrayList<>();
            if (regla != null) {
                salida.add(regla);
            }
            return salida;
        }, callback);
    }

    /**
     * Escapa texto libre a consulta FTS5 segura.
     * Replica {@code _consulta_fts}: sin tokens devuelve null (= sin resultados).
     */
    static String escaparFts(String textoLibre) {
        if (textoLibre == null) {
            return null;
        }
        String[] tokens = textoLibre.replace("\"", " ").trim().split("\\s+");
        List<String> partes = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isEmpty()) {
                partes.add("\"" + token.replace("\"", "\"\"") + "\"");
            }
        }
        if (partes.isEmpty()) {
            return null;
        }
        int ultimo = partes.size() - 1;
        partes.set(ultimo, partes.get(ultimo) + "*");
        StringBuilder consulta = new StringBuilder();
        for (int i = 0; i < partes.size(); i++) {
            if (i > 0) {
                consulta.append(' ');
            }
            consulta.append(partes.get(i));
        }
        return consulta.toString();
    }

    /** Patrón LIKE con comodines a ambos lados, % escapados del texto libre. */
    static String patronLike(String textoLibre) {
        if (textoLibre == null) {
            return "%";
        }
        String limpio = textoLibre.trim()
                .replace("%", "\\%")
                .replace("_", "\\_");
        if (limpio.isEmpty()) {
            return "%";
        }
        return "%" + limpio + "%";
    }

    /** Conexión única (la copia de la semilla ocurre aquí, si hace falta). */
    private AppDatabase bd() {
        return AppDatabase.obtener(contexto);
    }

    private <T> void ejecutarLista(Callable<List<T>> tarea,
                                   Resultado<T> callback) {
        ejecutor.execute(() -> {
            List<T> datos;
            try {
                datos = tarea.call();
            } catch (Exception e) {
                datos = new ArrayList<>();
            }
            List<T> entrega = datos;
            new Handler(Looper.getMainLooper())
                    .post(() -> callback.alCompletar(entrega));
        });
    }
}
