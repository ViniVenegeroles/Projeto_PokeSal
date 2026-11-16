package pokeucsal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Ajuda dos testes: roda um trecho com o console "de verdade" ligado (sem silêncio e sem pausas) e devolve o que foi impresso. */
final class ConsoleTestes {
    private ConsoleTestes() { }

    static String comConsole(Runnable trecho) {
        PrintStream original = System.out;
        boolean silenciosoAntes = Batalha.modoTeste;
        boolean semPausasAntes = Batalha.semPausas;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Batalha.modoTeste = false;
            Batalha.semPausas = true;
            trecho.run();
        } finally {
            System.setOut(original);
            Batalha.modoTeste = silenciosoAntes;
            Batalha.semPausas = semPausasAntes;
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
