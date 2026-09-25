package ve.guiafarmaceutica.app.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Espejo exacto de la tabla {@code medicamentos} de la BD semilla.
 *
 * <p>Generada contra
 * {@code medicamentos_guia_venezuela.db} (7057 filas, id TEXT = código
 * nacional de 8 dígitos). Los índices se declaran para documentar el esquema;
 * Room no los recrea sobre una BD preempaquetada abierta en solo lectura.</p>
 */
@Entity(tableName = "medicamentos",
        foreignKeys = {
                @ForeignKey(entity = Laboratorio.class,
                        parentColumns = "id",
                        childColumns = "laboratorio_id"),
                @ForeignKey(entity = Pais.class,
                        parentColumns = "id",
                        childColumns = "pais_id")
        },
        indices = {
                @Index("nombre"),
                @Index("slug"),
                @Index("letra"),
                @Index("prefijo"),
                @Index("laboratorio_id")
        })
public class Medicamento {

    @PrimaryKey
    @NonNull
    public String id;

    public String nombre;
    public String slug;
    public Integer laboratorio_id;
    public Integer pais_id;
    public String letra;
    public String prefijo;
    public String url_ficha;
    public String url_equivalencias;
    public String url_fuente;
}
