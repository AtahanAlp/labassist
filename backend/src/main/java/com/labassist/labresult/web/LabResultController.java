package com.labassist.labresult.web;

import com.labassist.common.web.PagedResponse;
import com.labassist.common.web.RequestUtils;
import com.labassist.labresult.LabResultService;
import com.labassist.labresult.domain.ReportStatus;
import com.labassist.security.AppUserDetails;
import com.labassist.security.domain.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Lab Reports")
@RestController
@RequestMapping("/api/lab-reports")
public class LabResultController {

    private final LabResultService labResultService;

    public LabResultController(LabResultService labResultService) {
        this.labResultService = labResultService;
    }

    @Operation(summary = "List lab reports (paged; filter by abnormal/critical/status/date/external-id)")
    @GetMapping
    public PagedResponse<LabReportSummaryDto> list(
            @RequestParam(required = false) Boolean abnormalOnly,
            @RequestParam(required = false) Boolean criticalOnly,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AppUserDetails principal,
            @ParameterObject @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return labResultService.list(abnormalOnly, criticalOnly, status, q,
                toInstant(from, false), toInstant(to, true), isAdmin(principal), pageable);
    }

    @Operation(summary = "Counts for the dashboard header (scoped to the caller's visibility)")
    @GetMapping("/summary")
    public ReportSummaryDto summary(@AuthenticationPrincipal AppUserDetails principal) {
        return labResultService.summary(isAdmin(principal));
    }

    @Operation(summary = "Get one lab report with its analytes and flags")
    @GetMapping("/{id}")
    public LabReportDetailDto get(@PathVariable UUID id,
                                  @AuthenticationPrincipal AppUserDetails principal,
                                  HttpServletRequest request) {
        return labResultService.get(id, principal.getUsername(), RequestUtils.clientIp(request), isAdmin(principal));
    }

    private static boolean isAdmin(AppUserDetails principal) {
        return principal.user().getRole() == UserRole.ADMIN;
    }

    /** A day-granularity bound; {@code exclusiveEnd} shifts a "to" date to the next day's start. */
    private static Instant toInstant(LocalDate date, boolean exclusiveEnd) {
        if (date == null) {
            return null;
        }
        LocalDate effective = exclusiveEnd ? date.plusDays(1) : date;
        return effective.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
