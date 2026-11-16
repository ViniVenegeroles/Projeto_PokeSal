package pokeucsal;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RegistroBatalhaEmMemoriaTest {
    @Test
    void testGuardaOsEventosNaOrdemEmQueChegam() {
        RegistroBatalhaEmMemoria registro = new RegistroBatalhaEmMemoria();
        registro.registrarAtaque("CharSal", "Brasas");
        registro.registrarDano("BulbaSal", 47);
        registro.registrarEfeitoStatus("BulbaSal", "Queimado");
        registro.registrarEfeitoTerreno("Campo", "Sal Shard ativou: Piso Escorregadio");
        registro.registrarUsoItem("Jogador", "Potion");

        assertEquals(Arrays.asList("ataque:CharSal:Brasas", "dano:BulbaSal:47", "status:BulbaSal:Queimado",
                "terreno:Campo:Sal Shard ativou: Piso Escorregadio", "item:Jogador:Potion"), registro.getEventos());
    }

    @Test
    void testListaDeEventosEhSomenteLeitura() {
        RegistroBatalhaEmMemoria registro = new RegistroBatalhaEmMemoria();
        assertThrows(UnsupportedOperationException.class, () -> registro.getEventos().add("x"));
    }
}
