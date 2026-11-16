package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Dano, cura e status do Pokésal. Classe sob teste = não se mocka; só o registro é um recorder em memória. */
class PokemonStatusTest {
    private RegistroEmMemoria registro;
    private Pokemon p;

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
        registro = new RegistroEmMemoria();
        p = new Pokemon("CharSal", new TipoFogo(), 50, 40, 160, 80, new Golpe[]{});
    }

    @AfterEach
    void tearDown() { Batalha.modoTeste = false; }

    @Test
    void testQueimaduraTiraUmDezesseisAvosDoHpMaximoPorTurno() {
        p.setQueimado(true);
        p.processarStatusFimDeTurno(registro);
        assertEquals(150, p.getHp());
        assertEquals(1, registro.quantos("status:CharSal:Dano Queimadura"));
    }

    @Test
    void testVenenoCausaDanoProgressivoEReiniciaAoEnvenenarDeNovo() {
        p.setEnvenenado(true);
        p.processarStatusFimDeTurno(registro);   // 10
        p.processarStatusFimDeTurno(registro);   // 20
        assertEquals(130, p.getHp());

        p.setEnvenenado(true);                   // reinicia a contagem
        p.processarStatusFimDeTurno(registro);   // volta a 10
        assertEquals(120, p.getHp());
        assertEquals(3, registro.quantos("status:CharSal:Dano Veneno"));
    }

    @Test
    void testSemStatusNadaAcontece() {
        p.processarStatusFimDeTurno(registro);
        assertEquals(160, p.getHp());
        assertTrue(registro.eventos.isEmpty());
    }

    @Test
    void testDanoDeStatusTemMinimoDeUmEHpNuncaFicaNegativo() {
        Pokemon fraco = new Pokemon("Fraquinho", new TipoAgua(), 10, 10, 10, 10, new Golpe[]{});
        fraco.setQueimado(true);                 // 10 / 16 = 0, mas o mínimo é 1
        for (int i = 0; i < 12; i++) fraco.processarStatusFimDeTurno(registro);
        assertEquals(0, fraco.getHp());
    }

    @Test
    void testDanoMinimoDeDoisEMaximoDe85PorCentoDoHp() {
        p.dano(0);                               // 0 - 13 de mitigação = negativo; vale o mínimo de 2
        assertEquals(158, p.getHp());

        p.dano(10000);                           // limitado a 85% do HP máximo
        assertEquals(158 - (int) (p.getMaxHp() * 0.85), p.getHp());

        p.dano(10000);
        p.dano(10000);
        assertEquals(0, p.getHp());              // HP nunca fica negativo
    }

    @Test
    void testCurarNaoPassaDoHpMaximo() {
        p.dano(10000);
        int antes = p.getHp();
        p.curar(0.5);
        assertEquals(antes + (int) (p.getMaxHp() * 0.5), p.getHp());
        p.curar(1.0);
        assertEquals(160, p.getHp());
    }

    @Test
    void testCurarStatusLimpaTudoEDevolveAVelocidade() {
        p.setQueimado(true);
        p.setEnvenenado(true);
        p.setParalisado(true);
        assertEquals(60, p.getSpd());

        p.curarStatus();

        assertFalse(p.isQueimado());
        assertFalse(p.isEnvenenado());
        assertFalse(p.isParalisado());
        assertEquals(80, p.getSpd());
    }

    @Test
    void testDesligarStatusIndividualmente() {
        p.setQueimado(true); p.setQueimado(false);
        p.setEnvenenado(true); p.setEnvenenado(false);
        p.setParalisado(true); p.setParalisado(false);
        assertFalse(p.isQueimado());
        assertFalse(p.isEnvenenado());
        assertFalse(p.isParalisado());
        assertEquals(80, p.getSpd());
    }

    @Test
    void testAtributosNuncaFicamNegativos() {
        p.setAtk(-5); p.setDef(-1); p.setSpd(-3);
        assertEquals(0, p.getAtk());
        assertEquals(0, p.getDef());
        assertEquals(0, p.getSpd());
    }

    @Test
    void testPrecisaoCaiMasNaoPassaDoMinimo() {
        assertEquals(100, p.getPrecisao());
        p.diminuirPrecisao(10);
        assertEquals(90, p.getPrecisao());
        p.diminuirPrecisao(500);
        assertEquals(Pokemon.PRECISAO_MINIMA, p.getPrecisao());
    }

    @Test
    void testGettersBasicos() {
        assertEquals("Fogo", p.getTipo().getNomeTipo());
        assertEquals(160, p.getMaxHp());
        assertEquals(0, p.getGolpes().length);
    }

    @Test
    void testMensagensDeConsole() {
        String saida = ConsoleTestes.comConsole(() -> {
            p.setQueimado(true);
            p.setEnvenenado(true);
            p.setParalisado(true);
            p.diminuirPrecisao(10);
            p.processarStatusFimDeTurno(registro);
            p.dano(30);
            p.exibirSts();
        });
        assertTrue(saida.contains("CharSal foi queimado!"));
        assertTrue(saida.contains("CharSal foi envenenado!"));
        assertTrue(saida.contains("CharSal foi paralisado! Velocidade caiu."));
        assertTrue(saida.contains("Precisão de CharSal Caiu Para 90%"));
        assertTrue(saida.contains("Dano por Queimadura"));
        assertTrue(saida.contains("Dano por Veneno Acumulado"));
        assertTrue(saida.contains("CharSal recebeu"));
        assertTrue(saida.contains("--- Status de CharSal ---"));
    }
}
