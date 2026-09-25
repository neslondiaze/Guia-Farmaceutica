package ve.guiafarmaceutica.app.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

/**
 * Favoritos del usuario (tabla propia, creada por Room en la BD de la app).
 */
@Dao
public interface FavoritoDao {

    @Insert
    long insertar(Favorito favorito);

    @Delete
    void eliminar(Favorito favorito);

    @Query("SELECT * FROM favoritos ORDER BY creado_en DESC")
    List<Favorito> todos();

    @Query("SELECT * FROM favoritos WHERE medicamento_id = :medicamentoId LIMIT 1")
    Favorito porMedicamento(String medicamentoId);

    @Query("SELECT COUNT(*) FROM favoritos")
    int contar();
}
