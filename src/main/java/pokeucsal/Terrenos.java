package pokeucsal;

import java.time.Clock;
import java.time.LocalTime;
import java.util.List;

/** Nomes dos terrenos e a regra de que o Asfalto Quente só existe de dia. */
public final class Terrenos {
    public static final String ASFALTO_QUENTE = "Asfalto Quente";
    public static final String PISO_ESCORREGADIO = "Piso Escorregadio";
    public static final String CANTEIRO_CENTRAL = "Canteiro Central";

    public static final String NEUTRO = "Terreno Neutro";

    private static final int HORA_AMANHECER = 6;
    private static final int HORA_ANOITECER = 18;

    private Terrenos() { }

    /**
     * Informa se é dia (das 6h às 17h59) segundo o relógio recebido.
     *
     * @param relogio relógio que informa o horário
     * @return true se for dia
     */
    public static boolean ehDia(Clock relogio) {
        int hora = LocalTime.now(relogio).getHour();
        return hora >= HORA_AMANHECER && hora < HORA_ANOITECER;
    }

    /**
     * Devolve os três terrenos com efeito, sem o terreno neutro.
     *
     * @return lista com Asfalto Quente, Piso Escorregadio e Canteiro Central
     */
    public static List<String> todos() {
        return List.of(ASFALTO_QUENTE, PISO_ESCORREGADIO, CANTEIRO_CENTRAL);
    }
}
