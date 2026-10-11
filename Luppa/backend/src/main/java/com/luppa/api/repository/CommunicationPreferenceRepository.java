package com.luppa.api.repository;

import com.luppa.api.entity.CommunicationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunicationPreferenceRepository extends JpaRepository<CommunicationPreference, Integer> {
}
