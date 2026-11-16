package pokeucsal;

public class ResultadoBatalha {
    private final String vencedor;
    private final String perdedor;
    private final int turnos;
    private final String pokemonVencedor;
    private final String pokemonPerdedor;

    public ResultadoBatalha(String v, String p, int t, String pv, String pp) {
        this.vencedor = v; this.perdedor = p; this.turnos = t;
        this.pokemonVencedor = pv; this.pokemonPerdedor = pp;
    }
    public String getVencedor() { return vencedor; }
    public String getPerdedor() { return perdedor; }
    public int getTurnos() { return turnos; }
    // Estes dois getters não existiam: os Pokésals usados eram guardados mas ninguém conseguia lê-los
    // (a regra 2.3 exige "Pokésal utilizados" no resultado, e o ArgumentCaptor dos testes precisa lê-los).
    public String getPokemonVencedor() { return pokemonVencedor; }
    public String getPokemonPerdedor() { return pokemonPerdedor; }

    @Override
    public String toString() {
        return vencedor + " (" + pokemonVencedor + ") venceu " + perdedor + " (" + pokemonPerdedor + ") em " + turnos + " turno(s)";
    }
}
