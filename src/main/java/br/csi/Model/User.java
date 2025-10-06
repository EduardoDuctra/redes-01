package br.csi.Model;

public class User {
    private String nome;
    private String status;
    private long ultimoSinal;

    public User(String nome, String status, long ultimoSinal) {
        this.nome = nome;
        this.status = status;
        this.ultimoSinal = ultimoSinal;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
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
