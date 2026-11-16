package pokeucsal;

/**
 * Retrato do tempo em um instante.
 *
 * @param codigoWmo código WMO do tempo presente (0 céu limpo, 3 encoberto, 61 chuva, 95 tempestade)
 * @param coberturaNuvens porcentagem do céu coberto por nuvens, de 0 a 100
 * @param precipitacaoMm precipitação do último intervalo, em milímetros
 */
public record ObservacaoClima(int codigoWmo, int coberturaNuvens, double precipitacaoMm) { }
