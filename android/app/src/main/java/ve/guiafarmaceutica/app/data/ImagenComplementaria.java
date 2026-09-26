package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "imagenes_complementarias")
public class ImagenComplementaria {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long paciente_id;
    public String ruta_imagen;
    public String descripcion_datos;
    public String fecha_registro;
}
