package vn.gastroai.be.application.document;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * SCR-AD-04 §3.2 (UC0049) - moi tai lieu chi co 1 ban "that": thay the = xoa vinh vien ban cu
 * (document + chunk + embedding, chunk/embedding xoa theo ON DELETE CASCADE), khong giu lich su.
 * File goc tren dia chi xoa SAU KHI giao dich da commit - commit loi thi van con file.
 */
@Component
public class DocumentGroupPruner {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    public DocumentGroupPruner(DocumentRepository documentRepository, FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
    }
    public int keepOnly(Document keep, List<Document> group) {
        List<Document> others = group.stream()
                .filter(document -> !document.getId().equals(keep.getId()))
                .toList();
        if (others.isEmpty()) {
            return 0;
        }
        if (keep.isReplacement()) {
            Document oldMain = others.stream()
                    .filter(document -> document.getId().equals(keep.getReplacesId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Thiếu dòng chính id=" + keep.getReplacesId() + " của bản thay thế id=" + keep.getId()));
            // Ban thay the luon co unpublished_at NULL (ck_documents_unpublished_on_main_row),
            // nen co "da go" chi nam o dong chinh cu - chuyen sang dong chinh moi.
            keep.setUnpublishedAt(oldMain.getUnpublishedAt());
            keep.setReplacesId(null);
            documentRepository.saveAndFlush(keep);
        }
        deleteRows(others);
        return others.size();
    }

    /** Xoa vinh vien ca tai lieu: khi xoa tai lieu, hoac thay the 1 tai lieu chua tung duoc dung cho AI. */
    public void deleteAll(List<Document> group) {
        deleteRows(group);
    }

    private void deleteRows(List<Document> rows) {
        List<Path> files = rows.stream()
                .map(Document::getSource)
                .filter(Objects::nonNull)
                .map(Path::of)
                .toList();
        documentRepository.deleteAllByIdInBatch(rows.stream().map(Document::getId).toList());
        files.forEach(this::deleteFileAfterCommit);
    }

    private void deleteFileAfterCommit(Path file) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorageService.delete(file);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.delete(file);
            }
        });
    }
}