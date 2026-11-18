package com.bookngo.authservice;

import com.bookngo.authservice.model.AuditLog;
import com.bookngo.authservice.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private List<AuditLog> saved;

    @BeforeEach
    void setUp() {
        saved = auditLogRepository.saveAll(List.of(
                new AuditLog("TEST_OLDER", "admin@test.com", "User", 1L, "older entry", LocalDateTime.of(2099, 1, 1, 10, 0)),
                new AuditLog("TEST_NEWER", "admin@test.com", "User", 2L, "newer entry", LocalDateTime.of(2099, 1, 2, 10, 0))));
    }

    @AfterEach
    void cleanUp() {
        auditLogRepository.deleteAll(saved);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminGetsAuditLogsNewestFirst() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs").param("page", "0").param("size", "2").param("sort", "timestamp,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].action").value("TEST_NEWER"))
                .andExpect(jsonPath("$.content[0].performedBy").value("admin@test.com"))
                .andExpect(jsonPath("$.content[0].targetEntity").value("User"))
                .andExpect(jsonPath("$.content[0].targetId").value(2))
                .andExpect(jsonPath("$.content[0].details").value("newer entry"))
                .andExpect(jsonPath("$.content[0].timestamp").exists())
                .andExpect(jsonPath("$.content[1].action").value("TEST_OLDER"))
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }
}
