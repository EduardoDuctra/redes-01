package br.csi.Listener;

import br.csi.Model.User;

public interface UDPService {


    void enviarMensagem(String mensagem, User destinatario, boolean chatGeral);

    //está sendo usada na classe ChatService
    void usuarioAlterado(User usuario);

    void addListenerMensagem(MessageListener listener);

    void addListenerUsuario(UserListener listener);



}
