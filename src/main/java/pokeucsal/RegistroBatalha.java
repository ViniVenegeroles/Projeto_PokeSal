package pokeucsal;

/**
 * Recebe cada evento do turno, na ordem: ataque, dano, efeito de status, efeito de terreno e uso de item.
 */
public interface RegistroBatalha {
    /**
     * Registra um ataque.
     *
     * @param atacante quem atacou
     * @param golpe golpe usado
     */
    void registrarAtaque(String atacante, String golpe);
    /**
     * Registra o dano sofrido.
     *
     * @param defensor quem recebeu
     * @param dano dano causado
     */
    void registrarDano(String defensor, int dano);
    /**
     * Registra um efeito de status.
     *
     * @param alvo Pokésal afetado
     * @param status efeito aplicado
     */
    void registrarEfeitoStatus(String alvo, String status);
    /**
     * Registra um efeito de terreno.
     *
     * @param pokesal Pokésal afetado
     * @param efeito efeito aplicado
     */
    void registrarEfeitoTerreno(String pokesal, String efeito);
    /**
     * Registra o uso de um item.
     *
     * @param treinador quem usou
     * @param item item usado
     */
    void registrarUsoItem(String treinador, String item);
}
