package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GolpeTest {
    @Mock GeradorAleatorio gerador;
    @Mock RegistroBatalha registro;

    private Pokesal fogo;
    private Pokesal planta;
    private Pokesal agua;

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
        Golpe[] nenhum = {};
        fogo = new Pokesal("CharSal", new TipoFogo(), 50, 40, 150, 90, nenhum);
        planta = new Pokesal("BulbaSal", new TipoPlanta(), 50, 40, 150, 80, nenhum);
        agua = new Pokesal("SquirtSal", new TipoAgua(), 50, 40, 150, 70, nenhum);
    }

    @AfterEach
    void tearDown() {
        Batalha.modoTeste = false;
    }

    @Test
    void testBuffAumentaAtributosCobraPrecisaoENaoCausaDano() {
        new Golpe.Buff("Dança das Chamas").executar(fogo, planta, Terrenos.NEUTRO, gerador, registro);

        assertEquals(60, fogo.getAtk());
        assertEquals(35, fogo.getDef());
        assertEquals(90, fogo.getPrecisao());
        assertEquals(150, planta.getHp());
        verify(registro, never()).registrarDano(anyString(), anyInt());
        verify(registro).registrarEfeitoStatus("CharSal", "Buff de Tipo");
        verifyNoMoreInteractions(gerador);
    }

    @Test
    void testPrecisaoDoBuffNaoCaiAbaixoDoMinimo() {
        Golpe buff = new Golpe.Buff("Dança das Chamas");
        for (int i = 0; i < 10; i++) buff.executar(fogo, planta, Terrenos.NEUTRO, gerador, registro);
        assertEquals(Pokesal.PRECISAO_MINIMA, fogo.getPrecisao());
    }

    @Test
    void testDebuffCausaDanoEReduzDefesaDoAlvo() {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.99);

        new Golpe.Debuff("Superaquecer", 20).executar(fogo, agua, Terrenos.NEUTRO, gerador, registro);

        assertEquals(141, agua.getHp());
        assertEquals(35, agua.getDef());
        InOrder ordem = inOrder(registro);
        ordem.verify(registro).registrarDano("SquirtSal", 9);
        ordem.verify(registro).registrarEfeitoStatus("SquirtSal", "Debuff de Tipo");
    }

    @Test
    void testDebuffNaoAplicaEfeitoEmAlvoQueDesmaiou() {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.99);
        planta.dano(1000);

        new Golpe.Debuff("Superaquecer", 40).executar(fogo, planta, Terrenos.NEUTRO, gerador, registro);

        assertEquals(0, planta.getHp());
        assertEquals(40, planta.getDef());
        verify(registro, never()).registrarEfeitoStatus(anyString(), eq("Debuff de Tipo"));
    }

    @Test
    void testDebuffQueErraNaoCausaDanoNemEfeito() {
        when(gerador.sortearPrecisao()).thenReturn(100);

        new Golpe.Debuff("Superaquecer", 40).executar(fogo, planta, Terrenos.NEUTRO, gerador, registro);

        assertEquals(150, planta.getHp());
        assertEquals(40, planta.getDef());
        verify(registro).registrarAtaque("CharSal", "Superaquecer");
        verify(registro, never()).registrarDano(anyString(), anyInt());
    }

    @Test
    void testCriticoERegistradoDepoisDoDano() {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.0);

        new Golpe.Normal("Tackle", 20).executar(fogo, planta, Terrenos.NEUTRO, gerador, registro);

        InOrder ordem = inOrder(registro);
        ordem.verify(registro).registrarAtaque("CharSal", "Tackle");
        ordem.verify(registro).registrarDano(eq("BulbaSal"), anyInt());
        ordem.verify(registro).registrarEfeitoStatus("CharSal", "Acerto Crítico!");
    }

    @Test
    void testAsfaltoQuenteAmplificaGolpeElementalDeFogo() {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.99);
        Pokesal alvoNeutro = new Pokesal("Alvo1", new TipoPlanta(), 50, 40, 150, 10, new Golpe[]{});
        Pokesal alvoAsfalto = new Pokesal("Alvo2", new TipoPlanta(), 50, 40, 150, 10, new Golpe[]{});
        Golpe lancaChamas = new Golpe.Elemental("Lança-Chamas", 35);

        lancaChamas.executar(fogo, alvoNeutro, Terrenos.NEUTRO, gerador, registro);
        lancaChamas.executar(fogo, alvoAsfalto, Terrenos.ASFALTO_QUENTE, gerador, registro);

        assertTrue(alvoAsfalto.getHp() < alvoNeutro.getHp(), "o bônus do Asfalto Quente deveria causar mais dano");
    }

    @Test
    void testElementalAplicaQueimaduraEmFogoQuandoSorteioPermite() {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.5, 0.01);

        new Golpe.Elemental("Lança-Chamas", 35).executar(fogo, planta, Terrenos.NEUTRO, gerador, registro);

        assertTrue(planta.isQueimado());
        verify(registro).registrarEfeitoStatus("BulbaSal", "Queimado");
    }

    @Test
    void testElementalNaoSorteiaStatusEmAlvoQueDesmaiou() {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.5);
        planta.dano(1000);

        new Golpe.Elemental("Lança-Chamas", 35).executar(fogo, planta, Terrenos.NEUTRO, gerador, registro);

        assertEquals(0, planta.getHp());
        verify(gerador, times(1)).sortearChance();
    }
}
