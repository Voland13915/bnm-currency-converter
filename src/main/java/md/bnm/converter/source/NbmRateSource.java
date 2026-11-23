package md.bnm.converter.source;

import md.bnm.converter.service.RateLoadErrorType;
import md.bnm.converter.service.RateLoadException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * HTTP client for fetching official NBM exchange rate XML responses.
 * Uses JDK HttpClient with a 10-second timeout, handles HTTP status codes,
 * and maps network/timeout/server failures to RateLoadException.
 */
public class NbmRateSource {

    public static final String DEFAULT_BASE_URL = "https://www.bnm.md/en/official_exchange_rates?get_xml=1";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Duration timeout;

    public NbmRateSource() {
        this(
            HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(),
            DEFAULT_BASE_URL,
            DEFAULT_TIMEOUT
        );
    }

    public NbmRateSource(HttpClient httpClient, String baseUrl, Duration timeout) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    }

    public String fetchXml(LocalDate date) throws RateLoadException {
        Objects.requireNonNull(date, "date must not be null");

        String delimiter = baseUrl.contains("?") ? "&" : "?";
        String url = baseUrl + delimiter + "date=" + date.format(DATE_FORMATTER);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout)
            .header("Accept", "application/xml, text/xml")
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new RateLoadException(
                    RateLoadErrorType.SERVER_ERROR,
                    "Сервер НБМ вернул ошибку HTTP " + status + " при запросе курсов валют."
                );
            }
            return response.body();
        } catch (HttpTimeoutException e) {
            throw new RateLoadException(
                RateLoadErrorType.TIMEOUT,
                "Превышено время ожидания ответа от сервера НБМ (10 секунд).",
                e
            );
        } catch (ConnectException | UnknownHostException e) {
            throw new RateLoadException(
                RateLoadErrorType.NETWORK_ERROR,
                "Не удалось подключиться к серверу НБМ. Проверьте сетевое подключение.",
                e
            );
        } catch (IOException e) {
            throw new RateLoadException(
                RateLoadErrorType.NETWORK_ERROR,
                "Ошибка сети при получении данных от сервера НБМ: " + e.getMessage(),
                e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RateLoadException(
                RateLoadErrorType.NETWORK_ERROR,
                "Запрос к серверу НБМ был прерван.",
                e
            );
        }
    }
}
