package pokeucsal;

/** Resultado de uma batalha: vencedor, perdedor, número de turnos e Pokésals usados. */
public class ResultadoBatalha {
    private final String vencedor;
    private final String perdedor;
    private final int turnos;
    private final String pokesalVencedor;
    private final String pokesalPerdedor;

    /**
     * Cria o resultado.
     *
     * @param v nome do vencedor
     * @param p nome do perdedor
     * @param t número de turnos
     * @param pv Pokésal do vencedor
     * @param pp Pokésal do perdedor
     */
    public ResultadoBatalha(String v, String p, int t, String pv, String pp) {
        this.vencedor = v;
        this.perdedor = p;
        this.turnos = t;
        this.pokesalVencedor = pv;
        this.pokesalPerdedor = pp;
    }
    /** Devolve o nome do vencedor. */
    public String getVencedor() {
        return vencedor;
    }
    /** Devolve o nome do perdedor. */
    public String getPerdedor() {
        return perdedor;
    }
    /** Devolve o número de turnos. */
    public int getTurnos() {
        return turnos;
    }

    /** Devolve o nome do Pokésal do vencedor. */
    public String getPokesalVencedor() {
        return pokesalVencedor;
    }
    /** Devolve o nome do Pokésal do perdedor. */
    public String getPokesalPerdedor() {
        return pokesalPerdedor;
    }

    @Override
    public String toString() {
        return vencedor + " (" + pokesalVencedor + ") venceu " + perdedor + " (" + pokesalPerdedor + ") em " + turnos + " turno(s)";
    }
}
