package com.labassist.llm;

import com.labassist.audit.AuditService;
import com.labassist.audit.domain.AuditAction;
import com.labassist.common.exception.LlmUnavailableException;
import com.labassist.common.exception.NotFoundException;
import com.labassist.config.OllamaProperties;
import com.labassist.labresult.LabReportRepository;
import com.labassist.labresult.domain.LabReport;
import com.labassist.llm.domain.LlmInterpretation;
import com.labassist.llm.domain.LlmStatus;
import com.labassist.llm.web.LlmInterpretationDto;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates and stores LLM interpretations.
 *
 * <p>Deliberately NOT wrapped in a single transaction: the Ollama call can take
 * tens of seconds on CPU, so holding a DB transaction open would tie up a
 * connection. Reads and the result save each use their own short transaction.
 * Every attempt (success, failure, timeout) is persisted for auditability.
 */
@Slf4j
@Service
public class LlmInterpretationService {

    private static final int MAX_ERROR_LENGTH = 1024;

    private final LabReportRepository reportRepository;
    private final LlmInterpretationRepository interpretationRepository;
    private final OllamaClient ollamaClient;
    private final PromptBuilder promptBuilder;
    private final OllamaProperties ollamaProperties;
    private final AuditService auditService;

    public LlmInterpretationService(LabReportRepository reportRepository,
                                    LlmInterpretationRepository interpretationRepository,
                                    OllamaClient ollamaClient, PromptBuilder promptBuilder,
                                    OllamaProperties ollamaProperties, AuditService auditService) {
        this.reportRepository = reportRepository;
        this.interpretationRepository = interpretationRepository;
        this.ollamaClient = ollamaClient;
        this.promptBuilder = promptBuilder;
        this.ollamaProperties = ollamaProperties;
        this.auditService = auditService;
    }

    /** Returns the latest successful interpretation for a report, if any. */
    @Transactional(readOnly = true)
    public Optional<LlmInterpretationDto> findLatest(UUID reportId) {
        return interpretationRepository
                .findFirstByReport_IdAndStatusOrderByCreatedAtDesc(reportId, LlmStatus.SUCCESS)
                .map(LlmInterpretationDto::from);
    }

    public LlmInterpretationDto interpret(UUID reportId, boolean refresh, String actor, String ipAddress) {
        LabReport report = loadReport(reportId);

        if (!refresh) {
            Optional<LlmInterpretation> cached = interpretationRepository
                    .findFirstByReport_IdAndStatusOrderByCreatedAtDesc(reportId, LlmStatus.SUCCESS);
            if (cached.isPresent()) {
                return LlmInterpretationDto.from(cached.get());
            }
        }

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(report);

        LlmInterpretation interpretation = new LlmInterpretation();
        interpretation.setReport(report);
        interpretation.setModel(ollamaProperties.model());
        interpretation.setPromptVersion(PromptBuilder.PROMPT_VERSION);
        interpretation.setCreatedBy(actor);

        long start = System.nanoTime();
        try {
            String content = ollamaClient.chat(systemPrompt, userPrompt);
            interpretation.setResponseText(content);
            interpretation.setStatus(LlmStatus.SUCCESS);
            interpretation.setLatencyMs(elapsedMs(start));
            LlmInterpretation saved = interpretationRepository.save(interpretation);
            auditService.success(AuditAction.LLM_INTERPRET, actor, "LabReport", reportId.toString(),
                    Map.of("model", ollamaProperties.model(), "latencyMs", saved.getLatencyMs()), ipAddress);
            return LlmInterpretationDto.from(saved);
        } catch (Exception e) {
            boolean timedOut = isTimeout(e);
            interpretation.setStatus(timedOut ? LlmStatus.TIMEOUT : LlmStatus.FAILURE);
            interpretation.setLatencyMs(elapsedMs(start));
            interpretation.setErrorMessage(truncate(e.getMessage()));
            interpretationRepository.save(interpretation);
            auditService.failure(AuditAction.LLM_INTERPRET, actor, "LabReport", reportId.toString(),
                    Map.of("model", ollamaProperties.model(), "error", e.getClass().getSimpleName()), ipAddress);
            log.warn("LLM interpretation failed for report {}: {}", reportId, e.getMessage());
            throw new LlmUnavailableException(
                    timedOut ? "The AI assistant timed out. Please try again." : "The AI assistant is unavailable.", e);
        }
    }

    /**
     * Loads the report with its analytes. The repository's {@code @EntityGraph}
     * eagerly fetches the tests, so the detached entity is safe to read while
     * building the prompt (no open transaction held during the slow LLM call).
     */
    private LabReport loadReport(UUID reportId) {
        return reportRepository.findWithTestsById(reportId)
                .orElseThrow(() -> new NotFoundException("Lab report not found: " + reportId));
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
