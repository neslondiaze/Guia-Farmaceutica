package ve.guiafarmaceutica.app.data;

import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import java.util.List;

/**
 * Contrato de acceso a medicamentos, catálogo y clasificación (Fase 1).
 *
 * <p><b>Patrón elegido:</b> la BD semilla es <em>legada</em> (10 tablas +
 * 2 FTS5 + 7 vistas generadas por {@code crear_bd_guia.py} /
 * {@code enriquecer_clinico.py}). Room no la registra como esquema propio
 * (ver {@link AppDatabase}), así que toda consulta contra tablas heredadas va
 * con {@link RawQuery @RawQuery}: Room ejecuta el SQL tal cual y mapea el
 * cursor a los POJOs por nombre de columna, sin validar el esquema en
 * compilación.</p>
 *
 * <p>Los builders estáticos {@code q…()} construyen la
 * {@link SimpleSQLiteQuery} con parámetros posicionales seguros (nunca se
 * concatena texto del usuario). El SQL es idéntico al validado contra la BD
 * real en {@code clasificacion_medicamentos.md}.</p>
 */
@Dao
public interface MedicamentoDao {

    /** Vista plana completa, ordenada por nombre (base de listados). */
    String SQL_LISTAR = "SELECT * FROM v_clinico ORDER BY nombre LIMIT ? OFFSET ?";

    /** Ficha de un medicamento por su código nacional (id de 8 dígitos). */
    String SQL_POR_ID = "SELECT * FROM v_clinico WHERE id = ? LIMIT 1";

    /** Búsqueda FTS5 en nombre+laboratorio con ranking por relevancia. */
    String SQL_BUSCAR_FTS = "SELECT v.* FROM medicamentos_fts "
            + "JOIN v_clinico AS v ON v.id = medicamentos_fts.id "
            + "WHERE medicamentos_fts MATCH ? ORDER BY rank LIMIT ? OFFSET ?";

    /**
     * Alternativa LIKE para Android 7–10 (sin FTS5 en el framework):
     * prefijo insensible a mayúsculas contra nombre y laboratorio.
     */
    String SQL_BUSCAR_LIKE = "SELECT * FROM v_clinico "
            + "WHERE nombre LIKE ? ESCAPE '\\' OR laboratorio LIKE ? ESCAPE '\\' "
            + "ORDER BY nombre LIMIT ? OFFSET ?";

    /** Genéricos homólogos: mismo principio activo (ATC), otros laboratorios. */
    String SQL_HOMOLOGOS = "SELECT * FROM v_clinico WHERE atc_codigo = ? AND id != ? "
            + "ORDER BY laboratorio, nombre";

    /** Navegación ATC: hijos directos de un nodo (padre → códigos). */
    String SQL_HIJOS_ATC = "SELECT * FROM atc WHERE padre = ? ORDER BY codigo";

    /** Raíces del árbol ATC: las 14 clases anatómicas (nivel 1). */
    String SQL_CLASES_ANATOMICAS = "SELECT * FROM atc WHERE nivel = 1 ORDER BY codigo";

    /** Grupos terapéuticos (niveles 2 y 3). */
    String SQL_GRUPOS_TERAPEUTICOS = "SELECT * FROM atc WHERE nivel IN (2, 3) ORDER BY codigo";

    /** Principios activos (nivel 5 / terminales ATC). */
    String SQL_PRINCIPIOS_ACTIVOS = "SELECT * FROM atc WHERE nivel = 5 ORDER BY descripcion";

    /** Conteo de medicamentos por clase anatómica (J=1040, A=1039, …). */
    String SQL_POR_CLASE = "SELECT substr(c.atc_codigo,1,1) AS grupo, COUNT(*) AS meds "
            + "FROM medicamento_clinico c GROUP BY 1 ORDER BY meds DESC";

