package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Nodo de la clasificación ATC jerárquica (2033 códigos en la BD semilla).
 *
 * <p>Niveles por longitud del código: 1 (letra, 14 clases anatómicas),
 * 3 (grupo terapéutico), 4 (subgrupo), 5 (subgrupo químico) y 7
 * (principio activo, 1270 terminales). Ejemplo:
 * {@code M → M01 → M01A → M01AE → M01AE01} (Ibuprofeno).</p>
 */
@Entity(tableName = "atc",
        foreignKeys = @ForeignKey(entity = Atc.class,
                parentColumns = "codigo",
                childColumns = "padre"),
        indices = @Index("padre"))
public class Atc {

    @PrimaryKey
    @NonNull
    public String codigo;

    public String descripcion;
    public int nivel;
    public String padre;
}
