package vn.gastroai.be.api.document;

public record DocumentStats(
        int total,
        int ready,
        int processing,
        int error
) {
}