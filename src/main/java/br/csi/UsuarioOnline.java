package br.csi;

public class UsuarioOnline {
    private String usuario;
    private String status;
    private long ultimoSinal;

    public UsuarioOnline(String usuario, String status, long ultimoSinal) {
        this.usuario = usuario;
        this.status = status;
        this.ultimoSinal = ultimoSinal;
    }

    public String getUsuario() {
        return usuario;
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
}