    /** Top grupos terapéuticos (nivel 2, ej. J01=790, M01=396). */
    String SQL_TOP_GRUPOS = "SELECT substr(c.atc_codigo,1,3) AS grupo, COUNT(*) AS meds "
            + "FROM medicamento_clinico c GROUP BY 1 ORDER BY meds DESC LIMIT ?";

    /** Consultas de filtrado específico de medicamentos. */
    String SQL_POR_ATC_PREFIX = "SELECT * FROM v_clinico WHERE atc_codigo LIKE ? ORDER BY nombre LIMIT ? OFFSET ?";
    String SQL_POR_VIA = "SELECT * FROM v_clinico WHERE via LIKE ? ORDER BY nombre LIMIT ? OFFSET ?";
    String SQL_POR_FORMA = "SELECT * FROM v_clinico WHERE forma LIKE ? ORDER BY nombre LIMIT ? OFFSET ?";
    String SQL_POR_LABORATORIO = "SELECT * FROM v_clinico WHERE laboratorio LIKE ? ORDER BY nombre LIMIT ? OFFSET ?";
    String SQL_POR_LETRA = "SELECT * FROM v_clinico WHERE nombre LIKE ? ORDER BY nombre LIMIT ? OFFSET ?";
    String SQL_POR_EMBARAZO = "SELECT * FROM v_clinico WHERE embarazo LIKE ? ORDER BY nombre LIMIT ? OFFSET ?";

    /** Catálogo de vías (48) y formas (107) para los filtros. */
    String SQL_RESUMEN_VIAS = "SELECT * FROM v_resumen_vias";
    String SQL_RESUMEN_FORMAS = "SELECT * FROM v_resumen_formas";
    String SQL_RESUMEN_LABORATORIOS = "SELECT * FROM v_resumen_laboratorios";
    String SQL_RESUMEN_LETRAS = "SELECT * FROM v_resumen_letras";

    String SQL_REGLA_DOSIFICACION = "SELECT * FROM reglas_dosificacion "
            + "WHERE atc_codigo = ? OR atc_codigo = substr(?, 1, 4) OR atc_codigo = substr(?, 1, 3) "
            + "ORDER BY length(atc_codigo) DESC LIMIT 1";

    /* ---------------------- Ejecución (@RawQuery) ---------------------- */

    @RawQuery
    List<FichaClinica> listar(SupportSQLiteQuery consulta);

    @RawQuery
    FichaClinica porId(SupportSQLiteQuery consulta);

    @RawQuery
    ReglaDosificacion reglaDosificacion(SupportSQLiteQuery consulta);

    @RawQuery
    List<FichaClinica> buscarFts(SupportSQLiteQuery consulta);

    @RawQuery
    List<FichaClinica> homologos(SupportSQLiteQuery consulta);

    @RawQuery
    List<Atc> hijosAtc(SupportSQLiteQuery consulta);

    @RawQuery
    List<Atc> clasesAnatomicas(SupportSQLiteQuery consulta);

    @RawQuery
    List<ConteoGrupo> porClaseAnatomica(SupportSQLiteQuery consulta);

    @RawQuery
    List<ConteoGrupo> topGruposTerapeuticos(SupportSQLiteQuery consulta);

    @RawQuery
    List<ResumenVia> resumenVias(SupportSQLiteQuery consulta);

    @RawQuery
    List<ResumenForma> resumenFormas(SupportSQLiteQuery consulta);

    @RawQuery
    List<ResumenLaboratorio> resumenLaboratorios(SupportSQLiteQuery consulta);

    @RawQuery
    List<ResumenLetra> resumenLetras(SupportSQLiteQuery consulta);

    /* --------------------- Builders (SimpleSQLiteQuery) --------------------- */

    static SupportSQLiteQuery qListar(int limite, int offset) {
        return new SimpleSQLiteQuery(SQL_LISTAR, new Object[]{limite, offset});
    }

    static SupportSQLiteQuery qPorId(String id) {
        return new SimpleSQLiteQuery(SQL_POR_ID, new Object[]{id});
    }

