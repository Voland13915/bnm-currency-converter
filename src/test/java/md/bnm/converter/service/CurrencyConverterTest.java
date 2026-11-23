package md.bnm.converter.service;

import md.bnm.converter.TestFixtures;
import md.bnm.converter.model.ConversionResult;
import md.bnm.converter.model.Currency;
import md.bnm.converter.model.RateOrigin;
import md.bnm.converter.model.RateSet;
import md.bnm.converter.source.NbmXmlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyConverterTest {

    private CurrencyConverter converter;
    private RateSet rateSet;

    @BeforeEach
    void setUp() throws Exception {
        converter = new CurrencyConverter();

        NbmXmlParser parser = new NbmXmlParser();
        String xml = TestFixtures.loadFixture(TestFixtures.WEEKDAY_FIXTURE);
        NbmXmlParser.ParsedXml parsed = parser.parse(xml);

        rateSet = new RateSet(
            LocalDate.of(2026, 11, 20),
            LocalDate.of(2026, 11, 20),
            parsed.rates(),
            RateOrigin.LIVE,
            null
        );
    }

    @Test
    @DisplayName("T032: Same-currency conversion returns entered amount with cross-rate 1 (MDL to MDL)")
    void testSameCurrencyMdl() throws Exception {
        BigDecimal amount = new BigDecimal("250.75");
        ConversionResult result = converter.convert(amount, Currency.MDL, Currency.MDL, rateSet);

        assertEquals(new BigDecimal("250.75"), result.convertedAmount());
        assertEquals(BigDecimal.ONE, result.crossRate());
        assertEquals(Currency.MDL, result.sourceCurrency());
        assertEquals(Currency.MDL, result.targetCurrency());
    }

    @Test
    @DisplayName("T032: Same-currency conversion returns entered amount with cross-rate 1 (EUR to EUR)")
    void testSameCurrencyEur() throws Exception {
        BigDecimal amount = new BigDecimal("100.00");
        Currency eur = rateSet.getRate("EUR").currency();

        ConversionResult result = converter.convert(amount, eur, eur, rateSet);

        assertEquals(new BigDecimal("100.00"), result.convertedAmount());
        assertEquals(BigDecimal.ONE, result.crossRate());
    }

    @Test
    @DisplayName("T032: Cross-currency conversion through MDL (EUR to MDL)")
    void testEurToMdl() throws Exception {
        BigDecimal amount = new BigDecimal("100.00");
        Currency eur = rateSet.getRate("EUR").currency();

        ConversionResult result = converter.convert(amount, eur, Currency.MDL, rateSet);

        // 100 * 20.1267 = 2012.67 MDL
        assertEquals(new BigDecimal("2012.67"), result.convertedAmount());
        assertEquals(new BigDecimal("20.1267000000"), result.crossRate());
        assertEquals("Национальный банк Молдовы", result.sourceAttribution());
        assertEquals(LocalDate.of(2026, 11, 20), result.applicableDate());
    }

    @Test
    @DisplayName("T032: Cross-currency conversion through MDL (MDL to EUR)")
    void testMdlToEur() throws Exception {
        BigDecimal amount = new BigDecimal("2012.67");
        Currency eur = rateSet.getRate("EUR").currency();

        ConversionResult result = converter.convert(amount, Currency.MDL, eur, rateSet);

        // 2012.67 / 20.1267 = 100.00 EUR
        assertEquals(new BigDecimal("100.00"), result.convertedAmount());
    }

    @Test
    @DisplayName("T032: Cross-currency conversion between non-MDL currencies (EUR to USD)")
    void testEurToUsd() throws Exception {
        BigDecimal amount = new BigDecimal("100.00");
        Currency eur = rateSet.getRate("EUR").currency();
        Currency usd = rateSet.getRate("USD").currency();

        ConversionResult result = converter.convert(amount, eur, usd, rateSet);

        // Cross-rate: 20.1267 / 18.3070 = 1.0993991369...
        // Amount: 100 * 20.1267 / 18.3070 = 109.9399... -> rounded HALF_UP = 109.94
        assertEquals(new BigDecimal("109.94"), result.convertedAmount());
        assertEquals(new BigDecimal("1.0993991369"), result.crossRate());

        // Formatting verification: cross rate up to 4 decimals
        String formattedRate = ConversionResultFormatter.formatRate(result.crossRate());
        assertEquals("1.0994", formattedRate);
    }

    @Test
    @DisplayName("T032: Conversion with Nominal 10 and Nominal 100 currencies (ALL to HUF)")
    void testNominal10ToNominal100() throws Exception {
        // ALL: Nominal 10, Value 2.2346 -> ratePerUnit 0.2234600000
        // HUF: Nominal 100, Value 5.4553 -> ratePerUnit 0.0545530000
        Currency all = rateSet.getRate("ALL").currency();
        Currency huf = rateSet.getRate("HUF").currency();

        BigDecimal amount = new BigDecimal("1000.00");
        ConversionResult result = converter.convert(amount, all, huf, rateSet);

        // 1000 * 0.22346 / 0.054553 = 4096.196... -> 4096.20
        assertEquals(new BigDecimal("4096.20"), result.convertedAmount());
    }

    @Test
    @DisplayName("T034: Unavailable currency missing from rate set is blocked with CURRENCY_NOT_AVAILABLE")
    void testMissingCurrencyBlocked() {
        Currency missing = new Currency("XYZ", "Unknown Currency");
        Currency eur = rateSet.getRate("EUR").currency();

        RateLoadException exSource = assertThrows(RateLoadException.class,
            () -> converter.convert(new BigDecimal("100.00"), missing, eur, rateSet));
        assertEquals(RateLoadErrorType.CURRENCY_NOT_AVAILABLE, exSource.getErrorType());

        RateLoadException exTarget = assertThrows(RateLoadException.class,
            () -> converter.convert(new BigDecimal("100.00"), eur, missing, rateSet));
        assertEquals(RateLoadErrorType.CURRENCY_NOT_AVAILABLE, exTarget.getErrorType());
    }

    @Test
    @DisplayName("T035: Reverse conversion recovery within two-decimal display rounding")
    void testReverseConversionRecovery() throws Exception {
        Currency eur = rateSet.getRate("EUR").currency();
        Currency usd = rateSet.getRate("USD").currency();

        BigDecimal originalAmount = new BigDecimal("100.00");

        // Forward: EUR -> USD
        ConversionResult forward = converter.convert(originalAmount, eur, usd, rateSet);
        assertEquals(new BigDecimal("109.94"), forward.convertedAmount());

        // Reverse: USD -> EUR using forward converted amount
        ConversionResult reverse = converter.convert(forward.convertedAmount(), usd, eur, rateSet);
        // 109.94 * 18.3070 / 20.1267 = 100.00007... -> rounds to 100.00
        assertEquals(originalAmount, reverse.convertedAmount());
    }
}
