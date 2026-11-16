package pokeucsal;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GeradorAleatorioPadraoTest {
    @Test
    void testPrecisaoFicaEntreZeroENoventaENove() {
        GeradorAleatorioPadrao gerador = new GeradorAleatorioPadrao(new Random(7));
        for (int i = 0; i < 1000; i++) {
            int precisao = gerador.sortearPrecisao();
            assertTrue(precisao >= 0 && precisao <= 99);
        }
    }

    @Test
    void testChanceFicaEntreZeroEUmSemIncluirUm() {
        GeradorAleatorioPadrao gerador = new GeradorAleatorioPadrao(new Random(7));
        for (int i = 0; i < 1000; i++) {
            double chance = gerador.sortearChance();
            assertTrue(chance >= 0.0 && chance < 1.0);
        }
    }

    @Test
    void testMesmaSementeDaOsMesmosSorteios() {
        GeradorAleatorioPadrao a = new GeradorAleatorioPadrao(new Random(3));
        GeradorAleatorioPadrao b = new GeradorAleatorioPadrao(new Random(3));
        assertEquals(a.sortearPrecisao(), b.sortearPrecisao());
        assertEquals(a.sortearChance(), b.sortearChance());
    }

    @Test
    void testConstrutorPadraoFunciona() {
        GeradorAleatorioPadrao gerador = new GeradorAleatorioPadrao();
        int precisao = gerador.sortearPrecisao();
        assertTrue(precisao >= 0 && precisao <= 99);
    }
}
