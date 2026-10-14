package vn.gastroai.be.application.support;

/** Escapes user supplied text before it is embedded in a SQL LIKE pattern. */
public final class SearchQueryEscaper {
    private SearchQueryEscaper() {
    }

    /**
     * Uses an explicit exclamation mark escape character in the JPQL queries.
     * Escaping the escape character first keeps wildcard characters literal.
     */
    public static String escapeLike(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("\\", "!\\");
    }
}
