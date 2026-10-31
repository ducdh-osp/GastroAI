package vn.gastroai.be.infrastructure.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywaySeedMigrationContractTest {
    private static final String MYSQL_SEED =
            "db/migration/mysql/V9__seed_comprehensive_30_day_test_data.sql";
    private static final String POSTGRES_SEED =
            "db/migration/postgres/V46__seed_comprehensive_30_day_test_data.sql";
    private static final String AI_CORPUS_SEED =
            "db/migration/postgres/V44__seed_ai_data_corpus.sql";

    @Test
    void comprehensiveTestDataIsSeededByFlywayUsingNaturalKeys() throws IOException {
        String mysqlSql = readResource(MYSQL_SEED);
        String postgresSql = readResource(POSTGRES_SEED);

        assertEquals(
                Set.of("gastroai.admin01@gmail.com", "gastroai.editor01@gmail.com"),
                distinctMatches(mysqlSql, "gastroai\\.(?:admin|editor)\\d{2}@gmail\\.com")
        );
        assertEquals(3, distinctMatches(mysqlSql, "gastroai\\.doctor\\d{2}@gmail\\.com").size());
        assertTrue(mysqlSql.contains("INSERT INTO admin_login_history"));
        assertTrue(mysqlSql.contains("FROM admins a"));
        assertTrue(mysqlSql.contains("INSERT INTO doctor_login_history"));
        assertTrue(mysqlSql.contains("FROM doctors doctor"));

        assertEquals(5, distinctMatches(postgresSql, "gastroai\\.patient\\d{2}@gmail\\.com").size());
        assertTrue(postgresSql.contains("JOIN patients p ON p.email = d.email"));
        assertTrue(postgresSql.contains("WHERE email LIKE 'gastroai.patient%@gmail.com'"));
        assertTrue(postgresSql.contains("generate_series(0, 29)"));

        // Child rows must resolve auto-generated patient IDs from the seeded email addresses.
        Pattern directPatientIdValues = Pattern.compile(
                "(?is)INSERT\\s+INTO\\s+\\w+\\s*\\([^;]*?\\bpatient_id\\b[^;]*?\\)\\s*VALUES\\s*\\(\\s*\\d+"
        );
        assertFalse(directPatientIdValues.matcher(postgresSql).find());
    }

    @Test
    void aiCorpusIsStaticCompleteAndUses3072DimensionGeminiEmbeddings() throws IOException {
        String sql = readResource(AI_CORPUS_SEED);

        long documentInserts = sql.lines()
                .filter(line -> line.startsWith("INSERT INTO documents "))
                .count();
        long chunkInserts = sql.lines()
                .filter(line -> line.startsWith("INSERT INTO chunks "))
                .count();
        long embeddingInserts = sql.lines()
                .filter(line -> line.startsWith("INSERT INTO embeddings "))
                .count();

        assertEquals(25, documentInserts);
        assertEquals(238, chunkInserts);
        assertEquals(chunkInserts, embeddingInserts);
        assertEquals(25, countOccurrences(sql, "NULL, 'DONE', now(), now())"));
        assertEquals(chunkInserts, countOccurrences(sql, "FROM documents d WHERE d.title ="));
        assertEquals(
                embeddingInserts,
                countOccurrences(sql, "FROM chunks c JOIN documents d ON d.id = c.document_id WHERE d.title =")
        );
        assertEquals(embeddingInserts, countOccurrences(sql, "'gemini-embedding-2'"));

        Matcher vectors = Pattern.compile("'\\[([^]]+)]'::vector").matcher(sql);
        int vectorCount = 0;
        while (vectors.find()) {
            assertEquals(3072, vectorDimensions(vectors.group(1)), "Embedding has an invalid dimension");
            vectorCount++;
        }
        assertEquals(embeddingInserts, vectorCount);

        String lowerSql = sql.toLowerCase();
        assertFalse(lowerSql.contains("curl "));
        assertFalse(lowerSql.contains("httpclient"));
        assertFalse(lowerSql.contains("webclient"));
    }

    private static String readResource(String resource) throws IOException {
        try (InputStream stream = FlywaySeedMigrationContractTest.class
                .getClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing migration resource: " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Set<String> distinctMatches(String value, String regex) {
        Set<String> matches = new HashSet<>();
        Matcher matcher = Pattern.compile(regex).matcher(value);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }

    private static int countOccurrences(String value, String expected) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(expected, offset)) >= 0) {
            count++;
            offset += expected.length();
        }
        return count;
    }

    private static int vectorDimensions(String vector) {
        int dimensions = 1;
        for (int i = 0; i < vector.length(); i++) {
            if (vector.charAt(i) == ',') {
                dimensions++;
            }
        }
        return dimensions;
    }
}
