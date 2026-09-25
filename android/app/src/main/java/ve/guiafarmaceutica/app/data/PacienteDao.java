package ve.guiafarmaceutica.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PacienteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(Paciente paciente);

    @Update
    void actualizar(Paciente paciente);

    @Query("DELETE FROM pacientes WHERE id = :id")
    void eliminar(long id);

    @Query("SELECT * FROM pacientes WHERE id = :id LIMIT 1")
    Paciente obtenerPorId(long id);

    @Query("SELECT * FROM pacientes WHERE identificacion = :identificacion LIMIT 1")
    Paciente obtenerPorIdentificacion(String identificacion);

    @Query("SELECT * FROM pacientes ORDER BY id DESC")
    List<Paciente> listarPacientes();

    @Query("SELECT * FROM pacientes WHERE nombre_completo LIKE '%' || :query || '%' OR identificacion LIKE '%' || :query || '%'")
    List<Paciente> buscarPacientes(String query);
}
