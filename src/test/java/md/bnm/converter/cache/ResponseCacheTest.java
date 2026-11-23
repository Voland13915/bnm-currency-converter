package md.bnm.converter.cache;

import md.bnm.converter.TestFixtures;
import md.bnm.converter.model.CachedResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ResponseCacheTest {

    @Test
    @DisplayName("T036: Cache writes raw XML and loads valid CachedResponse with correct date")
    void testSaveAndLoad(@TempDir Path tempDir) throws Exception {
        Path cacheFile = tempDir.resolve("test-cache.xml");
        ResponseCache cache = new ResponseCache(cacheFile);

        assertFalse(cache.hasCache());
        assertTrue(cache.load().isEmpty());

        String xml = TestFixtures.loadFixture(TestFixtures.WEEKDAY_FIXTURE);
        cache.save(xml);

        assertTrue(cache.hasCache());
        Optional<CachedResponse> loadedOpt = cache.load();
        assertTrue(loadedOpt.isPresent());

        CachedResponse cached = loadedOpt.get();
        assertEquals(LocalDate.of(2026, 11, 20), cached.date());
        assertEquals(xml, cached.rawXml());
    }

    @Test
    @DisplayName("T036: Cache survives across new ResponseCache instance (application restart simulation)")
    void testRestartReloading(@TempDir Path tempDir) throws Exception {
        Path cacheFile = tempDir.resolve("restart-cache.xml");

        // Session 1: save response
        ResponseCache session1Cache = new ResponseCache(cacheFile);
        String xml = TestFixtures.loadFixture(TestFixtures.WEEKDAY_FIXTURE);
        session1Cache.save(xml);

        // Session 2 (restart): new instance reading same file
        ResponseCache session2Cache = new ResponseCache(cacheFile);
        assertTrue(session2Cache.hasCache());

        Optional<CachedResponse> loadedOpt = session2Cache.load();
        assertTrue(loadedOpt.isPresent());
        assertEquals(LocalDate.of(2026, 11, 20), loadedOpt.get().date());
    }

    @Test
    @DisplayName("T036: Corrupted XML syntax in cache file is ignored with a warning and returns empty")
    void testCorruptedXmlIgnored(@TempDir Path tempDir) throws IOException {
        Path cacheFile = tempDir.resolve("corrupted-cache.xml");
        Files.writeString(cacheFile, "<ValCurs Date=\"20.11.2026\"><Valute ID=\"1\"><broken", StandardCharsets.UTF_8);

        ResponseCache cache = new ResponseCache(cacheFile);
        assertTrue(cache.hasCache());

        Optional<CachedResponse> loadedOpt = cache.load();
        assertTrue(loadedOpt.isEmpty(), "Corrupted cache must be ignored and return empty Optional");
    }

    @Test
    @DisplayName("T036: Cache file containing zero Valute elements is ignored as unusable")
    void testEmptyXmlIgnored(@TempDir Path tempDir) throws IOException {
        Path cacheFile = tempDir.resolve("empty-cache.xml");
        String emptyXml = TestFixtures.loadFixture(TestFixtures.EMPTY_FIXTURE);
        Files.writeString(cacheFile, emptyXml, StandardCharsets.UTF_8);

        ResponseCache cache = new ResponseCache(cacheFile);
        Optional<CachedResponse> loadedOpt = cache.load();
        assertTrue(loadedOpt.isEmpty(), "Cache with zero rates must be treated as unusable");
    }

    @Test
    @DisplayName("T036: Blank or whitespace cache file is ignored")
    void testBlankCacheIgnored(@TempDir Path tempDir) throws IOException {
        Path cacheFile = tempDir.resolve("blank-cache.xml");
        Files.writeString(cacheFile, "   \n\t  ", StandardCharsets.UTF_8);

        ResponseCache cache = new ResponseCache(cacheFile);
        Optional<CachedResponse> loadedOpt = cache.load();
        assertTrue(loadedOpt.isEmpty(), "Blank cache must be ignored");
    }

    @Test
    @DisplayName("T036: Saving blank XML throws IllegalArgumentException")
    void testSaveBlankThrows(@TempDir Path tempDir) {
        Path cacheFile = tempDir.resolve("throws-cache.xml");
        ResponseCache cache = new ResponseCache(cacheFile);

        assertThrows(IllegalArgumentException.class, () -> cache.save("   "));
        assertThrows(NullPointerException.class, () -> cache.save(null));
    }
}
