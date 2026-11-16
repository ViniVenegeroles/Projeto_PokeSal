package pokeucsal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Registro que guarda os eventos da batalha em memória, na ordem em que chegam. */
public class RegistroBatalhaEmMemoria implements RegistroBatalha {
    private final List<String> eventos = new ArrayList<>();

    @Override public void registrarAtaque(String atacante, String golpe) {
        eventos.add("ataque:" + atacante + ":" + golpe);
    }
    @Override public void registrarDano(String defensor, int dano) {
        eventos.add("dano:" + defensor + ":" + dano);
    }
    @Override public void registrarEfeitoStatus(String alvo, String status) {
        eventos.add("status:" + alvo + ":" + status);
    }
    @Override public void registrarEfeitoTerreno(String pokesal, String efeito) {
        eventos.add("terreno:" + pokesal + ":" + efeito);
    }
    @Override public void registrarUsoItem(String treinador, String item) {
        eventos.add("item:" + treinador + ":" + item);
    }

    /** Devolve os eventos registrados (somente leitura). */
    public List<String> getEventos() {
        return Collections.unmodifiableList(eventos);
    }
}
