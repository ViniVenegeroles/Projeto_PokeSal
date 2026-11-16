package pokeucsal;
public interface TipoPoke {
    String getNomeTipo();
    double calcMult(TipoPoke defensor);
    void apBf(Pokemon usuario);
    void apDb(Pokemon alvo);
}
