package pokeucsal;

/** Fonte externa de clima. Fica atrás de uma interface para os testes não dependerem de internet. */
public interface ClienteClima {
    ObservacaoClima obterObservacaoAtual() throws Exception;
}
