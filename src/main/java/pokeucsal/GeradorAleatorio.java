package pokeucsal;

/**
 * Toda a aleatoriedade do jogo passa por esta interface, para os testes escolherem os valores.
 */
public interface GeradorAleatorio {
    /**
     * Sorteia o valor do teste de precisão.
     *
     * @return um inteiro de 0 a 99; o golpe erra se for maior ou igual à precisão do atacante
     */
    int sortearPrecisao();
    /**
     * Sorteia uma chance.
     *
     * @return um número de 0 (inclusive) até 1 (exclusive)
     */
    double sortearChance();
}
