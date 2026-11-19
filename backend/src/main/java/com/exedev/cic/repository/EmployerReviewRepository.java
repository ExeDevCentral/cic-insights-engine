package com.exedev.cic.repository;

import com.exedev.cic.domain.EmployerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmployerReviewRepository extends JpaRepository<EmployerReview, UUID> {

    Page<EmployerReview> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);
}
