package md.bnm.converter.cache;

import md.bnm.converter.model.CachedResponse;
import md.bnm.converter.service.RateLoadException;
import md.bnm.converter.source.NbmXmlParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent cache for the last successfully loaded NBM XML response.
 * Stores raw XML under ~/.bnm-converter/. Reparses the cached XML using NbmXmlParser
 * to ensure integrity. A parse failure marks the cache unusable and produces a warning.
 */
public class ResponseCache {

    private static final String DEFAULT_CACHE_DIR = ".bnm-converter";
    private static final String DEFAULT_FILE_NAME = "cached_nbm_rates.xml";

    private final Path cacheFilePath;
    private final NbmXmlParser parser;

    public ResponseCache() {
        this(
            Path.of(System.getProperty("user.home"), DEFAULT_CACHE_DIR, DEFAULT_FILE_NAME),
            new NbmXmlParser()
        );
    }

    public ResponseCache(Path cacheFilePath) {
        this(cacheFilePath, new NbmXmlParser());
    }

    public ResponseCache(Path cacheFilePath, NbmXmlParser parser) {
        this.cacheFilePath = Objects.requireNonNull(cacheFilePath, "cacheFilePath must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
    }

    public synchronized void save(String rawXml) throws IOException {
        Objects.requireNonNull(rawXml, "rawXml must not be null");
        if (rawXml.isBlank()) {
            throw new IllegalArgumentException("Cannot cache blank XML response.");
        }

        Path parent = cacheFilePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Files.writeString(
            cacheFilePath,
            rawXml,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );
    }

    public synchronized Optional<CachedResponse> load() {
        if (!Files.exists(cacheFilePath)) {
            return Optional.empty();
        }

        try {
            String rawXml = Files.readString(cacheFilePath, StandardCharsets.UTF_8);
            if (rawXml.isBlank()) {
                System.err.println("Предупреждение: Файл кэша курсов валют пуст: " + cacheFilePath);
                return Optional.empty();
            }

            NbmXmlParser.ParsedXml parsed = parser.parse(rawXml);
            return Optional.of(new CachedResponse(parsed.date(), rawXml));
        } catch (RateLoadException e) {
            System.err.println("Предупреждение: Не удалось разобрать кэш курсов валют (" + cacheFilePath + "): " + e.getMessage());
            return Optional.empty();
        } catch (IOException e) {
            System.err.println("Предупреждение: Ошибка чтения кэша курсов валют (" + cacheFilePath + "): " + e.getMessage());
            return Optional.empty();
        }
    }

    public boolean hasCache() {
        return Files.exists(cacheFilePath);
    }

    public Path getCacheFilePath() {
        return cacheFilePath;
    }
}
