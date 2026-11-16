package pokeucsal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Treinador com um time de Pokésals (o inicial mais os capturados) e uma mochila. */
public class Treinador {
    private final String nome;
    private Pokesal pokesal;

    private final List<Pokesal> equipe = new ArrayList<>();
    private final Bag bag;

    private final boolean cpu;

    /**
     * Cria um treinador humano.
     *
     * @param nome nome do treinador
     * @param pokesal Pokésal inicial
     */
    public Treinador(String nome, Pokesal pokesal) {
        this(nome, pokesal, false);
    }

    /**
     * Cria um treinador.
     *
     * @param nome nome do treinador
     * @param pokesal Pokésal inicial
     * @param cpu true se o computador escolhe os golpes
     */
    public Treinador(String nome, Pokesal pokesal, boolean cpu) {
        this.nome = nome;
        this.pokesal = pokesal;
        this.equipe.add(pokesal);
        this.cpu = cpu;
        this.bag = new Bag();
    }
    /** Devolve o nome do treinador. */
    public String getNome() {
        return nome;
    }
    /** Devolve o Pokésal em campo. */
    public Pokesal getPokesal() {
        return pokesal;
    }
    /** Devolve a mochila. */
    public Bag getBag() {
        return bag;
    }
    /** Informa se o computador escolhe os golpes deste treinador. */
    public boolean isCpu() {
        return cpu;
    }

    /** Devolve o time (o inicial primeiro, depois os capturados), somente leitura. */
    public List<Pokesal> getEquipe() {
        return Collections.unmodifiableList(equipe);
    }

    /**
     * Adiciona um Pokésal ao time, com HP cheio e sem status.
     *
     * @param capturado Pokésal capturado
     * @return false se ele já era do time ou se veio nulo
     */
    public boolean capturar(Pokesal capturado) {
        if (capturado == null || equipe.contains(capturado)) {
            return false;
        }
        capturado.restaurar();
        equipe.add(capturado);
        return true;
    }

    /**
     * Coloca outro Pokésal do time em campo.
     *
     * @param novo Pokésal que entra em campo
     * @throws IllegalArgumentException se ele não estiver no time
     */
    public void trocarPokesal(Pokesal novo) {
        if (!equipe.contains(novo)) {
            throw new IllegalArgumentException(nome + " não possui " + (novo == null ? "null" : novo.getNome()) + " no time.");
        }
        this.pokesal = novo;
    }
}
