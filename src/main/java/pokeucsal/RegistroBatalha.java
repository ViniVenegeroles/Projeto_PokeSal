package pokeucsal;
public interface RegistroBatalha {
    void registrarAtaque(String atacante, String golpe);
    void registrarDano(String defensor, int dano);
    void registrarEfeitoStatus(String alvo, String status);
    void registrarEfeitoTerreno(String pokemon, String efeito);
    void registrarUsoItem(String treinador, String item);
}
