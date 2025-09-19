package br.csi;

public class UsuarioOnline {
    private String usuario;
    private String status;
    private long ultimoSinal; // timestamp do último sinal recebido

    public UsuarioOnline(String usuario, String status, long ultimoSinal) {
        this.usuario = usuario;
        this.status = status;
        this.ultimoSinal = ultimoSinal;
    }

    // Getters e Setters
    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getUltimoSinal() {
        return ultimoSinal;
    }

    public void setUltimoSinal(long ultimoSinal) {
        this.ultimoSinal = ultimoSinal;
    }

    @Override
    public String toString() {
        return "UsuarioOnline{" +
                "usuario='" + usuario + '\'' +
                ", status='" + status + '\'' +
                ", ultimoSinal=" + ultimoSinal +
                '}';
    }
}
