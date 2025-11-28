package com.example.seSAC_Petbulance_BE.domain.hospital.controller;

import com.example.seSAC_Petbulance_BE.domain.hospital.dto.response.HospitalMatchingResDto;
import com.example.seSAC_Petbulance_BE.domain.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hospitals")
@RequiredArgsConstructor
public class HospitalController {
    private final HospitalService hospitalService;

    @GetMapping("/matching")
    public List<HospitalMatchingResDto> hospitalMatching(
            @RequestParam String filter,
            @RequestParam String species,
            @RequestParam Double lat,
            @RequestParam Double lng
    ) {
        return hospitalService.hospitalMatching(filter, species, lat, lng);
    }


}
