package com.example.seSAC_Petbulance_BE.domain.hospital.repository;

import com.example.seSAC_Petbulance_BE.domain.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long>, HospitalRepositoryCustom {
}
