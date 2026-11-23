package md.bnm.converter.service;

import md.bnm.converter.cache.ResponseCache;
import md.bnm.converter.model.CachedResponse;
import md.bnm.converter.model.RateOrigin;
import md.bnm.converter.model.RateSet;
import md.bnm.converter.source.NbmRateSource;
import md.bnm.converter.source.NbmXmlParser;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaFX-independent rate provider.
 * Tracks dates loaded during the session to avoid duplicate HTTP requests.
 * Searches D-1 through D-10 when a valid response has no currencies.
 * Applies weekend context (stating that the previous Friday's rate applies).
 * Coordinates persistent cache read/write.
 */
public class RateProvider {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final NbmRateSource rateSource;
    private final NbmXmlParser xmlParser;
    private final ResponseCache responseCache;
    private final Map<LocalDate, RateSet> sessionCache;

    public RateProvider() {
        this(new NbmRateSource(), new NbmXmlParser(), new ResponseCache());
    }

    public RateProvider(NbmRateSource rateSource, NbmXmlParser xmlParser, ResponseCache responseCache) {
        this.rateSource = Objects.requireNonNull(rateSource, "rateSource must not be null");
        this.xmlParser = Objects.requireNonNull(xmlParser, "xmlParser must not be null");
        this.responseCache = Objects.requireNonNull(responseCache, "responseCache must not be null");
        this.sessionCache = new ConcurrentHashMap<>();
    }

    public boolean isDateLoadedInSession(LocalDate date) {
        return date != null && sessionCache.containsKey(date);
    }

    public RateSet getLoadedRateSet(LocalDate date) {
        return date != null ? sessionCache.get(date) : null;
    }

    public void clearSessionCache() {
        sessionCache.clear();
    }

    public RateSet getRates(LocalDate requestedDate) throws RateLoadException {
        Objects.requireNonNull(requestedDate, "requestedDate must not be null");

        if (requestedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Выбор будущей даты не допускается: " + requestedDate);
        }

        RateSet cachedInSession = sessionCache.get(requestedDate);
        if (cachedInSession != null) {
            return cachedInSession;
        }

        String rawXml = rateSource.fetchXml(requestedDate);
        NbmXmlParser.ParsedXml parsed = xmlParser.parse(rawXml);

        if (parsed.hasRates()) {
            try {
                responseCache.save(rawXml);
            } catch (IOException e) {
                System.err.println("Предупреждение: Не удалось сохранить кэш: " + e.getMessage());
            }

            RateSet rateSet = buildRateSetForDate(requestedDate, parsed);
            sessionCache.put(requestedDate, rateSet);
            return rateSet;
        }

        // Response is valid XML but contains zero currencies: search D-1 through D-10
        return searchEarlierRates(requestedDate);
    }

    private RateSet buildRateSetForDate(LocalDate requestedDate, NbmXmlParser.ParsedXml parsed) {
        boolean isWeekend = (requestedDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                             requestedDate.getDayOfWeek() == DayOfWeek.SUNDAY);

        LocalDate applicableDate;
        String explanation;

        if (isWeekend) {
            applicableDate = requestedDate.getDayOfWeek() == DayOfWeek.SATURDAY
                ? requestedDate.minusDays(1)
                : requestedDate.minusDays(2);
            explanation = "НБМ не устанавливает курс в выходные дни, применяется курс за предшествующую пятницу.";
        } else {
            applicableDate = requestedDate;
            explanation = null;
        }

        return new RateSet(requestedDate, applicableDate, parsed.rates(), RateOrigin.LIVE, explanation);
    }

    private RateSet searchEarlierRates(LocalDate requestedDate) throws RateLoadException {
        LocalDate fallbackDate = null;
        NbmXmlParser.ParsedXml fallbackParsed = null;
        String fallbackRawXml = null;

        for (int i = 1; i <= 10; i++) {
            LocalDate candidateDate = requestedDate.minusDays(i);
            String candidateXml = rateSource.fetchXml(candidateDate);
            NbmXmlParser.ParsedXml candidateParsed = xmlParser.parse(candidateXml);
            if (candidateParsed.hasRates()) {
                fallbackDate = candidateDate;
                fallbackParsed = candidateParsed;
                fallbackRawXml = candidateXml;
                break;
            }
        }

        if (fallbackParsed == null || fallbackDate == null) {
            throw new RateLoadException(
                RateLoadErrorType.EMPTY_RESPONSE,
                "Официальные курсы валют не найдены за выбранную дату и предшествующие 10 дней."
            );
        }

        try {
            responseCache.save(fallbackRawXml);
        } catch (IOException e) {
            System.err.println("Предупреждение: Не удалось сохранить кэш: " + e.getMessage());
        }

        boolean isWeekend = (requestedDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                             requestedDate.getDayOfWeek() == DayOfWeek.SUNDAY);

        String explanation;
        if (isWeekend) {
            explanation = "НБМ не устанавливает курс в выходные дни, применяется курс за предшествующую пятницу ("
                + fallbackDate.format(DATE_FORMATTER) + ").";
        } else {
            explanation = "Курсы на указанную дату не опубликованы. Применяются официальные курсы за "
                + fallbackDate.format(DATE_FORMATTER) + ".";
        }

        RateSet rateSet = new RateSet(requestedDate, fallbackDate, fallbackParsed.rates(), RateOrigin.EARLIER_DATE_FALLBACK, explanation);
        sessionCache.put(requestedDate, rateSet);
        return rateSet;
    }

    public Optional<CachedResponse> getCachedResponse() {
        return responseCache.load();
    }

    public boolean hasUsableCache() {
        return responseCache.load().isPresent();
    }

    public Optional<LocalDate> getCachedDate() {
        return responseCache.load().map(CachedResponse::date);
    }

    public RateSet loadFromCache(LocalDate requestedDate) throws RateLoadException {
        Objects.requireNonNull(requestedDate, "requestedDate must not be null");

        CachedResponse cached = responseCache.load().orElseThrow(
            () -> new RateLoadException(RateLoadErrorType.CACHE_UNAVAILABLE, "Сохранённые курсы валют отсутствуют в локальном кэше.")
        );

        NbmXmlParser.ParsedXml parsed = xmlParser.parse(cached.rawXml());
        if (!parsed.hasRates()) {
            throw new RateLoadException(RateLoadErrorType.CACHE_UNAVAILABLE, "Локальный кэш не содержит курсов валют.");
        }

        LocalDate rootDate = parsed.date();
        boolean isWeekend = (rootDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                             rootDate.getDayOfWeek() == DayOfWeek.SUNDAY);

        LocalDate applicableDate;
        String explanation;

        if (isWeekend) {
            applicableDate = rootDate.getDayOfWeek() == DayOfWeek.SATURDAY
                ? rootDate.minusDays(1)
                : rootDate.minusDays(2);
            explanation = "НБМ не устанавливает курс в выходные дни, применяется курс за предшествующую пятницу. "
                + "Использованы курсы из локального кэша за " + applicableDate.format(DATE_FORMATTER) + ".";
        } else {
            applicableDate = rootDate;
            explanation = "Использованы курсы из локального кэша за " + applicableDate.format(DATE_FORMATTER) + ".";
        }

        return new RateSet(requestedDate, applicableDate, parsed.rates(), RateOrigin.LOCAL_CACHE, explanation);
    }

    public ResponseCache getResponseCache() {
        return responseCache;
    }

    public NbmRateSource getRateSource() {
        return rateSource;
    }

    public NbmXmlParser getXmlParser() {
        return xmlParser;
    }
}
