package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "impresiones_diagnosticas")
public class ImpresionDiagnostica {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long paciente_id;
    public String paciente_nombre;
    public String paciente_cedula;
    public String paciente_edad;
    public String paciente_peso;
    public String diagnostico;
    public String fecha_vencimiento;
    public int renovaciones;
    public String estado; // "Borrador" / "Emitido"
    public String observaciones;
    public String fecha_creacion;
}
