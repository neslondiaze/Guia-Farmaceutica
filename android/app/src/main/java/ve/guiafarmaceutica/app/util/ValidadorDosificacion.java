package ve.guiafarmaceutica.app.util;

import java.util.Locale;
import ve.guiafarmaceutica.app.data.ReglaDosificacion;
import ve.guiafarmaceutica.app.data.ResultadoCalculo;

public class ValidadorDosificacion {

    /**
     * Conversión de unidades: Libras a Kilogramos (1 lb = 0.453592 kg)
     */
    public static double librasAKilogramos(double libras) {
        return libras * 0.45359237;
    }

    /**
     * Conversión de unidades: Kilogramos a Libras
     */
    public static double kilogramosALibras(double kg) {
        return kg / 0.45359237;
    }

    /**
     * Conversión de ml a gotas (1 ml = 20 macrogotas)
     */
    public static double mlAGotas(double ml) {
        return ml * 20.0;
    }

    /**
     * Conversión de ml a microgotas (1 ml = 60 microgotas)
     */
    public static double mlAMicrogotas(double ml) {
        return ml * 60.0;
    }

    /**
     * Calcula la dosis pediátrica por peso corporal y regla de tres.
     */
    public static ResultadoCalculo calcularPorPeso(double pesoKg, double dosisMgKg, double concMg, double volMl, ReglaDosificacion regla) {
        if (pesoKg <= 0 || dosisMgKg <= 0) {
            return new ResultadoCalculo(0, 0, ResultadoCalculo.EstadoSemaforo.NARANJA, "Ingrese peso y dosis válidos.");
        }

        double concentracion = concMg > 0 ? concMg : 250.0;
        double volumenPres = volMl > 0 ? volMl : 5.0;

        ResultadoCalculo.EstadoSemaforo estado = ResultadoCalculo.EstadoSemaforo.VERDE;
        String mensaje = "Dosis calculada dentro del rango seguro.";

        double maxToma = (regla != null && regla.dosis_max_toma_mg != null && regla.dosis_max_toma_mg > 0) ? regla.dosis_max_toma_mg : 1000.0;
        double dosisMg;

        if (pesoKg >= 50.0) {
            // Si el usuario ingresó la dosis total en mg directamente (ej: 500 o 650 mg)
            if (dosisMgKg >= 50.0) {
                dosisMg = Math.min(dosisMgKg, maxToma);
            } else {
                dosisMg = Math.min(pesoKg * dosisMgKg, maxToma);
            }
            estado = ResultadoCalculo.EstadoSemaforo.VERDE;
            mensaje = String.format(Locale.US, "🟢 Dosis de Adulto/Adolescente (≥50 kg): %.1f mg / toma dentro del rango seguro estándar (500-650 mg c/4-6h, Máx. 4000 mg/día).", dosisMg);
        } else {
            dosisMg = pesoKg * dosisMgKg;
            if (dosisMg > maxToma) {
                estado = ResultadoCalculo.EstadoSemaforo.ROJO;
                mensaje = String.format(Locale.US, "¡ALERTA DE SEGURIDAD! La dosis de %.1f mg supera la Dosis Máxima recomendada (%.1f mg).", dosisMg, maxToma);
            } else if (dosisMg >= maxToma * 0.9) {
                estado = ResultadoCalculo.EstadoSemaforo.NARANJA;
                mensaje = String.format(Locale.US, "Atención: La dosis calculada (%.1f mg) está cercana al límite máximo seguro de %.1f mg.", dosisMg, maxToma);
            }
        }

        // Regla de tres: Volumen = (Dosis requerida * Volumen presentación) / Concentración
        double volumenMl = (dosisMg * volumenPres) / concentracion;

        return new ResultadoCalculo(dosisMg, volumenMl, estado, mensaje);
    }

    public static ResultadoCalculo calcularPorPeso(double pesoKg, double dosisMgKg, ReglaDosificacion regla) {
        double concMg = (regla != null && regla.concentracion_mg != null && regla.concentracion_mg > 0) ? regla.concentracion_mg : 250.0;
        double volMl = (regla != null && regla.volumen_ml != null && regla.volumen_ml > 0) ? regla.volumen_ml : 5.0;
        return calcularPorPeso(pesoKg, dosisMgKg, concMg, volMl, regla);
    }

    // --- FÓRMULAS CLÁSICAS DE AJUSTE (PEDIATRÍA HISTÓRICA) ---

    /**
     * Regla de Fried (para bebés menores de 2 años): Dosis = (Edad en meses * Dosis Adulto) / 150
     */
    public static double reglaDeFried(double edadMeses, double dosisAdultoMg) {
        if (edadMeses <= 0 || dosisAdultoMg <= 0) return 0.0;
        return (edadMeses * dosisAdultoMg) / 150.0;
    }

    /**
     * Regla de Young (para niños de 2 a 12 años): Dosis = (Edad en años * Dosis Adulto) / (Edad en años + 12)
     */
    public static double reglaDeYoung(double edadAnos, double dosisAdultoMg) {
        if (edadAnos <= 0 || dosisAdultoMg <= 0) return 0.0;
        return (edadAnos * dosisAdultoMg) / (edadAnos + 12.0);
    }

    /**
     * Regla de Clark (basada en peso en libras): Dosis = (Peso en libras * Dosis Adulto) / 150
     */
    public static double reglaDeClark(double pesoLibras, double dosisAdultoMg) {
        if (pesoLibras <= 0 || dosisAdultoMg <= 0) return 0.0;
        return (pesoLibras * dosisAdultoMg) / 150.0;
    }

    /**
     * Calcula la Superficie Corporal (BSA) mediante la fórmula de Mosteller.
     */
    public static double calcularBSA(double pesoKg, double alturaCm) {
        if (pesoKg <= 0 || alturaCm <= 0) {
            return 0.0;
        }
        return Math.sqrt((pesoKg * alturaCm) / 3600.0);
    }

    /**
     * Calcula el goteo de infusión intravenosa en gotas/min o microgotas/min.
     */
    public static double calcularGoteoIv(double volumenMl, double tiempoHoras, boolean esMicrogoteo) {
        if (volumenMl <= 0 || tiempoHoras <= 0) {
            return 0.0;
        }
        if (esMicrogoteo) {
            return volumenMl / tiempoHoras;
        } else {
            return (volumenMl * 20.0) / (tiempoHoras * 60.0);
        }
    }
}
