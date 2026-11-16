package pokeucsal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteClimaOpenMeteoTest {
    private static final URI URI_TESTE = URI.create("https://exemplo.test/clima");
    private static final String RESPOSTA = "{\"latitude\":-12.9,\"longitude\":-38.5,"
            + "\"current_units\":{\"time\":\"iso8601\",\"interval\":\"seconds\",\"weather_code\":\"wmo code\",\"cloud_cover\":\"%\",\"precipitation\":\"mm\"},"
            + "\"current\":{\"time\":\"2026-11-13T10:00\",\"interval\":900,\"weather_code\":3,\"cloud_cover\":85,\"precipitation\":0.2}}";

    @Mock HttpClient http;
    @Mock HttpResponse<String> resposta;

    private ClienteClimaOpenMeteo cliente;

    @BeforeEach
    void setUp() {
        cliente = new ClienteClimaOpenMeteo(http, URI_TESTE);
    }

    @Test
    void testObtemObservacaoQuandoOServicoResponde200() throws Exception {
        when(resposta.statusCode()).thenReturn(200);
        when(resposta.body()).thenReturn(RESPOSTA);
        doReturn(resposta).when(http).send(any(HttpRequest.class), any());

        ObservacaoClima o = cliente.obterObservacaoAtual();

        assertEquals(new ObservacaoClima(3, 85, 0.2), o);
        ArgumentCaptor<HttpRequest> pedido = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(pedido.capture(), any());
        assertEquals(URI_TESTE, pedido.getValue().uri());
        assertEquals("GET", pedido.getValue().method());
    }

    @Test
    void testStatusDiferenteDe200LancaIOException() throws Exception {
        when(resposta.statusCode()).thenReturn(500);
        doReturn(resposta).when(http).send(any(HttpRequest.class), any());

        IOException erro = assertThrows(IOException.class, () -> cliente.obterObservacaoAtual());
        assertTrue(erro.getMessage().contains("500"));
        verify(resposta, never()).body();
    }

    @Test
    void testFalhaDeRedeSobeParaQuemChamou() throws Exception {
        doThrow(new IOException("timeout")).when(http).send(any(HttpRequest.class), any());
        assertThrows(IOException.class, () -> cliente.obterObservacaoAtual());
    }

    @Test
    void testUrlPadraoConsultaSalvador() {
        assertTrue(ClienteClimaOpenMeteo.URL_SALVADOR.contains("latitude=-12.9714"));
        assertTrue(ClienteClimaOpenMeteo.URL_SALVADOR.contains("longitude=-38.5014"));
        assertTrue(ClienteClimaOpenMeteo.URL_SALVADOR.contains("weather_code,cloud_cover,precipitation"));
    }

    @Test
    void testInterpretaOBlocoCurrentSemConfundirComCurrentUnits() throws IOException {
        ObservacaoClima o = ClienteClimaOpenMeteo.interpretar(RESPOSTA);
        assertEquals(3, o.codigoWmo());
        assertEquals(85, o.coberturaNuvens());
        assertEquals(0.2, o.precipitacaoMm(), 1e-9);
    }

    @Test
    void testRespostaInvalidaLancaIOException() {
        assertThrows(IOException.class, () -> ClienteClimaOpenMeteo.interpretar(null));
        assertThrows(IOException.class, () -> ClienteClimaOpenMeteo.interpretar("{}"));
        assertThrows(IOException.class, () -> ClienteClimaOpenMeteo.interpretar("{\"current\":{\"weather_code\":3"));
        assertThrows(IOException.class, () -> ClienteClimaOpenMeteo.interpretar("{\"current\":{\"weather_code\":3}}"));
    }
}
