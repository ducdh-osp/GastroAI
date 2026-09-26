package vn.gastroai.be.application.chat;

import vn.gastroai.be.application.rag.RagAnswer;

/**
 * UC0017 + UC0034/035 - cau tra loi RAG kem co bao hieu khan cap (Triage). Gemini van tra loi
 * binh thuong du emergency=true (khong chan som) - chi la co de FE hien thi canh bao noi bat
 * kem theo, tranh bo sot dau hieu cap cuu ma nguoi dung khong tu nhan ra.
 */
public record ChatAnswer(RagAnswer ragAnswer, boolean emergency) {
}
