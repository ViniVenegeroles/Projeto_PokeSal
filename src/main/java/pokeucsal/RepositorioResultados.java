package pokeucsal;

/** Guarda o resultado de cada batalha. */
public interface RepositorioResultados {
    /**
     * Guarda um resultado.
     *
     * @param resultado resultado da batalha
     * @throws Exception se não for possível guardar
     */
    void salvarResultado(ResultadoBatalha resultado) throws Exception;
}
