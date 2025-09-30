package br.csi.Model;

import java.io.Serializable;
import java.time.Instant;
import org.json.JSONObject;


public class Mensagem implements Serializable {

    private TipoMensagem tipo;
    private String remetente;
    private String destinatario; // null para mensagens de grupo ou sonda
    private String conteudo;     // texto da mensagem ou info da sonda
    private long timestamp;

    // Construtor geral
    public Mensagem(TipoMensagem tipo, String remetente, String destinatario, String conteudo) {
        this.tipo = tipo;
        this.remetente = remetente;
        this.destinatario = destinatario;
        this.conteudo = conteudo;
        this.timestamp = Instant.now().toEpochMilli();
    }

    // ===== Getters e Setters =====
    public TipoMensagem getTipo() { return tipo; }
    public void setTipo(TipoMensagem tipo) { this.tipo = tipo; }

    public String getRemetente() { return remetente; }
    public void setRemetente(String remetente) { this.remetente = remetente; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }

    public long getTimestamp() { return timestamp; }

    // ===== Métodos de fábrica para criar mensagens =====
    public static Mensagem criarSonda(String remetente, String status) {
        return new Mensagem(TipoMensagem.SONDA, remetente, null, status);
    }

    public static Mensagem criarMsgIndividual(String remetente, String destinatario, String texto) {
        return new Mensagem(TipoMensagem.MSG_INDIVIDUAL, remetente, destinatario, texto);
    }

    public static Mensagem criarMsgGrupo(String remetente, String texto) {
        return new Mensagem(TipoMensagem.MSG_GRUPO, remetente, null, texto);
    }

    public static Mensagem criarFimChat(String remetente, String destinatario) {
        return new Mensagem(TipoMensagem.FIM_CHAT, remetente, destinatario, null);
    }

    // ===== Serialização / Desserialização JSON =====
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("tipo", tipo.name());
        json.put("remetente", remetente);
        json.put("destinatario", destinatario);
        json.put("conteudo", conteudo);
        json.put("timestamp", timestamp);
        return json;
    }

    public static Mensagem fromJson(JSONObject json) {
        TipoMensagem tipo = TipoMensagem.valueOf(json.getString("tipo"));
        String remetente = json.getString("remetente");
        String destinatario = json.optString("destinatario", null);
        String conteudo = json.optString("conteudo", null);
        long timestamp = json.optLong("timestamp", Instant.now().toEpochMilli());

        Mensagem msg = new Mensagem(tipo, remetente, destinatario, conteudo);
        msg.timestamp = timestamp;
        return msg;
    }
}
