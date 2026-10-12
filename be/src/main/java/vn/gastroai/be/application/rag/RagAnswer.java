package vn.gastroai.be.application.rag;

import java.util.List;

/**
 * UC0029 - cau tra loi kem danh sach nguon da dung (RagSource) de FE hien thi trich dan, va
 * goi y cau hoi lien quan (relatedQuestions) de nguoi dung khai thac them.
 */
public record RagAnswer(String answer, List<RagSource> sources, List<String> relatedQuestions) {
}
