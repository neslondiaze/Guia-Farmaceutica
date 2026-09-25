package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Monografía del principio activo (1270 en la BD semilla, 1 por código ATC
 * terminal). Reúne las 15 secciones clínicas: mecanismo de acción,
 * indicaciones terapéuticas (+ posología), posología, modo de administración,
 * contraindicaciones, advertencias y precauciones, insuficiencia hepática y
 * renal, interacciones, embarazo, lactancia, efectos sobre la conducción,
 * reacciones adversas y sobredosificación.
 */
@Entity(tableName = "monografias",
        foreignKeys = @ForeignKey(entity = Atc.class,
                parentColumns = "codigo",
                childColumns = "atc_codigo"),
        indices = @Index("titulo"))
public class Monografia {

    @PrimaryKey
    @NonNull
    public String atc_codigo;

    public String titulo;
    public String url_monografia;
    public String embarazo;
    public String lactancia;
    public String notas;
    public String mecanismo_accion;
    public String indicaciones_terapeuticas;
    public String indicaciones_posologia;
    public String posologia;
    public String modo_administracion;
    public String contraindicaciones;
    public String advertencias_precauciones;
    public String insuficiencia_hepatica;
    public String insuficiencia_renal;
    public String interacciones;
    public String embarazo_texto;
    public String lactancia_texto;
    public String conduccion;
    public String reacciones_adversas;
    public String sobredosificacion;
    public String actualizado_en;
}
