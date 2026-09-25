package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pacientes")
public class Paciente {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String nombre_completo;
    public String identificacion; // ej: DEMO-10482
    public String fecha_nacimiento;
    public String edad_calculada;
    public String sexo;

    public String telefono;
    public String correo;
    public String direccion;
    public String contacto_emergencia;

    public String alergias;
    public String condiciones_relevantes;
    public String grupo_sanguineo;
    public String peso;
    public String altura;
    public String notas_clinicas;
}
