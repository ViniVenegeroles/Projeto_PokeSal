package pokeucsal;

/** Lançada quando o treinador tenta usar mais itens do que o limite da batalha. */
public class LimiteDeItensExcedidoException extends RuntimeException {
    /**
     * Cria a exceção com a mensagem do limite.
     *
     * @param limite máximo de itens por batalha
     */
    public LimiteDeItensExcedidoException(int limite) {
        super("Limite Máximo De " + limite + " itens Atingido Nesta Batalha.");
    }
}
