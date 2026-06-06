package com.labassist.audit.web;

import com.labassist.audit.AuditLogRepository;
import com.labassist.common.web.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only view of the audit trail (restricted to ROLE_ADMIN in SecurityConfig). */
@Tag(name = "Audit")
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Operation(summary = "List recent audit entries, newest first (admin only)")
    @GetMapping
    public PagedResponse<AuditEntryDto> list(
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return PagedResponse.from(auditLogRepository.findAllByOrderByAtDesc(pageable).map(AuditEntryDto::from));
    }
}
