package pokeucsal;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TerrenosTest {
    private static Clock as(String horaUtc) {
        return Clock.fixed(Instant.parse("2026-11-13T" + horaUtc + "Z"), ZoneOffset.UTC);
    }

    @Test
    void testFronteirasDoDia() {
        assertFalse(Terrenos.ehDia(as("05:59:00")));
        assertTrue(Terrenos.ehDia(as("06:00:00")));
        assertTrue(Terrenos.ehDia(as("17:59:00")));
        assertFalse(Terrenos.ehDia(as("18:00:00")));
        assertFalse(Terrenos.ehDia(as("00:00:00")));
    }

    @Test
    void testTodosOsTerrenosDoSalShardIncluemOAsfaltoMesmoDeNoite() {
        List<String> terrenos = Terrenos.todos();
        assertEquals(3, terrenos.size());
        assertTrue(terrenos.contains(Terrenos.ASFALTO_QUENTE));
        assertTrue(terrenos.contains(Terrenos.PISO_ESCORREGADIO));
        assertTrue(terrenos.contains(Terrenos.CANTEIRO_CENTRAL));
        assertFalse(terrenos.contains(Terrenos.NEUTRO));
    }
}
