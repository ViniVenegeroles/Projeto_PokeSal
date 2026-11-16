package pokeucsal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

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
