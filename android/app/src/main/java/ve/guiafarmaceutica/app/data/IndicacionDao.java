package ve.guiafarmaceutica.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface IndicacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(IndicacionNoFarmacologica indicacion);

    @Update
    void actualizar(IndicacionNoFarmacologica indicacion);

    @Query("DELETE FROM indicaciones_no_farmacologicas WHERE id = :id")
    void eliminar(long id);

    @Query("SELECT * FROM indicaciones_no_farmacologicas WHERE id = :id LIMIT 1")
    IndicacionNoFarmacologica obtenerPorId(long id);

    @Query("SELECT * FROM indicaciones_no_farmacologicas ORDER BY id DESC")
    List<IndicacionNoFarmacologica> listarIndicaciones();

    @Query("SELECT * FROM indicaciones_no_farmacologicas WHERE paciente_id = :pacienteId ORDER BY id DESC")
    List<IndicacionNoFarmacologica> listarPorPaciente(long pacienteId);

    @Query("SELECT * FROM indicaciones_no_farmacologicas WHERE estado = :estado ORDER BY id DESC")
    List<IndicacionNoFarmacologica> filtrarIndicaciones(String estado);
}
