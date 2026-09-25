package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "impresion_detalles")
public class ImpresionDetalle {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long impresion_id;
    public String medicamento_id;
    public String medicamento_nombre;
    public String presentacion;
    public String concentracion;
    public String dosificacion;
    public String via_administracion;
    public String frecuencia_instrucciones;
    public String duracion_dias;
    public String cantidad_despachar;
    public String instrucciones_paciente;
}
