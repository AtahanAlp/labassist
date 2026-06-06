package com.labassist.labresult.reference;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenceRangeRepository extends JpaRepository<ReferenceRange, UUID> {

    List<ReferenceRange> findByCode(String code);
}
