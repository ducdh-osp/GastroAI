package vn.gastroai.be.application.rag;

import java.util.List;

public record StreamingDoneEvent(
        List<RagSource> sources,
        List<String> relatedQuestions,
        boolean emergency,
        List<String> matchedGroups
) {
}