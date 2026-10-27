package test.modelo;

public class MateriaAsignada {
    private final String codigo;
    private final String materia;
    private final String grado;

    public MateriaAsignada(String codigo, String materia, String grado) {
        this.codigo = codigo;
        this.materia = materia;
        this.grado = grado;
    }

    public String getCodigo() { return codigo; }
    public String getMateria() { return materia; }
    public String getGrado() { return grado; }
}
