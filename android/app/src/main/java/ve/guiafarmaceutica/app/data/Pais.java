package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/** Dimensión de países (1 fila hoy: Venezuela). */
@Entity(tableName = "paises",
        indices = {@androidx.room.Index(value = "nombre", unique = true)})
public class Pais {

    @PrimaryKey
    @NonNull
    public int id;

    public String nombre;
}
