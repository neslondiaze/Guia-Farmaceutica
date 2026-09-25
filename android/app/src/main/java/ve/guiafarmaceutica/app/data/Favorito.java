package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Favoritos del usuario (tabla propia de la app, NO forma parte de la BD
 * semilla, que se trata como de solo lectura).
 *
 * <p>Permite guardar medicamentos con foto opcional (Glide la carga desde
 * {@code foto_path}, en almacenamiento interno) y una nota libre.</p>
 */
@Entity(tableName = "favoritos")
public class Favorito {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String medicamento_id;
    public String foto_path;
    public String nota;
    public long creado_en;

    public Favorito(String medicamento_id, String foto_path, String nota, long creado_en) {
        this.medicamento_id = medicamento_id;
        this.foto_path = foto_path;
        this.nota = nota;
        this.creado_en = creado_en;
    }
}
