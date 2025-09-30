package br.csi.Listener;

import br.csi.Model.User;

public interface MessageListener {

    void mensagemRecebida(String mensagem, User remetente, boolean chatGeral);
}
