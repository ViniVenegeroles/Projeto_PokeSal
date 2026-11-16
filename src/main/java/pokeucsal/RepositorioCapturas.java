package pokeucsal;

/** Guarda cada captura feita no torneio. */
public interface RepositorioCapturas {
    /**
     * Guarda uma captura.
     *
     * @param treinador quem capturou
     * @param pokesalCapturado nome do Pokésal capturado
     * @throws Exception se não for possível guardar
     */
    void salvarCaptura(String treinador, String pokesalCapturado) throws Exception;
}
