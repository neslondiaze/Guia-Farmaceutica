package ve.guiafarmaceutica.app.data;

/** Modelo genérico para elementos de categorías y grupos de filtro. */
public class CategoryItem {
    public String id;
    public String titulo;
    public String subtitulo;
    public String extra;

    public CategoryItem(String id, String titulo, String subtitulo) {
        this.id = id;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
    }

    public CategoryItem(String id, String titulo, String subtitulo, String extra) {
        this.id = id;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.extra = extra;
    }
}
