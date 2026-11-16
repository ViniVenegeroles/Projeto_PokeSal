package pokeucsal;

/** Guarda cada captura feita no torneio (colaborador externo do requisito de captura). Pode falhar: ver Torneio. */
public interface RepositorioCapturas {
    void salvarCaptura(String treinador, String pokemonCapturado) throws Exception;
}
