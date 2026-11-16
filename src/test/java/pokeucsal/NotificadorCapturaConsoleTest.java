package pokeucsal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificadorCapturaConsoleTest {
    @Test
    void testAnunciaQuemCapturouEOQue() {
        String saida = ConsoleTestes.comConsole(() -> new NotificadorCapturaConsole().notificarCaptura("Jogador", "BulbaSal"));
        assertTrue(saida.contains("BulbaSal foi capturado e entrou para o time de Jogador!"));
    }
}
