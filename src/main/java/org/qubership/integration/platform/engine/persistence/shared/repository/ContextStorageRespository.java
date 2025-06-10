package org.qubership.integration.platform.engine.persistence.shared.repository;

import org.qubership.integration.platform.engine.persistence.shared.entity.ContextStorage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContextStorageRespository extends JpaRepository<ContextStorage, String> {
    // Define any additional query methods if needed
}
