package pokeucsal;

import java.util.Random;

/**
 * Implementação "de verdade" do GeradorAleatorio (nos testes ela é substituída por um mock).
 * É o único lugar que usa java.util.Random: a regra de negócio só enxerga a interface.
 */
public class GeradorAleatorioPadrao implements GeradorAleatorio {
    private final Random random;

    public GeradorAleatorioPadrao() { this(new Random()); }
    public GeradorAleatorioPadrao(Random random) { this.random = random; }

    /** 0 a 99. O golpe erra quando o valor sorteado é >= à precisão do atacante (100 nunca erra). */
    @Override public int sortearPrecisao() { return random.nextInt(100); }

    /** 0.0 (inclusive) a 1.0 (exclusive). */
    @Override public double sortearChance() { return random.nextDouble(); }
}
