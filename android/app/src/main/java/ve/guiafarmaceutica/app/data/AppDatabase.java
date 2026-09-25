package ve.guiafarmaceutica.app.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Base de datos de la app: abre la BD semilla copiada desde {@code assets/}.
 *
 * <p><b>Estrategia de siembra (Fase 1):</b> en el primer arranque se copia
 * {@code assets/databases/medicamentos_guia_venezuela.db} (~24 MB) al
 * almacenamiento interno. Room solo registra {@code favoritos} (tabla propia
 * que crea en {@code onCreate}); las 10 tablas + 2 FTS5 + 7 vistas heredadas
 * del scraper se leen con {@code @RawQuery} vía {@link MedicamentoDao} y
 * {@link MonografiaDao}, sin registrarlas ni validarlas.</p>
 *
 * <p><b>FTS5:</b> la semilla trae {@code medicamentos_fts} y
 * {@code monografias_fts}. El SQLite del framework soporta FTS5 desde
 * Android 11; en 7.0–10 el Repository degrada a búsquedas LIKE, que
 * contra 7057 filas son instantáneas.</p>
 */
@Database(
        entities = {Favorito.class, ImpresionDiagnostica.class, ImpresionDetalle.class, Paciente.class, IndicacionNoFarmacologica.class},
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    /** Nombre del fichero de la semilla, tal cual va en {@code assets/databases/}. */
    public static final String NOMBRE_BD_SEMILLA = "medicamentos_guia_venezuela.db";

    /** Nombre de la BD abierta por la app (copia de trabajo en files internos). */
    static final String NOMBRE_BD_APP = "guia_farmaceutica.db";

    /** Conteo esperado tras la copia (protege contra copias truncadas). */
    static final int MEDICAMENTOS_ESPERADOS = 7057;

    private static volatile AppDatabase instancia;

    public abstract FavoritoDao favoritoDao();

    public abstract MedicamentoDao medicamentoDao();

    public abstract MonografiaDao monografiaDao();

    public abstract ImpresionDao impresionDao();

    public abstract PacienteDao pacienteDao();

    public abstract IndicacionDao indicacionDao();

    /**
     * Devuelve la instancia única, copiando antes la semilla si hace falta.
     * Debe llamarse fuera del hilo principal en el primer arranque.
     */
    public static AppDatabase obtener(Context contexto) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    asegurarSemilla(contexto.getApplicationContext());
                    instancia = Room.databaseBuilder(
                                    contexto.getApplicationContext(),
                                    AppDatabase.class,
                                    NOMBRE_BD_APP)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instancia;
    }

    /**
     * Copia la semilla desde {@code assets/} si no existe o está corrupta.
     * Idempotente y segura de llamar en cada arranque.
     */
    static void asegurarSemilla(Context contexto) {
        File destino = contexto.getDatabasePath(NOMBRE_BD_APP);
        if (destino.exists() && semillaValida(contexto, destino)) {
            return;
        }
        File padre = destino.getParentFile();
        if (padre != null && !padre.exists()) {
            //noinspection ResultOfMethodCallIgnored
            padre.mkdirs();
        }
        try (InputStream entrada = contexto.getAssets().open("databases/" + NOMBRE_BD_SEMILLA);
             OutputStream salida = new FileOutputStream(destino, false)) {
            byte[] buffer = new byte[8192];
            int leidos;
            while ((leidos = entrada.read(buffer)) != -1) {
                salida.write(buffer, 0, leidos);
            }
            salida.flush();
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            destino.delete();
            throw new IllegalStateException("No se pudo copiar la BD semilla", e);
        }
        if (!semillaValida(contexto, destino)) {
            //noinspection ResultOfMethodCallIgnored
            destino.delete();
            throw new IllegalStateException("La BD semilla copiada está corrupta");
        }
    }

    /**
     * La copia es válida si abre (SQLite del framework) y trae los 7057
     * medicamentos esperados.
     */
    static boolean semillaValida(Context contexto, File destino) {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(
                    destino.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY);
            Cursor cursor =
                    db.rawQuery("SELECT COUNT(*) FROM medicamentos", null);
            try {
                return cursor.moveToFirst()
                        && cursor.getInt(0) == MEDICAMENTOS_ESPERADOS;
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
}
