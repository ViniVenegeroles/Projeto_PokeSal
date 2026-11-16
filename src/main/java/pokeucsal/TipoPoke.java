package pokeucsal;

/** Tipo elemental de um Pokésal (Fogo, Água ou Planta). */
public interface TipoPoke {
    /** Devolve o nome do tipo. */
    String getNomeTipo();
    /**
     * Calcula o multiplicador de dano contra outro tipo.
     *
     * @param defensor tipo de quem recebe o golpe
     * @return 2.0 com vantagem, 0.5 com desvantagem, 1.0 nos demais casos
     */
    double calcMult(TipoPoke defensor);
    /**
     * Aplica o buff do tipo em quem usou o golpe de suporte.
     *
     * @param usuario Pokésal que usou o golpe
     */
    void apBf(Pokesal usuario);
    /**
     * Aplica o debuff do tipo no alvo do golpe.
     *
     * @param alvo Pokésal que recebe o enfraquecimento
     */
    void apDb(Pokesal alvo);
}
