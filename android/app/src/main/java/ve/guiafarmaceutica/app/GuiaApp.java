package ve.guiafarmaceutica.app;

import android.app.Application;

/**
 * Application de Guia Farmaceutica (Fase 0-1).
 *
 * <p>Punto de entrada para inicialización global. La copia de la BD semilla
 * ({@code SeedManager}) se ejecuta de forma perezosa en los repositorios,
 * no aquí, para no bloquear el arranque.</p>
 */
public class GuiaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
