package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "indicaciones_no_farmacologicas")
public class IndicacionNoFarmacologica {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long paciente_id;
    public String paciente_nombre;

    public String actividad_reposo;
    public String alimentacion;
    public String hidratacion;
    public String cuidados_generales;
    public String estudios_solicitados;
    public String senales_alarma;
    public String fecha_seguimiento;
    public String notas_adicionales;

    public String estado; // "Borrador" / "Emitido"
    public String fecha_creacion;
}
