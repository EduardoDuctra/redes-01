package br.csi;

import org.json.JSONObject;
public class TestJson {
    public static void main(String[] args) {
        // Criar um objeto JSON
        JSONObject obj = new JSONObject();
        obj.put("tipoMensagem", "sonda");
        obj.put("usuario", "Joao");
        obj.put("status", "Disponível");
        // Converter para string JSON
        String jsonString = obj.toString();
        // Imprimir JSON
        System.out.println(jsonString);
    }
}
