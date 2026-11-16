package pokeucsal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Treinador {
    private final String nome;
    private Pokemon pokemon; // o Pokésal "em campo" (pode ser trocado entre lutas do torneio)
    // Time do treinador: o inicial + os capturados no torneio. Sempre contém o Pokésal em campo.
    private final List<Pokemon> equipe = new ArrayList<>();
    private final Bag bag;
    // true = o computador escolhe os golpes. Antes, no torneio, até as lutas entre adversários
    // (CPU x CPU) ficavam esperando o jogador digitar os golpes no teclado.
    private final boolean cpu;

    public Treinador(String nome, Pokemon pokemon) { this(nome, pokemon, false); }

    public Treinador(String nome, Pokemon pokemon, boolean cpu) {
        this.nome = nome;
        this.pokemon = pokemon;
        this.equipe.add(pokemon);
        this.cpu = cpu;
        this.bag = new Bag();
    }
    public String getNome() { return nome; }
    public Pokemon getPokemon() { return pokemon; }
    public Bag getBag() { return bag; }
    public boolean isCpu() { return cpu; }

    /** Visão somente-leitura do time (o Pokésal inicial vem primeiro, depois os capturados na ordem). */
    public List<Pokemon> getEquipe() { return Collections.unmodifiableList(equipe); }

    /**
     * Adiciona um Pokésal ao time. Devolve false se ele já era do time (ou se veio null).
     * O Pokésal capturado entra com HP cheio e sem status: um Pokésal desmaiado não serviria para lutar.
     */
    public boolean capturar(Pokemon capturado) {
        if (capturado == null || equipe.contains(capturado)) return false;
        capturado.restaurar();
        equipe.add(capturado);
        return true;
    }

    /** Coloca outro Pokésal do time em campo. Só aceita quem já está no time. */
    public void trocarPokemon(Pokemon novo) {
        if (!equipe.contains(novo)) {
            throw new IllegalArgumentException(nome + " não possui " + (novo == null ? "null" : novo.getNome()) + " no time.");
        }
        this.pokemon = novo;
    }
}
