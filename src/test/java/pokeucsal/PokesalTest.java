package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PokesalTest {
    private Pokesal p;

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
        p = new Pokesal("CharSal", new TipoFogo(), 50, 40, 150, 80, new Golpe[]{});
    }

    @AfterEach
    void tearDown() {
        Batalha.modoTeste = false;
    }

    @Test
    void testAntidotoDevolveAVelocidadePerdidaNaParalisia() {
        p.setParalisado(true);
        assertEquals(60, p.getSpd());

        p.curarStatus();

        assertFalse(p.isParalisado());
        assertEquals(80, p.getSpd());
    }

    @Test
    void testParalisiaNaoAcumulaSeJaEstaParalisado() {
        p.setParalisado(true);
        p.setParalisado(true);
        assertEquals(60, p.getSpd());
    }

    @Test
    void testRestaurarDesfazBuffsDebuffsPrecisaoEStatus() {
        p.getTipo().apBf(p);
        p.diminuirPrecisao(10);
        p.setParalisado(true);
        p.setQueimado(true);
        p.dano(100);

        p.restaurar();

        assertEquals(50, p.getAtk());
        assertEquals(40, p.getDef());
        assertEquals(80, p.getSpd());
        assertEquals(100, p.getPrecisao());
        assertEquals(150, p.getHp());
        assertFalse(p.isQueimado());
        assertFalse(p.isParalisado());
        assertFalse(p.isEnvenenado());
    }
}
