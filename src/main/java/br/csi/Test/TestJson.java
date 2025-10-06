package br.csi.Test;

import org.json.JSONObject;
public class TestJson {
    public static void main(String[] args) {
        JSONObject obj = new JSONObject();
        obj.put("tipoMensagem", "sonda");
        obj.put("usuario", "Joao");
        obj.put("status", "Disponível");
        String jsonString = obj.toString();
        System.out.println(jsonString);
    }
}
