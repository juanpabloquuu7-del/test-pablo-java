package test.modelo;

public class ProfesorInfo {
    private final int id;
    private final String tipoDocumento;
    private final String numeroDocumento;
    private final String nombreCompleto;

    public ProfesorInfo(int id, String tipoDocumento, String numeroDocumento, String nombreCompleto) {
        this.id = id;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombreCompleto = nombreCompleto;
    }

    public int getId() { return id; }
    public String getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public String getNombreCompleto() { return nombreCompleto; }
}
