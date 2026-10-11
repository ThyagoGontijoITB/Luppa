package com.luppa.api.repository;

import com.luppa.api.entity.LegalAcceptance;
import com.luppa.api.entity.LegalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalAcceptanceRepository extends JpaRepository<LegalAcceptance, Long> {
    boolean existsByUserIdAndDocumentType(Integer userId, LegalDocumentType documentType);
}
