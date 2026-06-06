package com.labassist.labresult;

import com.labassist.labresult.domain.LabReport;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LabReportRepository
        extends JpaRepository<LabReport, UUID>, JpaSpecificationExecutor<LabReport> {

    boolean existsByExternalId(String externalId);

    /** Detail fetch that eagerly loads the analytes to avoid N+1 on the detail view. */
    @EntityGraph(attributePaths = "tests")
    Optional<LabReport> findWithTestsById(UUID id);
}
