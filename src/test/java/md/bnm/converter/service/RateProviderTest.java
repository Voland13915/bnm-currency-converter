package md.bnm.converter.service;

import md.bnm.converter.TestFixtures;
import md.bnm.converter.cache.ResponseCache;
import md.bnm.converter.model.RateOrigin;
import md.bnm.converter.model.RateSet;
import md.bnm.converter.source.NbmXmlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RateProviderTest {

    private TestFixtures.FakeNbmRateSource fakeSource;
    private NbmXmlParser parser;
    private ResponseCache cache;
    private RateProvider provider;

    private String weekdayXml;
    private String weekendXml;
    private String emptyXml;
    private String malformedXml;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        weekdayXml = TestFixtures.loadFixture(TestFixtures.WEEKDAY_FIXTURE);
        weekendXml = TestFixtures.loadFixture(TestFixtures.WEEKEND_FIXTURE);
        emptyXml = TestFixtures.loadFixture(TestFixtures.EMPTY_FIXTURE);
        malformedXml = TestFixtures.loadFixture(TestFixtures.MALFORMED_FIXTURE);

        fakeSource = new TestFixtures.FakeNbmRateSource();
        parser = new NbmXmlParser();
        cache = new ResponseCache(tempDir.resolve("test-rates.xml"));
        provider = new RateProvider(fakeSource, parser, cache);
    }

    @Test
    @DisplayName("T033: Weekday rate request sets applicableDate to requestedDate with null explanation")
    void testWeekdayRateSet() throws Exception {
        LocalDate friday = LocalDate.of(2026, 11, 20);
        fakeSource.setResponse(friday, weekdayXml);

        RateSet rateSet = provider.getRates(friday);

        assertNotNull(rateSet);
        assertEquals(friday, rateSet.requestedDate());
        assertEquals(friday, rateSet.applicableDate());
        assertEquals(RateOrigin.LIVE, rateSet.origin());
        assertNull(rateSet.explanation());
        assertTrue(rateSet.hasCurrency("EUR"));
        assertTrue(rateSet.hasCurrency("MDL"));
    }

    @Test
    @DisplayName("T033: Saturday request applies previous Friday's date and includes weekend explanation")
    void testSaturdayWeekendRule() throws Exception {
        LocalDate saturday = LocalDate.of(2026, 11, 21);
        LocalDate friday = LocalDate.of(2026, 11, 20);

        fakeSource.setResponse(saturday, weekdayXml);

        RateSet rateSet = provider.getRates(saturday);

        assertEquals(saturday, rateSet.requestedDate());
        assertEquals(friday, rateSet.applicableDate());
        assertEquals(RateOrigin.LIVE, rateSet.origin());
        assertNotNull(rateSet.explanation());
        assertTrue(rateSet.explanation().contains("НБМ не устанавливает курс в выходные дни"));
    }

    @Test
    @DisplayName("T033: Sunday request applies previous Friday's date and includes weekend explanation")
    void testSundayWeekendRule() throws Exception {
        LocalDate sunday = LocalDate.of(2026, 11, 22);
        LocalDate friday = LocalDate.of(2026, 11, 20);

        fakeSource.setResponse(sunday, weekendXml);

        RateSet rateSet = provider.getRates(sunday);

        assertEquals(sunday, rateSet.requestedDate());
        assertEquals(friday, rateSet.applicableDate());
        assertEquals(RateOrigin.LIVE, rateSet.origin());
        assertNotNull(rateSet.explanation());
        assertTrue(rateSet.explanation().contains("НБМ не устанавливает курс в выходные дни"));
    }

    @Test
    @DisplayName("T033: Empty response triggers fallback search D-1 through D-10 and uses first earlier non-empty date")
    void testEmptyResponseFallbackSearch() throws Exception {
        LocalDate requested = LocalDate.of(2026, 11, 20);
        LocalDate dMinus1 = LocalDate.of(2026, 11, 19);
        LocalDate dMinus2 = LocalDate.of(2026, 11, 18);

        // Requested date is empty
        fakeSource.setResponse(requested, emptyXml);
        // D-1 is also empty
        fakeSource.setResponse(dMinus1, emptyXml);
        // D-2 has valid rates
        fakeSource.setResponse(dMinus2, weekdayXml);

        RateSet rateSet = provider.getRates(requested);

        assertEquals(requested, rateSet.requestedDate());
        assertEquals(dMinus2, rateSet.applicableDate());
        assertEquals(RateOrigin.EARLIER_DATE_FALLBACK, rateSet.origin());
        assertNotNull(rateSet.explanation());
        assertTrue(rateSet.explanation().contains("18.11.2026"));
    }

    @Test
    @DisplayName("T037: Fallback search stops immediately and rethrows NETWORK_ERROR on earlier date")
    void testFallbackSearchStopsOnNetworkError() {
        LocalDate requested = LocalDate.of(2026, 11, 20);
        LocalDate dMinus1 = LocalDate.of(2026, 11, 19);
        LocalDate dMinus2 = LocalDate.of(2026, 11, 18);

        fakeSource.setResponse(requested, emptyXml);
        fakeSource.setError(dMinus1, new RateLoadException(RateLoadErrorType.NETWORK_ERROR));
        fakeSource.setResponse(dMinus2, weekdayXml);

        RateLoadException ex = assertThrows(RateLoadException.class, () -> provider.getRates(requested));
        assertEquals(RateLoadErrorType.NETWORK_ERROR, ex.getErrorType());
        // Verify search stopped at D-1 and did not request D-2
        assertEquals(0, fakeSource.getRequestCount(dMinus2));
    }

    @Test
    @DisplayName("T037: Fallback search stops immediately and rethrows TIMEOUT on earlier date")
    void testFallbackSearchStopsOnTimeout() {
        LocalDate requested = LocalDate.of(2026, 11, 20);
        LocalDate dMinus1 = LocalDate.of(2026, 11, 19);

        fakeSource.setResponse(requested, emptyXml);
        fakeSource.setError(dMinus1, new RateLoadException(RateLoadErrorType.TIMEOUT));

        RateLoadException ex = assertThrows(RateLoadException.class, () -> provider.getRates(requested));
        assertEquals(RateLoadErrorType.TIMEOUT, ex.getErrorType());
    }

    @Test
    @DisplayName("T037: Fallback search stops immediately and rethrows SERVER_ERROR on earlier date")
    void testFallbackSearchStopsOnServerError() {
        LocalDate requested = LocalDate.of(2026, 11, 20);
        LocalDate dMinus1 = LocalDate.of(2026, 11, 19);

        fakeSource.setResponse(requested, emptyXml);
        fakeSource.setError(dMinus1, new RateLoadException(RateLoadErrorType.SERVER_ERROR));

        RateLoadException ex = assertThrows(RateLoadException.class, () -> provider.getRates(requested));
        assertEquals(RateLoadErrorType.SERVER_ERROR, ex.getErrorType());
    }

    @Test
    @DisplayName("T037: Fallback search stops immediately and rethrows MALFORMED_RESPONSE on earlier date")
    void testFallbackSearchStopsOnMalformedXml() {
        LocalDate requested = LocalDate.of(2026, 11, 20);
        LocalDate dMinus1 = LocalDate.of(2026, 11, 19);

        fakeSource.setResponse(requested, emptyXml);
        fakeSource.setResponse(dMinus1, malformedXml);

        RateLoadException ex = assertThrows(RateLoadException.class, () -> provider.getRates(requested));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, ex.getErrorType());
    }

    @Test
    @DisplayName("T037: All 10 days empty throws EMPTY_RESPONSE")
    void testAllTenDaysEmptyThrows() {
        LocalDate requested = LocalDate.of(2026, 11, 20);
        fakeSource.setDefaultResponse(emptyXml);

        RateLoadException ex = assertThrows(RateLoadException.class, () -> provider.getRates(requested));
        assertEquals(RateLoadErrorType.EMPTY_RESPONSE, ex.getErrorType());
        assertTrue(ex.getUserMessage().contains("10 дней"));
    }

    @Test
    @DisplayName("T033: Future dates are prohibited with IllegalArgumentException")
    void testFutureDatesProhibited() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThrows(IllegalArgumentException.class, () -> provider.getRates(tomorrow));
    }

    @Test
    @DisplayName("T033: Session deduplication prevents redundant HTTP requests for the same date")
    void testSessionDeduplication() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 20);
        fakeSource.setResponse(date, weekdayXml);

        RateSet first = provider.getRates(date);
        RateSet second = provider.getRates(date);

        assertSame(first, second);
        assertEquals(1, fakeSource.getRequestCount(date));
    }

    @Test
    @DisplayName("T038: Cache origin is applied ONLY after explicit acceptance (loadFromCache)")
    void testCacheOriginOnlyAfterAcceptance() throws Exception {
        LocalDate requested = LocalDate.of(2026, 11, 20);

        // Pre-populate cache with weekday XML
        cache.save(weekdayXml);
        assertTrue(provider.hasUsableCache());
        assertEquals(LocalDate.of(2026, 11, 20), provider.getCachedDate().orElse(null));

        // Live source fails with NETWORK_ERROR
        fakeSource.setError(requested, new RateLoadException(RateLoadErrorType.NETWORK_ERROR));

        // Calling getRates fails with NETWORK_ERROR (does NOT silently switch to cache)
        RateLoadException ex = assertThrows(RateLoadException.class, () -> provider.getRates(requested));
        assertEquals(RateLoadErrorType.NETWORK_ERROR, ex.getErrorType());

        // Cache origin is applied ONLY when explicitly requested via loadFromCache
        RateSet cachedRateSet = provider.loadFromCache(requested);
        assertNotNull(cachedRateSet);
        assertEquals(requested, cachedRateSet.requestedDate());
        assertEquals(LocalDate.of(2026, 11, 20), cachedRateSet.applicableDate());
        assertEquals(RateOrigin.LOCAL_CACHE, cachedRateSet.origin());
        assertTrue(cachedRateSet.explanation().contains("локального кэша"));

        // Verify loadFromCache does NOT put in sessionCache: getRates still attempts live source
        assertFalse(provider.isDateLoadedInSession(requested));
        assertThrows(RateLoadException.class, () -> provider.getRates(requested));
    }

    @Test
    @DisplayName("T038: Cached weekend response applies weekend rule (applicable date = Friday)")
    void testCachedWeekendResponseAppliesWeekendRule() throws Exception {
        LocalDate requested = LocalDate.of(2026, 11, 22);

        // Pre-populate cache with weekend XML (root Date="22.11.2026" Sunday)
        cache.save(weekendXml);

        RateSet cachedRateSet = provider.loadFromCache(requested);
        assertEquals(requested, cachedRateSet.requestedDate());
        // Weekend rule: 22.11.2026 (Sunday) -> applicableDate = 20.11.2026 (Friday)
        assertEquals(LocalDate.of(2026, 11, 20), cachedRateSet.applicableDate());
        assertEquals(RateOrigin.LOCAL_CACHE, cachedRateSet.origin());
        assertTrue(cachedRateSet.explanation().contains("НБМ не устанавливает курс в выходные дни"));
        assertTrue(cachedRateSet.explanation().contains("локального кэша"));
    }

    @Test
    @DisplayName("T037: loadFromCache throws CACHE_UNAVAILABLE when cache is missing")
    void testLoadFromCacheWhenMissingThrows() {
        assertFalse(provider.hasUsableCache());
        RateLoadException ex = assertThrows(RateLoadException.class,
            () -> provider.loadFromCache(LocalDate.of(2026, 11, 20)));
        assertEquals(RateLoadErrorType.CACHE_UNAVAILABLE, ex.getErrorType());
    }
}
