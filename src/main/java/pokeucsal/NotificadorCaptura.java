package pokeucsal;

/** Avisa que um Pokésal foi capturado. Uma falha aqui não desfaz a captura. */
public interface NotificadorCaptura {
    /**
     * Avisa a captura.
     *
     * @param treinador quem capturou
     * @param pokesalCapturado nome do Pokésal capturado
     * @throws Exception se o aviso falhar
     */
    void notificarCaptura(String treinador, String pokesalCapturado) throws Exception;
}
