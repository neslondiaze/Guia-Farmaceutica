package ve.guiafarmaceutica.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ImagenComplementariaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(ImagenComplementaria imagen);

    @Query("SELECT * FROM imagenes_complementarias WHERE paciente_id = :pacienteId ORDER BY id DESC")
    List<ImagenComplementaria> listarPorPaciente(long pacienteId);

    @Query("DELETE FROM imagenes_complementarias WHERE id = :id")
    void eliminar(long id);
}
