package com.labassist;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labassist.ingestion.MessageIngestor;
import com.labassist.labresult.LabReportRepository;
import com.labassist.llm.OllamaClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end API tests (security, results, LLM) over MockMvc against a Testcontainers Postgres. */
@SpringBootTest(properties = "labassist.lab-device.polling-enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LabAssistApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MessageIngestor messageIngestor;
    @Autowired
    private LabReportRepository reportRepository;

    @MockitoBean
    private OllamaClient ollamaClient;

    @BeforeEach
    void clean() {
        reportRepository.deleteAll();
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private UUID seedReport(String externalId, String name) {
        Map<String, Object> patient = Map.of("name", name, "mrn", "MRN-2002", "age", 60, "sex", "F");
        Map<String, Object> test = Map.of("code", "K", "name", "Potassium", "value", 4.2, "unit", "mmol/L");
        Map<String, Object> message = Map.of(
                "externalId", externalId, "deviceId", "ANALYZER-TEST", "patient", patient,
                "sampleCollectedAt", "2026-06-06T08:00:00Z", "tests", List.of(test));
        JsonNode node = objectMapper.valueToTree(message);
        messageIngestor.ingest(node);
        return reportRepository.findAll().get(0).getId();
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/lab-reports")).andExpect(status().isUnauthorized());
    }

    @Test
    void badCredentialsAreRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"doctor\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void doctorCanListAndViewReportsWithDecryptedPii() throws Exception {
        UUID id = seedReport("API-1", "Ada Yilmaz");
        String token = login("doctor", "Doctor123!");

        mockMvc.perform(get("/api/lab-reports").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/lab-reports/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Ada Yilmaz"))
                .andExpect(jsonPath("$.tests[0].flag").value("NORMAL"));
    }

    @Test
    void doctorIsForbiddenFromAuditLog() throws Exception {
        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + login("doctor", "Doctor123!")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAuditLog() throws Exception {
        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + login("admin", "Admin123!")))
                .andExpect(status().isOk());
    }

    @Test
    void interpretationEndpointReturnsLlmOutput() throws Exception {
        when(ollamaClient.chat(any(), any())).thenReturn("Mock preliminary interpretation.");
        UUID id = seedReport("API-LLM", "Test Patient");
        String token = login("doctor", "Doctor123!");

        mockMvc.perform(post("/api/lab-reports/" + id + "/interpretation").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.responseText").value("Mock preliminary interpretation."));
    }
}
