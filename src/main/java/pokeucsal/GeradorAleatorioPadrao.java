package pokeucsal;

import java.util.Random;

/** Implementação real do gerador, baseada em {@link java.util.Random}. */
public class GeradorAleatorioPadrao implements GeradorAleatorio {
    private final Random random;

    /** Cria o gerador com semente aleatória. */
    public GeradorAleatorioPadrao() {
        this(new Random());
    }
    /**
     * Cria o gerador com um {@link java.util.Random} escolhido (semente fixa em testes).
     *
     * @param random fonte de números aleatórios
     */
    public GeradorAleatorioPadrao(Random random) {
        this.random = random;
    }

    @Override public int sortearPrecisao() {
        return random.nextInt(100);
    }

    @Override public double sortearChance() {
        return random.nextDouble();
    }
}
