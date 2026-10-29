package vn.gastroai.be.application.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.gastroai.be.api.cms.audit.AuditLogPageResponse;
import vn.gastroai.be.api.cms.audit.AuditLogResponse;
import vn.gastroai.be.application.auth.ClientRequestInfo;
import vn.gastroai.be.application.auth.ClientRequestInfoResolver;
import vn.gastroai.be.application.auth.CmsAuthService;
import vn.gastroai.be.domain.admin.CmsAccountType;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.audit.AuditLog;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AuditLogRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** UC0052, UC0053, UC0064 - ghi, loc va lay du lieu xuat audit log cua tai khoan CMS. */
@Service
public class AuditLogService {

    public static final int MAX_EXPORT_ROWS = 10_000;

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final int MAX_DETAIL = 1000;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "occurredAt");

    private final AuditLogRepository auditLogRepository;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final ClientRequestInfoResolver requestInfoResolver;
    private final AuditAnomalyDetector anomalyDetector;

    public AuditLogService(AuditLogRepository auditLogRepository,
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            ClientRequestInfoResolver requestInfoResolver,
            AuditAnomalyDetector anomalyDetector) {
        this.auditLogRepository = auditLogRepository;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.requestInfoResolver = requestInfoResolver;
        this.anomalyDetector = anomalyDetector;
    }

    public void record(AuditAction action, String targetType, Object targetId, String detail) {
        try {
            HttpServletRequest request = currentRequest();
            String actorType = "SYSTEM";
            Long actorId = null;
            String actorEmail = null;
            String ip = "unknown";
            String userAgent = "unknown";

            if (request != null) {
                ClientRequestInfo info = requestInfoResolver.resolve(request);
                ip = info.ipAddress();
                userAgent = info.userAgent();

                HttpSession session = request.getSession(false);
                if (session != null) {
                    Object id = session.getAttribute(CmsAuthService.AUTH_USER_ID);
                    Object type = session.getAttribute(CmsAuthService.AUTH_ACCOUNT_TYPE);
                    if (id != null && type != null) {
                        actorId = Long.valueOf(id.toString());
                        actorType = type.toString();
                        actorEmail = findEmail(actorType, actorId);
                    }
                }
            }

            AuditLog saved = auditLogRepository.save(new AuditLog(
                    Instant.now(), actorType, actorId, actorEmail, action,
                    targetType, targetId == null ? null : targetId.toString(),
                    truncate(detail), ip, truncate(userAgent)));
            anomalyDetector.check(saved);
        } catch (RuntimeException exception) {
            // Khong de loi ghi audit lam hong thao tac chinh da thanh cong.
            log.error("Ghi audit log that bai: action={}, target={}:{}", action, targetType, targetId, exception);
        }
    }

    public String currentActorEmail() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object id = session.getAttribute(CmsAuthService.AUTH_USER_ID);
        Object type = session.getAttribute(CmsAuthService.AUTH_ACCOUNT_TYPE);
        if (id == null || type == null) {
            return null;
        }
        return findEmail(type.toString(), Long.valueOf(id.toString()));
    }

    @Transactional(value = "mysqlTransactionManager", readOnly = true)
    public AuditLogPageResponse search(LocalDate fromDate, LocalDate toDate, AuditAction action,
            String targetType, String keyword, int page, int size) {
        SearchCriteria criteria = toCriteria(fromDate, toDate, action, targetType, keyword);
        Page<AuditLogResponse> result = runSearch(criteria, PageRequest.of(page, size, NEWEST_FIRST))
                .map(AuditLogResponse::from);
        return AuditLogPageResponse.from(result);
    }

    @Transactional(value = "mysqlTransactionManager", readOnly = true)
    public List<AuditLogResponse> findForExport(LocalDate fromDate, LocalDate toDate, AuditAction action,
            String targetType, String keyword) {
        SearchCriteria criteria = toCriteria(fromDate, toDate, action, targetType, keyword);
        Page<AuditLog> result = runSearch(criteria, PageRequest.of(0, MAX_EXPORT_ROWS, NEWEST_FIRST));
        if (result.getTotalElements() > MAX_EXPORT_ROWS) {
            throw new IllegalArgumentException("Có " + result.getTotalElements()
                    + " bản ghi khớp bộ lọc, vượt quá giới hạn xuất " + MAX_EXPORT_ROWS
                    + " dòng. Vui lòng thu hẹp khoảng thời gian hoặc thêm bộ lọc.");
        }
        return result.getContent().stream().map(AuditLogResponse::from).toList();
    }

    private SearchCriteria toCriteria(LocalDate fromDate, LocalDate toDate, AuditAction action,
            String targetType, String keyword) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Từ ngày phải trước hoặc bằng Đến ngày");
        }
        Instant fromTime = fromDate == null ? null : fromDate.atStartOfDay(VIETNAM).toInstant();
        Instant toTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay(VIETNAM).toInstant();

        String rawKeyword = normalize(keyword);
        String likeKeyword = rawKeyword == null ? null : "%" + rawKeyword.toLowerCase() + "%";

        return new SearchCriteria(fromTime, toTime, action, normalize(targetType), likeKeyword, rawKeyword);
    }

    private Page<AuditLog> runSearch(SearchCriteria criteria, Pageable pageable) {
        return auditLogRepository.search(criteria.fromTime(), criteria.toTime(), criteria.action(),
                criteria.targetType(), criteria.likeKeyword(), criteria.rawKeyword(), pageable);
    }

    private record SearchCriteria(Instant fromTime, Instant toTime, AuditAction action,
            String targetType, String likeKeyword, String rawKeyword) {
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String findEmail(String actorType, Long actorId) {
        if (CmsAccountType.ADMIN.name().equals(actorType)) {
            return adminRepository.findById(actorId).map(a -> a.getEmail()).orElse(null);
        }
        if (CmsAccountType.DOCTOR.name().equals(actorType)) {
            return doctorRepository.findById(actorId).map(d -> d.getEmail()).orElse(null);
        }
        return null;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_DETAIL ? value : value.substring(0, MAX_DETAIL);
    }
}