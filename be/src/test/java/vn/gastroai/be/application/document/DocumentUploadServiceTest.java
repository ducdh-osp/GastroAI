package vn.gastroai.be.application.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private DocumentUploadService documentUploadService;

    // UC0049 - ban moi nhat cua nhom dang PENDING/PROCESSING thi chua cho tai ban moi len
    // (-> IllegalStateException, GlobalExceptionHandler doi thanh 409). Phai chan TRUOC khi
    // luu file xuong dia, neu khong se de lai file mo coi.
    @Test
    void uploadNewVersionRejectsWhenPreviousVersionStillProcessing() {
        Document v1 = new Document();
        v1.setId(1L);
        v1.setVersionNo(1);
        v1.setStatus(DocumentStatus.PROCESSING);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(documentRepository.findGroup(1L)).thenReturn(List.of(v1));

        assertThrows(IllegalStateException.class,
                () -> documentUploadService.uploadNewVersion(1L, file, null));

        verify(fileStorageService, never()).store(any());
        verify(documentRepository, never()).save(any());
    }

    // UC0049 - tai lieu goc khong ton tai -> ResourceNotFoundException (404), khong luu gi ca.
    @Test
    void uploadNewVersionThrowsWhenDocumentNotFound() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> documentUploadService.uploadNewVersion(99L, file, null));

        verify(fileStorageService, never()).store(any());
        verify(documentRepository, never()).save(any());
    }
}