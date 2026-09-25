package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/** Dimensión de vías de administración (48 valores en la BD semilla). */
@Entity(tableName = "vias",
        indices = {@androidx.room.Index(value = "nombre", unique = true)})
public class Via {

    @PrimaryKey
    @NonNull
    public int id;

    public String nombre;
}