    /** Consulta FTS ya escapada (ver {@code MedicamentoRepository.escaparFts}). */
    static SupportSQLiteQuery qBuscarFts(String consultaFts, int limite, int offset) {
        return new SimpleSQLiteQuery(SQL_BUSCAR_FTS,
                new Object[]{consultaFts, limite, offset});
    }

    /** Búsqueda LIKE de respaldo para dispositivos sin FTS5 (Android 7–10). */
    static SupportSQLiteQuery qBuscarLike(String patron, int limite, int offset) {
        return new SimpleSQLiteQuery(SQL_BUSCAR_LIKE,
                new Object[]{patron, patron, limite, offset});
    }

    static SupportSQLiteQuery qHomologos(String atc, String excluirId) {
        return new SimpleSQLiteQuery(SQL_HOMOLOGOS, new Object[]{atc, excluirId});
    }

    static SupportSQLiteQuery qHijosAtc(String padreCodigo) {
        return new SimpleSQLiteQuery(SQL_HIJOS_ATC, new Object[]{padreCodigo});
    }

    static SupportSQLiteQuery qClasesAnatomicas() {
        return new SimpleSQLiteQuery(SQL_CLASES_ANATOMICAS);
    }

    static SupportSQLiteQuery qGruposTerapeuticos() {
        return new SimpleSQLiteQuery(SQL_GRUPOS_TERAPEUTICOS);
    }

    static SupportSQLiteQuery qPrincipiosActivos() {
        return new SimpleSQLiteQuery(SQL_PRINCIPIOS_ACTIVOS);
    }

    static SupportSQLiteQuery qPorClaseAnatomica() {
        return new SimpleSQLiteQuery(SQL_POR_CLASE);
    }

    static SupportSQLiteQuery qTopGruposTerapeuticos(int limite) {
        return new SimpleSQLiteQuery(SQL_TOP_GRUPOS, new Object[]{limite});
    }

    static SupportSQLiteQuery qPorAtcPrefix(String prefix, int limite, int offset) {
        return new SimpleSQLiteQuery(SQL_POR_ATC_PREFIX, new Object[]{prefix + "%", limite, offset});
    }

    static SupportSQLiteQuery qPorVia(String via, int limite, int offset) {
        return new SimpleSQLiteQuery(SQL_POR_VIA, new Object[]{via, limite, offset});
    }

    static SupportSQLiteQuery qPorForma(String forma, int limite, int offset) {
        return new SimpleSQLiteQuery(SQL_POR_FORMA, new Object[]{forma, limite, offset});
    }

    static SupportSQLiteQuery qPorLaboratorio(String lab, int limite, int offset) {
        return new SimpleSQLiteQuery(SQL_POR_LABORATORIO, new Object[]{lab, limite, offset});
    }

    static SupportSQLiteQuery qPorLetra(String letra, int limite, int offset) {
        return new SimpleSQLiteQuery(SQL_POR_LETRA, new Object[]{letra + "%", limite, offset});
    }

    static SupportSQLiteQuery qPorEmbarazo(String embarazo, int limite, int offset) {
        return new SimpleSQLiteQuery(SQL_POR_EMBARAZO, new Object[]{embarazo, limite, offset});
    }

    static SupportSQLiteQuery qReglaDosificacion(String atc) {
        return new SimpleSQLiteQuery(SQL_REGLA_DOSIFICACION, new Object[]{atc, atc, atc});
    }

    static SupportSQLiteQuery qResumenVias() {
        return new SimpleSQLiteQuery(SQL_RESUMEN_VIAS);
    }

    static SupportSQLiteQuery qResumenFormas() {
        return new SimpleSQLiteQuery(SQL_RESUMEN_FORMAS);
    }

    static SupportSQLiteQuery qResumenLaboratorios() {
        return new SimpleSQLiteQuery(SQL_RESUMEN_LABORATORIOS);
    }

    static SupportSQLiteQuery qResumenLetras() {
        return new SimpleSQLiteQuery(SQL_RESUMEN_LETRAS);
    }
}
