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

    /** Ingests a malformed message (no externalId) → stored as REJECTED. */
    private void seedRejected() {
        Map<String, Object> patient = Map.of("name", "X", "mrn", "Y", "age", 50, "sex", "M");
        Map<String, Object> test = Map.of("code", "K", "name", "Potassium", "value", 4.0, "unit", "mmol/L");
        Map<String, Object> message = Map.of("deviceId", "ANALYZER-TEST", "patient", patient,
                "sampleCollectedAt", "2026-06-06T08:00:00Z", "tests", List.of(test));
        messageIngestor.ingest(objectMapper.valueToTree(message));
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
    void doctorListExcludesRejectedButAdminSeesIt() throws Exception {
        seedReport("OK-1", "Valid Patient"); // VALIDATED
        seedRejected();                      // REJECTED

        mockMvc.perform(get("/api/lab-reports").header("Authorization", "Bearer " + login("doctor", "Doctor123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/lab-reports").header("Authorization", "Bearer " + login("admin", "Admin123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void summaryReturnsScopedCounts() throws Exception {
        seedReport("S-1", "Patient");
        mockMvc.perform(get("/api/lab-reports/summary").header("Authorization", "Bearer " + login("doctor", "Doctor123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void adminCanCreateUserButDoctorCannot() throws Exception {
        String newUser = "{\"username\":\"nurse1\",\"password\":\"Password123\",\"displayName\":\"Nurse\",\"role\":\"DOCTOR\"}";

        mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + login("doctor", "Doctor123!"))
                        .contentType(MediaType.APPLICATION_JSON).content(newUser))
                .andExpect(status().isForbidden());

        String adminToken = login("admin", "Admin123!");
        mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(newUser))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nurse1"))
                .andExpect(jsonPath("$.role").value("DOCTOR"));

        // Duplicate username -> 409
        mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(newUser))
                .andExpect(status().isConflict());
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
