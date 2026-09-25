package ve.guiafarmaceutica.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ImpresionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertarImpresion(ImpresionDiagnostica impresion);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarDetalles(List<ImpresionDetalle> detalles);

    @Query("SELECT * FROM impresiones_diagnosticas WHERE id = :id LIMIT 1")
    ImpresionDiagnostica obtenerImpresion(long id);

    @Query("SELECT * FROM impresion_detalles WHERE impresion_id = :impresionId")
    List<ImpresionDetalle> obtenerDetalles(long impresionId);

    @Query("SELECT * FROM impresiones_diagnosticas ORDER BY id DESC")
    List<ImpresionDiagnostica> listarImpresiones();

    @Query("DELETE FROM impresiones_diagnosticas WHERE id = :id")
    void eliminarImpresion(long id);

    @Query("DELETE FROM impresion_detalles WHERE impresion_id = :impresionId")
    void eliminarDetalles(long impresionId);
}
