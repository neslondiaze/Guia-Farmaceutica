package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Enlace medicamento → clasificación clínica (7057 filas, cobertura 100 %).
 *
 * <p>Cada fila apunta al ATC terminal, la vía y la forma extraídos de la ficha
 * de referencia externa, más las banderas de embarazo/lactancia. {@code leido_en}
 * es la marca de progreso del enriquecimiento reanudable; {@code error} queda
 * no nulo solo si la ficha no pudo descargarse.</p>
 */
@Entity(tableName = "medicamento_clinico",
        foreignKeys = {
                @ForeignKey(entity = Medicamento.class,
                        parentColumns = "id",
                        childColumns = "medicamento_id"),
                @ForeignKey(entity = Atc.class,
                        parentColumns = "codigo",
                        childColumns = "atc_codigo"),
                @ForeignKey(entity = Via.class,
                        parentColumns = "id",
                        childColumns = "via_id"),
                @ForeignKey(entity = Forma.class,
                        parentColumns = "id",
                        childColumns = "forma_id")
        },
        indices = {
                @Index("atc_codigo"),
                @Index("via_id"),
                @Index("forma_id")
        })
public class MedicamentoClinico {

    @PrimaryKey
    @NonNull
    public String medicamento_id;

    public String atc_codigo;
    public Integer via_id;
    public Integer forma_id;
    public String embarazo;
    public String lactancia;
    public String url_monografia;
    public String leido_en;
    public String error;
}
