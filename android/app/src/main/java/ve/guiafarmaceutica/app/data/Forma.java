package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/** Dimensión de formas farmacéuticas (107 valores en la BD semilla). */
@Entity(tableName = "formas",
        indices = {@androidx.room.Index(value = "nombre", unique = true)})
public class Forma {

    @PrimaryKey
    @NonNull
    public int id;

    public String nombre;
}
