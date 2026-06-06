package com.labassist.labresult.web;

import com.labassist.common.web.PagedResponse;
import com.labassist.common.web.RequestUtils;
import com.labassist.labresult.LabResultService;
import com.labassist.labresult.domain.ReportStatus;
import com.labassist.security.AppUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @Operation(summary = "List lab reports (paged, filterable by abnormal/status/external-id)")
    @GetMapping
    public PagedResponse<LabReportSummaryDto> list(
            @RequestParam(required = false) Boolean abnormalOnly,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) String q,
            @ParameterObject @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return labResultService.list(abnormalOnly, status, q, pageable);
    }

    @Operation(summary = "Get one lab report with its analytes and flags")
    @GetMapping("/{id}")
    public LabReportDetailDto get(@PathVariable UUID id,
                                  @AuthenticationPrincipal AppUserDetails principal,
                                  HttpServletRequest request) {
        return labResultService.get(id, principal.getUsername(), RequestUtils.clientIp(request));
    }
}
