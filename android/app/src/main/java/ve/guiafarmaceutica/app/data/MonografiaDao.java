package ve.guiafarmaceutica.app.data;

import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import java.util.List;

/**
 * Contrato de acceso a monografías clínicas (1270, una por principio activo)
 * y a sus banderas de seguridad. Mismo patrón {@link RawQuery @RawQuery} que
 * {@link MedicamentoDao}: la semilla es legada y no se registra en Room.
 *
 * <p>El FTS de monografías ({@code monografias_fts}) se consulta con
 * {@code MATCH} + {@code snippet()} sobre la columna 2 (extracto).</p>
 */
@Dao
public interface MonografiaDao {

    /** Monografía completa de un principio activo (código ATC terminal). */
    String SQL_POR_ATC = "SELECT * FROM monografias WHERE atc_codigo = ? LIMIT 1";

    /** Búsqueda FTS5 dentro del texto clínico, con extracto resaltado. */
    String SQL_BUSCAR_FTS = "SELECT m.atc_codigo AS atc, m.titulo AS titulo, "
            + "snippet(monografias_fts, 2, '[', ']', '…', 12) AS extracto "
            + "FROM monografias_fts "
            + "JOIN monografias m ON m.atc_codigo = monografias_fts.atc_codigo "
            + "WHERE monografias_fts MATCH ? ORDER BY rank LIMIT ?";

    /**
     * Alternativa LIKE para Android 7–10 (sin FTS5 en el framework): busca
     * en título e indicaciones, con extracto del inicio de las indicaciones.
     */
    String SQL_BUSCAR_LIKE = "SELECT atc_codigo AS atc, titulo AS titulo, "
            + "substr(indicaciones_terapeuticas,1,200) AS extracto "
            + "FROM monografias "
            + "WHERE titulo LIKE ? ESCAPE '\\' "
            + "OR indicaciones_terapeuticas LIKE ? ESCAPE '\\' "
            + "ORDER BY titulo LIMIT ?";

    /** Banderas de seguridad de un medicamento concreto (embarazo/lactancia). */
    String SQL_BANDERAS = "SELECT embarazo, lactancia FROM medicamento_clinico "
            + "WHERE medicamento_id = ? LIMIT 1";

    @RawQuery
    Monografia porAtc(SupportSQLiteQuery consulta);

    @RawQuery
    List<MonografiaBusqueda> buscarFts(SupportSQLiteQuery consulta);

    @RawQuery
    BanderaSeguridad banderas(SupportSQLiteQuery consulta);

    static SupportSQLiteQuery qPorAtc(String atc) {
        return new SimpleSQLiteQuery(SQL_POR_ATC, new Object[]{atc});
    }

    /** Consulta FTS ya escapada (ver {@code MedicamentoRepository.escaparFts}). */
    static SupportSQLiteQuery qBuscarFts(String consultaFts, int limite) {
        return new SimpleSQLiteQuery(SQL_BUSCAR_FTS, new Object[]{consultaFts, limite});
    }

    /** Búsqueda LIKE de respaldo para dispositivos sin FTS5 (Android 7–10). */
    static SupportSQLiteQuery qBuscarLike(String patron, int limite) {
        return new SimpleSQLiteQuery(SQL_BUSCAR_LIKE,
                new Object[]{patron, patron, limite});
    }

    static SupportSQLiteQuery qBanderas(String medicamentoId) {
        return new SimpleSQLiteQuery(SQL_BANDERAS, new Object[]{medicamentoId});
    }
}
