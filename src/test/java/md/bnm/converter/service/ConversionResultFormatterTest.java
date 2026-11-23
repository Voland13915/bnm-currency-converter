package md.bnm.converter.service;

import md.bnm.converter.model.ConversionResult;
import md.bnm.converter.model.Currency;
import md.bnm.converter.model.RateOrigin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ConversionResultFormatterTest {

    @Test
    @DisplayName("Format amount always displays 2 decimal places with HALF_UP")
    void testFormatAmount() {
        assertEquals("100.00", ConversionResultFormatter.formatAmount(new BigDecimal("100")));
        assertEquals("100.50", ConversionResultFormatter.formatAmount(new BigDecimal("100.5")));
        assertEquals("100.56", ConversionResultFormatter.formatAmount(new BigDecimal("100.556")));
        assertEquals("100.55", ConversionResultFormatter.formatAmount(new BigDecimal("100.554")));
    }

    @Test
    @DisplayName("Format rate displays at most 4 decimal places with HALF_UP")
    void testFormatRate() {
        assertEquals("1.0994", ConversionResultFormatter.formatRate(new BigDecimal("1.0993991369")));
        assertEquals("1.0", ConversionResultFormatter.formatRate(BigDecimal.ONE));
        assertEquals("20.1267", ConversionResultFormatter.formatRate(new BigDecimal("20.1267000000")));
        assertEquals("0.2235", ConversionResultFormatter.formatRate(new BigDecimal("0.2234600000")));
    }

    @Test
    @DisplayName("Format source and date includes local cache note when origin is LOCAL_CACHE")
    void testFormatLocalCacheProvenance() {
        ConversionResult result = new ConversionResult(
            new BigDecimal("100.00"),
            Currency.MDL,
            new Currency("EUR", "Euro"),
            new BigDecimal("4.97"),
            new BigDecimal("0.049685"),
            LocalDate.of(2026, 11, 22),
            LocalDate.of(2026, 11, 20),
            ConversionResult.DEFAULT_SOURCE_ATTRIBUTION,
            RateOrigin.LOCAL_CACHE,
            "Использованы курсы из локального кэша за 20.11.2026."
        );

        String sourceAndDate = ConversionResultFormatter.formatSourceAndDate(result);
        assertTrue(sourceAndDate.contains("20.11.2026"));
        assertTrue(sourceAndDate.contains("Национальный банк Молдовы"));
        assertTrue(sourceAndDate.contains("(локальный кэш)"));

        String full = ConversionResultFormatter.formatFullResult(result);
        assertTrue(full.contains("100.00 MDL = 4.97 EUR"));
        assertTrue(full.contains("Курс: 1 MDL = 0.0497 EUR"));
        assertTrue(full.contains("Примечание: Использованы курсы из локального кэша за 20.11.2026."));
    }
}
