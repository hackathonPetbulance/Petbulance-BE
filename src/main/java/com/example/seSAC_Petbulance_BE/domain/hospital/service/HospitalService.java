package com.example.seSAC_Petbulance_BE.domain.hospital.service;

import com.example.seSAC_Petbulance_BE.domain.hospital.dto.response.DetailHospitalResDto;
import com.example.seSAC_Petbulance_BE.domain.hospital.dto.response.HospitalMatchingResDto;
import com.example.seSAC_Petbulance_BE.domain.hospital.entity.Hospital;
import com.example.seSAC_Petbulance_BE.domain.hospital.repository.HospitalRepository;
import com.example.seSAC_Petbulance_BE.global.common.error.exception.CustomException;
import com.example.seSAC_Petbulance_BE.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    /*GET /matching?filter=IS_OPEN_NOW&species=RABBIT&userLat=37.5665&userLng=126.9780
     */
    public List<HospitalMatchingResDto> hospitalMatching(String filter, String species, Double lat, Double lng) {

        LocalDate today = LocalDate.now(); // 현재 날짜
        LocalTime now = LocalTime.now(); // 현재 시간

        return hospitalRepository.findMatchingHospitals(
                species,
                filter,
                lat,
                lng,
                today.getDayOfWeek(), // 현재 요일
                now // 현재 시간
        );
    }

    public DetailHospitalResDto detailHospital(Long hospitalId, Double lat, Double lng) {
        return hospitalRepository.findHospitalDetail(hospitalId, lat, lng);

    }
}
