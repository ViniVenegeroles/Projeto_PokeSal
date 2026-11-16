package pokeucsal;

import java.util.ArrayList;
import java.util.List;

/** RegistroBatalha de mentira que só guarda os eventos, na ordem em que chegam, como texto "tipo:a:b". */
final class RegistroEmMemoria implements RegistroBatalha {
    final List<String> eventos = new ArrayList<>();

    @Override public void registrarAtaque(String atacante, String golpe) { eventos.add("ataque:" + atacante + ":" + golpe); }
    @Override public void registrarDano(String defensor, int dano) { eventos.add("dano:" + defensor + ":" + dano); }
    @Override public void registrarEfeitoStatus(String alvo, String status) { eventos.add("status:" + alvo + ":" + status); }
    @Override public void registrarEfeitoTerreno(String pokemon, String efeito) { eventos.add("terreno:" + pokemon + ":" + efeito); }
    @Override public void registrarUsoItem(String treinador, String item) { eventos.add("item:" + treinador + ":" + item); }

    long quantos(String evento) { return eventos.stream().filter(evento::equals).count(); }
}
