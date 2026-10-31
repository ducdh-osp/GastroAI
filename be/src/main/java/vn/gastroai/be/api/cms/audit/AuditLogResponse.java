package vn.gastroai.be.api.cms.audit;

import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.audit.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Instant occurredAt,
        String actorType,
        Long actorId,
        String actorEmail,
        AuditAction action,
        String targetType,
        String targetId,
        String detail,
        String ipAddress,
        String userAgent
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getOccurredAt(),
                log.getActorType(),
                log.getActorId(),
                log.getActorEmail(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetail(),
                log.getIpAddress(),
                log.getUserAgent());
    }
}