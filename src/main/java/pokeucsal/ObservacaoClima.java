package pokeucsal;

/**
 * Retrato do tempo em um instante, já em formato simples.
 *
 * @param codigoWmo     código WMO de "tempo presente" (0 = céu limpo, 3 = encoberto, 61 = chuva, 95 = tempestade...)
 * @param coberturaNuvens porcentagem do céu coberto por nuvens (0 a 100)
 * @param precipitacaoMm  chuva/precipitação do último intervalo, em milímetros
 */
public record ObservacaoClima(int codigoWmo, int coberturaNuvens, double precipitacaoMm) { }
