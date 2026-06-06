package com.labassist.llm.web;

import com.labassist.common.web.RequestUtils;
import com.labassist.llm.LlmInterpretationService;
import com.labassist.security.AppUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Interpretation")
@RestController
@RequestMapping("/api/lab-reports/{id}/interpretation")
public class LlmController {

    private final LlmInterpretationService interpretationService;

    public LlmController(LlmInterpretationService interpretationService) {
        this.interpretationService = interpretationService;
    }

    @Operation(summary = "Generate (or return the cached) AI interpretation for a report")
    @PostMapping
    public LlmInterpretationDto interpret(@PathVariable UUID id,
                                          @RequestParam(defaultValue = "false") boolean refresh,
                                          @AuthenticationPrincipal AppUserDetails principal,
                                          HttpServletRequest request) {
        return interpretationService.interpret(id, refresh, principal.getUsername(), RequestUtils.clientIp(request));
    }

    @Operation(summary = "Return the latest stored AI interpretation, if any")
    @GetMapping
    public ResponseEntity<LlmInterpretationDto> latest(@PathVariable UUID id) {
        return interpretationService.findLatest(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
