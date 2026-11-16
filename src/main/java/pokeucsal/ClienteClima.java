package pokeucsal;

/** Fonte externa de clima, atrás de uma interface para os testes não dependerem de internet. */
public interface ClienteClima {
    /**
     * Consulta o clima atual.
     *
     * @return a observação do clima agora
     * @throws Exception se o serviço externo falhar
     */
    ObservacaoClima obterObservacaoAtual() throws Exception;
}
