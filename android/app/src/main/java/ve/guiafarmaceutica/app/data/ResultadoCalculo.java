package ve.guiafarmaceutica.app.data;

public class ResultadoCalculo {

    public enum EstadoSemaforo {
        VERDE, NARANJA, ROJO
    }

    public double dosisMg;
    public double volumenMl;
    public double gotasMin;
    public double bsaM2;
    public EstadoSemaforo estado;
    public String mensajeAlerta;

    public ResultadoCalculo(double dosisMg, double volumenMl, EstadoSemaforo estado, String mensajeAlerta) {
        this.dosisMg = dosisMg;
        this.volumenMl = volumenMl;
        this.estado = estado;
        this.mensajeAlerta = mensajeAlerta;
    }
}
