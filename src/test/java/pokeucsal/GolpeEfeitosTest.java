package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Efeitos dos golpes que os testes com Mockito (GolpeTest) não alcançam: status de cada tipo, erro de precisão,
 * crítico, multiplicadores e bônus de terreno, mais as mensagens de console. Aqui o gerador é um fake de valores fixos.
 */
class GolpeEfeitosTest {
    private static final GeradorFixo ACERTA_SEM_CRITICO_NEM_STATUS = new GeradorFixo(0, 0.99);
    private static final GeradorFixo ACERTA_E_APLICA_STATUS = new GeradorFixo(0, 0.1);      // 0.1 > 6,25% (sem crítico) e < 12,5% (status)
    private static final GeradorFixo ACERTA_COM_CRITICO = new GeradorFixo(0, 0.01);
    private static final GeradorFixo ERRA = new GeradorFixo(100, 0.99);

    private RegistroEmMemoria registro;

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
        registro = new RegistroEmMemoria();
    }

    @AfterEach
    void tearDown() { Batalha.modoTeste = false; }

    private static Pokemon pokemon(String nome, TipoPoke tipo) {
        return new Pokemon(nome, tipo, 50, 40, 150, 50, new Golpe[]{});
    }

    // ------------------------------------------------------------------ status

    @Test
    void testElementalDeFogoQueimaOAlvo() {
        Pokemon alvo = pokemon("Alvo", new TipoPlanta());
        new Golpe.Elemental("Brasas", 35).executar(pokemon("CharSal", new TipoFogo()), alvo, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);
        assertTrue(alvo.isQueimado());
        assertEquals(1, registro.quantos("status:Alvo:Queimado"));
    }

    @Test
    void testElementalDePlantaEnvenenaOAlvo() {
        Pokemon alvo = pokemon("Alvo", new TipoAgua());
        new Golpe.Elemental("Cansanção", 35).executar(pokemon("BulbaSal", new TipoPlanta()), alvo, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);
        assertTrue(alvo.isEnvenenado());
        assertEquals(1, registro.quantos("status:Alvo:Envenenado"));
    }

    @Test
    void testElementalDeAguaParalisaOAlvo() {
        Pokemon alvo = pokemon("Alvo", new TipoFogo());
        new Golpe.Elemental("Jato", 35).executar(pokemon("SquirtSal", new TipoAgua()), alvo, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);
        assertTrue(alvo.isParalisado());
        assertEquals(1, registro.quantos("status:Alvo:Paralisado"));
    }

    @Test
    void testElementalNaoAplicaStatusSeOSorteioNaoPermite() {
        Pokemon alvo = pokemon("Alvo", new TipoPlanta());
        new Golpe.Elemental("Brasas", 35).executar(pokemon("CharSal", new TipoFogo()), alvo, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertFalse(alvo.isQueimado());
        assertTrue(registro.eventos.stream().noneMatch(e -> e.startsWith("status:")));
    }

    @Test
    void testElementalNaoRepeteStatusQueOAlvoJaTem() {
        Pokemon queimado = pokemon("Q", new TipoPlanta());
        queimado.setQueimado(true);
        new Golpe.Elemental("Brasas", 35).executar(pokemon("CharSal", new TipoFogo()), queimado, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);

        Pokemon envenenado = pokemon("E", new TipoAgua());
        envenenado.setEnvenenado(true);
        new Golpe.Elemental("Cansanção", 35).executar(pokemon("BulbaSal", new TipoPlanta()), envenenado, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);

        Pokemon paralisado = pokemon("P", new TipoFogo());
        paralisado.setParalisado(true);
        new Golpe.Elemental("Jato", 35).executar(pokemon("SquirtSal", new TipoAgua()), paralisado, Terrenos.NEUTRO, ACERTA_E_APLICA_STATUS, registro);

        assertEquals(0, registro.quantos("status:Q:Queimado"));
        assertEquals(0, registro.quantos("status:E:Envenenado"));
        assertEquals(0, registro.quantos("status:P:Paralisado"));
    }

    // ---------------------------------------------------------- precisão e crítico

    @Test
    void testGolpeQueErraNaoCausaDanoEmNenhumTipoDeGolpe() {
        Golpe[] golpes = { new Golpe.Normal("Tackle", 20), new Golpe.Elemental("Brasas", 35), new Golpe.Debuff("Rajada", 40) };
        for (Golpe golpe : golpes) {
            Pokemon alvo = pokemon("Alvo", new TipoPlanta());
            golpe.executar(pokemon("CharSal", new TipoFogo()), alvo, Terrenos.NEUTRO, ERRA, registro);
            assertEquals(150, alvo.getHp());
        }
        assertEquals(0, registro.eventos.stream().filter(e -> e.startsWith("dano:")).count());
    }

    @Test
    void testCriticoCausaMaisDanoQueOGolpeNormal() {
        Pokemon semCritico = pokemon("A", new TipoPlanta());
        Pokemon comCritico = pokemon("B", new TipoPlanta());
        Golpe tackle = new Golpe.Normal("Tackle", 20);
        Pokemon atacante = pokemon("CharSal", new TipoFogo());

        tackle.executar(atacante, semCritico, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        tackle.executar(atacante, comCritico, Terrenos.NEUTRO, ACERTA_COM_CRITICO, registro);

        assertEquals(150 - 32, semCritico.getHp());   // 20 + 50/2 = 45 de dano bruto, menos 13 de mitigação
        assertEquals(150 - 54, comCritico.getHp());   // (int) (45 x 1.5) = 67, menos 13
    }

    // ------------------------------------------------------------ multiplicadores

    @Test
    void testVantagemEDesvantagemElementalMudamODano() {
        Golpe brasas = new Golpe.Elemental("Brasas", 35);
        Pokemon charSal = pokemon("CharSal", new TipoFogo());
        Pokemon contraPlanta = pokemon("P", new TipoPlanta());
        Pokemon contraAgua = pokemon("A", new TipoAgua());
        Pokemon contraFogo = pokemon("F", new TipoFogo());

        brasas.executar(charSal, contraPlanta, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        brasas.executar(charSal, contraAgua, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        brasas.executar(charSal, contraFogo, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);

        // Dano bruto base: 35 + 50/2 = 60. Menos 13 de mitigação: x2.0 = 107, x0.5 = 17, x1.0 = 47.
        assertEquals(150 - 107, contraPlanta.getHp());
        assertEquals(150 - 17, contraAgua.getHp());
        assertEquals(150 - 47, contraFogo.getHp());
    }

    @Test
    void testAsfaltoQuenteAumentaGolpeDeFogoESoDeFogo() {
        Golpe brasas = new Golpe.Elemental("Brasas", 35);
        Pokemon neutro = pokemon("N", new TipoFogo());
        Pokemon asfalto = pokemon("A", new TipoFogo());
        brasas.executar(pokemon("CharSal", new TipoFogo()), neutro, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        brasas.executar(pokemon("CharSal", new TipoFogo()), asfalto, Terrenos.ASFALTO_QUENTE, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertTrue(asfalto.getHp() < neutro.getHp());

        Pokemon planta = pokemon("P", new TipoAgua());
        Pokemon plantaNoAsfalto = pokemon("PA", new TipoAgua());
        Golpe cansacao = new Golpe.Elemental("Cansanção", 35);
        cansacao.executar(pokemon("BulbaSal", new TipoPlanta()), planta, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        cansacao.executar(pokemon("BulbaSal", new TipoPlanta()), plantaNoAsfalto, Terrenos.ASFALTO_QUENTE, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertEquals(planta.getHp(), plantaNoAsfalto.getHp());   // o bônus não vale para outros tipos
    }

    @Test
    void testPisoEscorregadioAumentaGolpeDeAgua() {
        Golpe jato = new Golpe.Elemental("Jato", 35);
        Pokemon neutro = pokemon("N", new TipoAgua());
        Pokemon piso = pokemon("P", new TipoAgua());
        jato.executar(pokemon("SquirtSal", new TipoAgua()), neutro, Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        jato.executar(pokemon("SquirtSal", new TipoAgua()), piso, Terrenos.PISO_ESCORREGADIO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
        assertTrue(piso.getHp() < neutro.getHp());
    }

    // -------------------------------------------------------------------- console

    @Test
    void testMensagensDeCombateNoConsole() {
        String saida = ConsoleTestes.comConsole(() -> {
            Golpe brasas = new Golpe.Elemental("Brasas", 35);
            Pokemon fogo = pokemon("CharSal", new TipoFogo());
            brasas.executar(fogo, pokemon("Planta", new TipoPlanta()), Terrenos.ASFALTO_QUENTE, ACERTA_E_APLICA_STATUS, registro);
            brasas.executar(fogo, pokemon("Agua", new TipoAgua()), Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
            new Golpe.Elemental("Jato", 35).executar(pokemon("SquirtSal", new TipoAgua()), pokemon("Alvo", new TipoAgua()),
                    Terrenos.PISO_ESCORREGADIO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
            new Golpe.Normal("Tackle", 20).executar(fogo, pokemon("Alvo", new TipoPlanta()), Terrenos.NEUTRO, ACERTA_COM_CRITICO, registro);
            new Golpe.Normal("Tackle", 20).executar(fogo, pokemon("Alvo", new TipoPlanta()), Terrenos.NEUTRO, ERRA, registro);
            new Golpe.Buff("Dança das Chamas").executar(fogo, pokemon("Alvo", new TipoPlanta()), Terrenos.NEUTRO, ERRA, registro);
            new Golpe.Debuff("Rajada", 40).executar(fogo, pokemon("Alvo", new TipoPlanta()), Terrenos.NEUTRO, ACERTA_SEM_CRITICO_NEM_STATUS, registro);
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
}
