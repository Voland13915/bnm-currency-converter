package md.bnm.converter;

import md.bnm.converter.service.RateLoadException;
import md.bnm.converter.source.NbmRateSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * Reusable test helpers and in-memory FakeNbmRateSource for unit testing without network calls.
 */
public final class TestFixtures {

    public static final String WEEKDAY_FIXTURE = "bnm-weekday.xml";
    public static final String WEEKEND_FIXTURE = "bnm-weekend.xml";
    public static final String EMPTY_FIXTURE = "bnm-empty.xml";
    public static final String MALFORMED_FIXTURE = "bnm-malformed.xml";

    private TestFixtures() {
    }

    public static String loadFixture(String resourceName) {
        String path = resourceName.startsWith("/") ? resourceName : "/" + resourceName;
        try (InputStream is = TestFixtures.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Fixture resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read fixture: " + path, e);
        }
    }

    public static class FakeNbmRateSource extends NbmRateSource {
        private final Map<LocalDate, String> responses = new HashMap<>();
        private final Map<LocalDate, RateLoadException> errors = new HashMap<>();
        private String defaultResponse;
        private RateLoadException defaultError;
        private final List<LocalDate> requestHistory = new ArrayList<>();

        public FakeNbmRateSource() {
            super();
        }

        public void setResponse(LocalDate date, String xml) {
            responses.put(date, xml);
        }

        public void setError(LocalDate date, RateLoadException error) {
            errors.put(date, error);
        }

        public void setDefaultResponse(String xml) {
            this.defaultResponse = xml;
        }

        public void setDefaultError(RateLoadException error) {
            this.defaultError = error;
        }

        public List<LocalDate> getRequestHistory() {
            return Collections.unmodifiableList(requestHistory);
        }

        public int getRequestCount(LocalDate date) {
            int count = 0;
            for (LocalDate d : requestHistory) {
                if (d.equals(date)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public String fetchXml(LocalDate date) throws RateLoadException {
            requestHistory.add(date);

            if (errors.containsKey(date)) {
                throw errors.get(date);
            }
            if (responses.containsKey(date)) {
                return responses.get(date);
            }
            if (defaultError != null) {
                throw defaultError;
            }
            if (defaultResponse != null) {
                return defaultResponse;
            }

            throw new IllegalStateException("No mocked response or error configured for date: " + date);
        }
    }
}
