package br.csi.Listener;

import br.csi.Model.User;

public interface UserListener {

    /**
     * Notifica que um usuário foi adicionado
     * @param usuario
     */
    void usuarioAdicionado(User usuario);

    /**
     * Notifica que um usuário foi removido
     * @param usuario
     */
    void usuarioRemovido(User usuario);

    /**
     * Notifica que um usuário foi alterado
     * @param usuario
     */
    void usuarioAlterado(User usuario);

}
