package pokeucsal;

import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Centraliza os nomes dos terrenos e a regra "Asfalto Quente só existe de dia".
 *
 * Por quê: antes os nomes ("Asfalto Quente", "Piso Escorregadio"...) estavam digitados como
 * texto solto em Main, Batalha e Golpe. Um erro de digitação em qualquer um deles
 * faria o bônus de terreno simplesmente nunca ativar, sem nenhum erro de compilação.
 */
public final class Terrenos {
    public static final String ASFALTO_QUENTE = "Asfalto Quente";
    public static final String PISO_ESCORREGADIO = "Piso Escorregadio";
    public static final String CANTEIRO_CENTRAL = "Canteiro Central";
    /** Terreno "sem efeito", usado como valor padrão se o serviço de terreno falhar ou devolver null. */
    public static final String NEUTRO = "Terreno Neutro";

    private static final int HORA_AMANHECER = 6;   // dia: 06:00 até 17:59
    private static final int HORA_ANOITECER = 18;

    private Terrenos() { }

    /** O horário SEMPRE vem do Clock recebido (e não de new Date()), para poder ser mockado nos testes. */
    public static boolean ehDia(Clock relogio) {
        int hora = LocalTime.now(relogio).getHour();
        return hora >= HORA_AMANHECER && hora < HORA_ANOITECER;
    }

    /** Terrenos que podem existir agora: de noite o Asfalto Quente fica de fora. */
    public static List<String> disponiveis(boolean dia) {
        List<String> lista = new ArrayList<>();
        if (dia) lista.add(ASFALTO_QUENTE);
        lista.add(PISO_ESCORREGADIO);
        lista.add(CANTEIRO_CENTRAL);
        return lista;
    }
}
