package ve.guiafarmaceutica.app.data;

import java.util.List;

/**
 * Fila plana de la vista {@code v_clinico}: medicamento + laboratorio + país +
 * ATC + vía + forma + banderas, en una sola consulta.
 *
 * <p>Es el DTO principal de listados y ficha (Fases 2–3). Se mapea desde el
 * cursor/SQLite, no es una {@code @Entity}: las vistas de la BD semilla se
 * leen, nunca se escriben.</p>
 */
public class FichaClinica {

    public String id;
    public String nombre;
    public String laboratorio;
    public String pais;
    public String atc_codigo;
    public String atc_descripcion;
    public Integer atc_nivel;
    public String via;
    public String forma;
    public String embarazo;
    public String lactancia;
    public int tiene_monografia;

    /** Nombres de columna en el mismo orden, para {@code Cursor} manual si hiciera falta. */
    public static final List<String> COLUMNAS = java.util.Arrays.asList(
            "id", "nombre", "laboratorio", "pais", "atc_codigo", "atc_descripcion",
            "atc_nivel", "via", "forma", "embarazo", "lactancia", "tiene_monografia");
}
