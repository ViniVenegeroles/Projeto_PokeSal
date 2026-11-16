package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

class GolpeEfeitosTest {
    private static final GeradorFixo ACERTA_SEM_CRITICO_NEM_STATUS = new GeradorFixo(0, 0.99);
    private static final GeradorFixo ACERTA_E_APLICA_STATUS = new GeradorFixo(0, 0.1);
    private static final GeradorFixo ACERTA_COM_CRITICO = new GeradorFixo(0, 0.01);
    private static final GeradorFixo ERRA = new GeradorFixo(100, 0.99);

    private RegistroEmMemoria registro;

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
        registro = new RegistroEmMemoria();
    }

    @AfterEach
    void tearDown() {
        Batalha.modoTeste = false;
    }

    private static Pokesal pokesal(String nome, TipoPoke tipo) {
        return new Pokesal(nome, tipo, 50, 40, 150, 50, new Golpe[]{});
    }

    @Test
    void testElementalDeFogoQueimaOAlvo() {
        Pokesal alvo = pokesal("Alvo", new TipoPlanta());
        new Golpe.Elemental("Brasas", 35).executar(pokesal("CharSal", new TipoFogo()), alvo, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);
        assertTrue(alvo.isQueimado());
        assertEquals(1, registro.quantos("status:Alvo:Queimado"));
    }

    @Test
    void testElementalDePlantaEnvenenaOAlvo() {
        Pokesal alvo = pokesal("Alvo", new TipoAgua());
        new Golpe.Elemental("Cansanção", 35).executar(pokesal("BulbaSal", new TipoPlanta()), alvo, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);
        assertTrue(alvo.isEnvenenado());
        assertEquals(1, registro.quantos("status:Alvo:Envenenado"));
    }

    @Test
    void testElementalDeAguaParalisaOAlvo() {
        Pokesal alvo = pokesal("Alvo", new TipoFogo());
        new Golpe.Elemental("Jato", 35).executar(pokesal("SquirtSal", new TipoAgua()), alvo, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);
        assertTrue(alvo.isParalisado());
        assertEquals(1, registro.quantos("status:Alvo:Paralisado"));
    }

    @Test
    void testElementalNaoAplicaStatusSeOSorteioNaoPermite() {
        Pokesal alvo = pokesal("Alvo", new TipoPlanta());
        new Golpe.Elemental("Brasas", 35).executar(pokesal("CharSal", new TipoFogo()), alvo, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertFalse(alvo.isQueimado());
        assertTrue(registro.eventos.stream().noneMatch(e -> e.startsWith("status:")));
    }

    @Test
    void testElementalNaoRepeteStatusQueOAlvoJaTem() {
        Pokesal queimado = pokesal("Q", new TipoPlanta());
        queimado.setQueimado(true);
        new Golpe.Elemental("Brasas", 35).executar(pokesal("CharSal", new TipoFogo()), queimado, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);

        Pokesal envenenado = pokesal("E", new TipoAgua());
        envenenado.setEnvenenado(true);
        new Golpe.Elemental("Cansanção", 35).executar(pokesal("BulbaSal", new TipoPlanta()), envenenado, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);

        Pokesal paralisado = pokesal("P", new TipoFogo());
        paralisado.setParalisado(true);
        new Golpe.Elemental("Jato", 35).executar(pokesal("SquirtSal", new TipoAgua()), paralisado, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);

        assertEquals(0, registro.quantos("status:Q:Queimado"));
        assertEquals(0, registro.quantos("status:E:Envenenado"));
        assertEquals(0, registro.quantos("status:P:Paralisado"));
    }

    @Test
    void testGolpeQueErraNaoCausaDanoEmNenhumTipoDeGolpe() {
        Golpe[] golpes = { new Golpe.Normal("Tackle", 20), new Golpe.Elemental("Brasas", 35), new Golpe.Debuff("Rajada", 40) };
        for (Golpe golpe : golpes) {
            Pokesal alvo = pokesal("Alvo", new TipoPlanta());
            golpe.executar(pokesal("CharSal", new TipoFogo()), alvo, Terrenos.NEUTRO, ERRA, registro);
            assertEquals(150, alvo.getHp());
        }
        assertEquals(0, registro.eventos.stream().filter(e -> e.startsWith("dano:")).count());
    }

    @Test
    void testCriticoCausaMaisDanoQueOGolpeNormal() {
        Pokesal semCritico = pokesal("A", new TipoPlanta());
        Pokesal comCritico = pokesal("B", new TipoPlanta());
        Golpe tackle = new Golpe.Normal("Tackle", 20);
        Pokesal atacante = pokesal("CharSal", new TipoFogo());

        tackle.executar(atacante, semCritico, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        tackle.executar(atacante, comCritico, Terrenos.NEUTRO, ACERTA_COM_CRITICO, registro);

        assertEquals(150 - 32, semCritico.getHp());
        assertEquals(150 - 54, comCritico.getHp());
    }

    @Test
    void testVantagemEDesvantagemElementalMudamODano() {
        Golpe brasas = new Golpe.Elemental("Brasas", 35);
        Pokesal charSal = pokesal("CharSal", new TipoFogo());
        Pokesal contraPlanta = pokesal("P", new TipoPlanta());
        Pokesal contraAgua = pokesal("A", new TipoAgua());
        Pokesal contraFogo = pokesal("F", new TipoFogo());

        brasas.executar(charSal, contraPlanta, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        brasas.executar(charSal, contraAgua, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        brasas.executar(charSal, contraFogo, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);

        assertEquals(150 - 107, contraPlanta.getHp());
        assertEquals(150 - 17, contraAgua.getHp());
        assertEquals(150 - 47, contraFogo.getHp());
    }

    @Test
    void testAsfaltoQuenteAumentaGolpeDeFogoESoDeFogo() {
        Golpe brasas = new Golpe.Elemental("Brasas", 35);
        Pokesal neutro = pokesal("N", new TipoFogo());
        Pokesal asfalto = pokesal("A", new TipoFogo());
        brasas.executar(pokesal("CharSal", new TipoFogo()), neutro, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        brasas.executar(pokesal("CharSal", new TipoFogo()), asfalto, Terrenos.ASFALTO_QUENTE, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertTrue(asfalto.getHp() < neutro.getHp());

        Pokesal planta = pokesal("P", new TipoAgua());
        Pokesal plantaNoAsfalto = pokesal("PA", new TipoAgua());
        Golpe cansacao = new Golpe.Elemental("Cansanção", 35);
        cansacao.executar(pokesal("BulbaSal", new TipoPlanta()), planta, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        cansacao.executar(pokesal("BulbaSal", new TipoPlanta()), plantaNoAsfalto, Terrenos.ASFALTO_QUENTE, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertEquals(planta.getHp(), plantaNoAsfalto.getHp());
    }

    @Test
    void testPisoEscorregadioAumentaGolpeDeAgua() {
        Golpe jato = new Golpe.Elemental("Jato", 35);
        Pokesal neutro = pokesal("N", new TipoAgua());
        Pokesal piso = pokesal("P", new TipoAgua());
        jato.executar(pokesal("SquirtSal", new TipoAgua()), neutro, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        jato.executar(pokesal("SquirtSal", new TipoAgua()), piso, Terrenos.PISO_ESCORREGADIO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertTrue(piso.getHp() < neutro.getHp());
    }

    @Test
    void testMensagensDeCombateNoConsole() {
        String saida = ConsoleTestes.comConsole(() -> {
            Golpe brasas = new Golpe.Elemental("Brasas", 35);
            Pokesal fogo = pokesal("CharSal", new TipoFogo());
            brasas.executar(fogo, pokesal("Planta", new TipoPlanta()), Terrenos.ASFALTO_QUENTE, ACERTA_E_APLICA_STATUS, registro);
            brasas.executar(fogo, pokesal("Agua", new TipoAgua()), Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
            new Golpe.Elemental("Jato", 35).executar(pokesal("SquirtSal", new TipoAgua()), pokesal("Alvo", new TipoAgua()),
                    Terrenos.PISO_ESCORREGADIO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
            new Golpe.Normal("Tackle", 20).executar(fogo, pokesal("Alvo", new TipoPlanta()), Terrenos.NEUTRO, ACERTA_COM_CRITICO, registro);
            new Golpe.Normal("Tackle", 20).executar(fogo, pokesal("Alvo", new TipoPlanta()), Terrenos.NEUTRO, ERRA, registro);
            new Golpe.Buff("Dança das Chamas").executar(fogo, pokesal("Alvo", new TipoPlanta()), Terrenos.NEUTRO, ERRA, registro);
            new Golpe.Debuff("Rajada", 40).executar(fogo, pokesal("Alvo", new TipoPlanta()), Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        });
        assertTrue(saida.contains("CharSal usou Brasas!"));
        assertTrue(saida.contains("É Super Efetivo!"));
        assertTrue(saida.contains("Não é Muito Efetivo..."));
        assertTrue(saida.contains("[TERRENO] O Asfalto Quente Potencializou o Golpe!"));
        assertTrue(saida.contains("[TERRENO] A Poça de Chuva Amplificou o Ataque!"));
        assertTrue(saida.contains("UM ACERTO CRÍTICO!"));
        assertTrue(saida.contains("O Ataque Falhou!"));
        assertTrue(saida.contains("Efeito Único (Fogo): O Ataque de CharSal subiu"));
        assertTrue(saida.contains("Efeito Único (Fogo): A Defesa de Alvo foi reduzida"));
    }

    private static int danoCausado(Golpe golpe, Pokesal atacante, TipoPoke tipoDoAlvo, String terreno, RegistroEmMemoria registro) {
        Pokesal alvo = pokesal("Alvo", tipoDoAlvo);
        golpe.executar(atacante, alvo, terreno, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        return alvo.getMaxHp() - alvo.getHp();
    }

    @Test
    void testEfeitoTerrenoEstacionamentoUCSal() {
        Golpe brasas = new Golpe.Elemental("Brasas", 35);
        int fogoNeutro = danoCausado(brasas, pokesal("CharSal", new TipoFogo()), new TipoFogo(), Terrenos.NEUTRO, registro);
        int fogoAsfalto = danoCausado(brasas, pokesal("CharSal", new TipoFogo()), new TipoFogo(), Terrenos.ASFALTO_QUENTE, registro);
        assertTrue(fogoAsfalto - fogoNeutro >= 8 && fogoAsfalto - fogoNeutro <= 9);

        Golpe jato = new Golpe.Elemental("Jato", 35);
        int aguaNeutro = danoCausado(jato, pokesal("SquirtSal", new TipoAgua()), new TipoAgua(), Terrenos.NEUTRO, registro);
        int aguaPiso = danoCausado(jato, pokesal("SquirtSal", new TipoAgua()), new TipoAgua(), Terrenos.PISO_ESCORREGADIO, registro);
        assertTrue(aguaPiso - aguaNeutro >= 5 && aguaPiso - aguaNeutro <= 6);

        int foraDoAsfalto = danoCausado(jato, pokesal("SquirtSal", new TipoAgua()), new TipoAgua(), Terrenos.ASFALTO_QUENTE, registro);
        assertEquals(aguaNeutro, foraDoAsfalto);

        Batalha canteiro = new Batalha(c -> Terrenos.CANTEIRO_CENTRAL, Clock.systemUTC(), r -> { }, registro, ACERTA_SEM_CRITICO_NEM_STATUS);
        Pokesal planta = pokesal("BulbaSal", new TipoPlanta());
        planta.dano(100);
        int antes = planta.getHp();
        canteiro.processarFimDeTurno(planta, pokesal("CharSal", new TipoFogo()));
        assertEquals(antes + (int) (planta.getMaxHp() * 0.05), planta.getHp());
    }

    @Test
    void testCalculoDanoBoundaryValues() {
        Golpe tackle = new Golpe.Normal("Tackle", 20);
        Pokesal zeroAtkZeroDef = new Pokesal("A", new TipoFogo(), 0, 0, 150, 50, new Golpe[]{});
        Pokesal alvoSemDefesa = new Pokesal("D", new TipoFogo(), 0, 0, 150, 50, new Golpe[]{});
        tackle.executar(zeroAtkZeroDef, alvoSemDefesa, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertEquals(130, alvoSemDefesa.getHp());

        Pokesal defesaAltissima = new Pokesal("D", new TipoFogo(), 0, 255, 150, 50, new Golpe[]{});
        tackle.executar(zeroAtkZeroDef, defesaAltissima, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertEquals(148, defesaAltissima.getHp());

        Pokesal atkAltissimo = new Pokesal("A", new TipoFogo(), 255, 0, 150, 50, new Golpe[]{});
        Pokesal alvoCheio = new Pokesal("D", new TipoFogo(), 0, 0, 150, 50, new Golpe[]{});
        tackle.executar(atkAltissimo, alvoCheio, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertEquals(150 - (int) (150 * 0.85), alvoCheio.getHp());

        Pokesal hpMinimo = new Pokesal("D", new TipoFogo(), 0, 0, 1, 50, new Golpe[]{});
        tackle.executar(zeroAtkZeroDef, hpMinimo, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertEquals(0, hpMinimo.getHp());

        Pokesal hpExato = new Pokesal("D", new TipoFogo(), 0, 0, 150, 50, new Golpe[]{});
        hpExato.dano(127);
        hpExato.dano(3);
        assertEquals(20, hpExato.getHp());
        tackle.executar(zeroAtkZeroDef, hpExato, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertEquals(0, hpExato.getHp());
        tackle.executar(zeroAtkZeroDef, hpExato, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertEquals(0, hpExato.getHp());
    }
}
