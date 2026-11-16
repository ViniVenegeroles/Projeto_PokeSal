package pokeucsal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Consulta o clima atual de Salvador na API gratuita Open-Meteo, sem chave de acesso. */
public class ClienteClimaOpenMeteo implements ClienteClima {
    static final String URL_SALVADOR = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=-12.9714&longitude=-38.5014"
            + "&current=weather_code,cloud_cover,precipitation"
            + "&timezone=America%2FBahia";

    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(4);

    private final HttpClient http;
    private final URI uri;

    /** Cria o cliente apontando para Salvador, com tempo limite de 4 segundos. */
    public ClienteClimaOpenMeteo() {
        this(HttpClient.newBuilder().connectTimeout(TEMPO_LIMITE).build(), URI.create(URL_SALVADOR));
    }

    /**
     * Cria o cliente com um cliente HTTP e um endereço escolhidos (usado nos testes).
     *
     * @param http cliente HTTP
     * @param uri endereço da consulta
     */
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

    static ObservacaoClima interpretar(String json) throws IOException {
        if (json == null) {
            throw new IOException("Resposta vazia do serviço de clima.");
        }
        int inicio = json.indexOf("\"current\":");
        if (inicio < 0) {
            throw new IOException("Resposta sem o bloco \"current\".");
        }
        int fim = json.indexOf('}', inicio);
        if (fim < 0) {
            throw new IOException("Bloco \"current\" incompleto.");
        }
        String bloco = json.substring(inicio, fim);

        return new ObservacaoClima(
                (int) Math.round(numero(bloco, "weather_code")),
                (int) Math.round(numero(bloco, "cloud_cover")),
                numero(bloco, "precipitation"));
    }

    private static double numero(String bloco, String campo) throws IOException {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(bloco);
        if (!m.find()) {
            throw new IOException("Campo \"" + campo + "\" ausente na resposta do clima.");
        }
        return Double.parseDouble(m.group(1));
    }
}
