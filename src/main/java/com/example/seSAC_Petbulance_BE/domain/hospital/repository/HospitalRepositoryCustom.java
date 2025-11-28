package com.example.seSAC_Petbulance_BE.domain.hospital.repository;

import com.example.seSAC_Petbulance_BE.domain.hospital.dto.response.DetailHospitalResDto;
import com.example.seSAC_Petbulance_BE.domain.hospital.dto.response.HospitalMatchingResDto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface HospitalRepositoryCustom {
    List<HospitalMatchingResDto> findMatchingHospitals(
            String species,
            String filter,
            Double lat,
            Double lng,
            DayOfWeek today,
            LocalTime now
    );
    DetailHospitalResDto findHospitalDetail(Long hospitalId, Double lat, Double lng);

}
