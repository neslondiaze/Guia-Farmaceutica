package ve.guiafarmaceutica.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reglas_dosificacion")
public class ReglaDosificacion {

    @PrimaryKey
    @NonNull
    public String atc_codigo = "";

    public String titulo;
    public Double dosis_recomendada_mg_kg;
    public Double dosis_max_toma_mg;
    public Double dosis_max_dia_mg;
    public Double dosis_max_peso_mg_kg_dia;
    public Integer intervalo_min_horas;
    public Double concentracion_mg;
    public Double volumen_ml;
    public String unidad;
}
