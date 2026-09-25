package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/** Dimensión de laboratorios titulares (136 en el dataset actual). */
@Entity(tableName = "laboratorios",
        indices = {@androidx.room.Index(value = "nombre", unique = true)})
public class Laboratorio {

    @PrimaryKey
    @NonNull
    public int id;

    public String nombre;
}
