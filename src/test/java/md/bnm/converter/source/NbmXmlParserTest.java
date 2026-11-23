package md.bnm.converter.source;

import md.bnm.converter.TestFixtures;
import md.bnm.converter.model.PublishedRate;
import md.bnm.converter.service.RateLoadErrorType;
import md.bnm.converter.service.RateLoadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class NbmXmlParserTest {

    private NbmXmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new NbmXmlParser();
    }

    @Test
    @DisplayName("T029: Parse real weekday XML fixture with nominal 1, 10, and 100 quotes")
    void testParseRealWeekdayFixture() throws Exception {
        String xml = TestFixtures.loadFixture(TestFixtures.WEEKDAY_FIXTURE);
        NbmXmlParser.ParsedXml result = parser.parse(xml);

        assertNotNull(result);
        assertEquals(LocalDate.of(2026, 11, 20), result.date());
        assertTrue(result.hasRates());
        assertFalse(result.rates().isEmpty());

        // Nominal 1: EUR
        PublishedRate eur = result.rates().get("EUR");
        assertNotNull(eur);
        assertEquals("EUR", eur.currencyCode());
        assertEquals("978", eur.currency().numCode());
        assertEquals("Euro", eur.currency().displayName());
        assertEquals(1, eur.nominal());
        assertEquals(new BigDecimal("20.1267"), eur.value());
        assertEquals(new BigDecimal("20.1267000000"), eur.ratePerUnit());

        // Nominal 1: USD
        PublishedRate usd = result.rates().get("USD");
        assertNotNull(usd);
        assertEquals("USD", usd.currencyCode());
        assertEquals("840", usd.currency().numCode());
        assertEquals(1, usd.nominal());
        assertEquals(new BigDecimal("18.3070"), usd.value());
        assertEquals(new BigDecimal("18.3070000000"), usd.ratePerUnit());

        // Nominal 10: ALL (Albanian lek)
        PublishedRate all = result.rates().get("ALL");
        assertNotNull(all);
        assertEquals("ALL", all.currencyCode());
        assertEquals("008", all.currency().numCode());
        assertEquals("Albanian lek", all.currency().displayName());
        assertEquals(10, all.nominal());
        assertEquals(new BigDecimal("2.2346"), all.value());
        // Value / Nominal at scale 10: 2.2346 / 10 = 0.2234600000
        assertEquals(new BigDecimal("0.2234600000"), all.ratePerUnit());

        // Nominal 100: HUF (Hungarian Forint)
        PublishedRate huf = result.rates().get("HUF");
        assertNotNull(huf);
        assertEquals("HUF", huf.currencyCode());
        assertEquals("348", huf.currency().numCode());
        assertEquals(100, huf.nominal());
        assertEquals(new BigDecimal("5.4553"), huf.value());
        // Value / Nominal at scale 10: 5.4553 / 100 = 0.0545530000
        assertEquals(new BigDecimal("0.0545530000"), huf.ratePerUnit());

        // Nominal 100: JPY (Japanese Yen)
        PublishedRate jpy = result.rates().get("JPY");
        assertNotNull(jpy);
        assertEquals("JPY", jpy.currencyCode());
        assertEquals("392", jpy.currency().numCode());
        assertEquals(100, jpy.nominal());
        assertEquals(new BigDecimal("11.4480"), jpy.value());
        // Value / Nominal at scale 10: 11.4480 / 100 = 0.1144800000
        assertEquals(new BigDecimal("0.1144800000"), jpy.ratePerUnit());
    }

    @Test
    @DisplayName("T029: Parse real weekend XML fixture")
    void testParseRealWeekendFixture() throws Exception {
        String xml = TestFixtures.loadFixture(TestFixtures.WEEKEND_FIXTURE);
        NbmXmlParser.ParsedXml result = parser.parse(xml);

        assertNotNull(result);
        assertEquals(LocalDate.of(2026, 11, 22), result.date());
        assertTrue(result.hasRates());

        PublishedRate eur = result.rates().get("EUR");
        assertNotNull(eur);
        assertEquals(new BigDecimal("20.1267000000"), eur.ratePerUnit());
    }

    @Test
    @DisplayName("T030: Valid XML with no Valute elements is parsed as empty rates (not malformed)")
    void testParseEmptyXmlFixture() throws Exception {
        String xml = TestFixtures.loadFixture(TestFixtures.EMPTY_FIXTURE);
        NbmXmlParser.ParsedXml result = parser.parse(xml);

        assertNotNull(result);
        assertEquals(LocalDate.of(2026, 11, 23), result.date());
        assertFalse(result.hasRates());
        assertTrue(result.rates().isEmpty());
    }

    @Test
    @DisplayName("T030: Malformed XML fixture throws MALFORMED_RESPONSE")
    void testParseMalformedXmlFixture() {
        String xml = TestFixtures.loadFixture(TestFixtures.MALFORMED_FIXTURE);
        RateLoadException ex = assertThrows(RateLoadException.class, () -> parser.parse(xml));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, ex.getErrorType());
    }

    @Test
    @DisplayName("T030: Null and blank XML content throws MALFORMED_RESPONSE")
    void testBlankAndNullXml() {
        RateLoadException exNull = assertThrows(RateLoadException.class, () -> parser.parse((String) null));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, exNull.getErrorType());

        RateLoadException exBlank = assertThrows(RateLoadException.class, () -> parser.parse("   "));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, exBlank.getErrorType());
    }

    @Test
    @DisplayName("T030: Root element missing or wrong tag throws MALFORMED_RESPONSE")
    void testInvalidRootElement() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ExchangeRates Date=\"20.11.2026\"></ExchangeRates>";
        RateLoadException ex = assertThrows(RateLoadException.class, () -> parser.parse(xml));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, ex.getErrorType());
    }

    @Test
    @DisplayName("T030: Missing or invalid Date attribute throws MALFORMED_RESPONSE")
    void testMissingDateAttribute() {
        String xmlNoDate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ValCurs name=\"Official exchange rate\"></ValCurs>";
        RateLoadException ex1 = assertThrows(RateLoadException.class, () -> parser.parse(xmlNoDate));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, ex1.getErrorType());

        String xmlBadDate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ValCurs Date=\"not-a-date\"></ValCurs>";
        RateLoadException ex2 = assertThrows(RateLoadException.class, () -> parser.parse(xmlBadDate));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, ex2.getErrorType());
    }

    @Test
    @DisplayName("T030: Missing required Valute fields throws MALFORMED_RESPONSE")
    void testMissingValuteFields() {
        // Missing CharCode
        String xmlNoCharCode = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ValCurs Date="20.11.2026">
                <Valute ID="1"><Nominal>1</Nominal><Value>10.0</Value><Name>Test</Name></Valute>
            </ValCurs>
            """;
        RateLoadException ex1 = assertThrows(RateLoadException.class, () -> parser.parse(xmlNoCharCode));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, ex1.getErrorType());

        // Missing Nominal
        String xmlNoNominal = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ValCurs Date="20.11.2026">
                <Valute ID="1"><CharCode>USD</CharCode><Value>10.0</Value><Name>Test</Name></Valute>
            </ValCurs>
            """;
        RateLoadException ex2 = assertThrows(RateLoadException.class, () -> parser.parse(xmlNoNominal));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, ex2.getErrorType());

        // Non-positive Nominal
        String xmlZeroNominal = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ValCurs Date="20.11.2026">
                <Valute ID="1"><CharCode>USD</CharCode><Nominal>0</Nominal><Value>10.0</Value><Name>Test</Name></Valute>
            </ValCurs>
            """;
        RateLoadException ex3 = assertThrows(RateLoadException.class, () -> parser.parse(xmlZeroNominal));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, ex3.getErrorType());

        // Invalid Value
        String xmlBadValue = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ValCurs Date="20.11.2026">
                <Valute ID="1"><CharCode>USD</CharCode><Nominal>1</Nominal><Value>invalid</Value><Name>Test</Name></Valute>
            </ValCurs>
            """;
        RateLoadException ex4 = assertThrows(RateLoadException.class, () -> parser.parse(xmlBadValue));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, ex4.getErrorType());
    }

    @Test
    @DisplayName("T030: DTD declarations and external entities are disallowed")
    void testDtdDisallowed() {
        String xxeXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE ValCurs [
                <!ENTITY xxe SYSTEM "file:///non-existent-file.txt">
            ]>
            <ValCurs Date="20.11.2026">
                <Valute ID="1"><CharCode>&xxe;</CharCode><Nominal>1</Nominal><Value>10</Value><Name>Test</Name></Valute>
            </ValCurs>
            """;
        RateLoadException ex = assertThrows(RateLoadException.class, () -> parser.parse(xxeXml));
        assertEquals(RateLoadErrorType.MALFORMED_RESPONSE, ex.getErrorType());
    }
}
