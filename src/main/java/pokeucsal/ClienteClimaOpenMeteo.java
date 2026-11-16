package pokeucsal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consulta o clima ATUAL de Salvador na API gratuita Open-Meteo (https://open-meteo.com), sem chave de API.
 *
 * Por quê sem biblioteca de JSON: só precisamos de 3 números do bloco "current"; assim o pom.xml não muda.
 */
public class ClienteClimaOpenMeteo implements ClienteClima {
    // Salvador-BA: latitude -12.9714, longitude -38.5014.
    static final String URL_SALVADOR = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=-12.9714&longitude=-38.5014"
            + "&current=weather_code,cloud_cover,precipitation"
            + "&timezone=America%2FBahia";

    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(4);

    private final HttpClient http;
    private final URI uri;

    public ClienteClimaOpenMeteo() {
        this(HttpClient.newBuilder().connectTimeout(TEMPO_LIMITE).build(), URI.create(URL_SALVADOR));
    }

    public ClienteClimaOpenMeteo(HttpClient http, URI uri) {
        this.http = http;
        this.uri = uri;
    }

    @Override
    public ObservacaoClima obterObservacaoAtual() throws IOException, InterruptedException {
        HttpRequest pedido = HttpRequest.newBuilder(uri).timeout(TEMPO_LIMITE).GET().build();
        HttpResponse<String> resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() != 200) {
            throw new IOException("Open-Meteo respondeu HTTP " + resposta.statusCode());
        }
        return interpretar(resposta.body());
    }

    /**
     * Lê o bloco "current" da resposta, que tem esta forma:
     * {"current_units":{...}, "current":{"time":"...","weather_code":3,"cloud_cover":85,"precipitation":0.0}}
     * Procura por "current": (com aspas e dois-pontos) para não confundir com "current_units".
     */
    static ObservacaoClima interpretar(String json) throws IOException {
        if (json == null) throw new IOException("Resposta vazia do serviço de clima.");
        int inicio = json.indexOf("\"current\":");
        if (inicio < 0) throw new IOException("Resposta sem o bloco \"current\".");
        int fim = json.indexOf('}', inicio);
        if (fim < 0) throw new IOException("Bloco \"current\" incompleto.");
        String bloco = json.substring(inicio, fim);

        return new ObservacaoClima(
                (int) Math.round(numero(bloco, "weather_code")),
                (int) Math.round(numero(bloco, "cloud_cover")),
                numero(bloco, "precipitation"));
    }

    private static double numero(String bloco, String campo) throws IOException {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(bloco);
        if (!m.find()) throw new IOException("Campo \"" + campo + "\" ausente na resposta do clima.");
        return Double.parseDouble(m.group(1));
    }
}
