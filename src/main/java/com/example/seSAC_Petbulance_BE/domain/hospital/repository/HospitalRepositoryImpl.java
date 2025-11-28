package com.example.seSAC_Petbulance_BE.domain.hospital.repository;

import com.example.seSAC_Petbulance_BE.domain.hospital.dto.response.HospitalMatchingResDto;
import com.example.seSAC_Petbulance_BE.domain.hospital.entity.QHospital;
import com.example.seSAC_Petbulance_BE.domain.hospitalWorktime.entity.QHospitalWorkTime;
import com.example.seSAC_Petbulance_BE.domain.treatmentAnimal.entity.QTreatmentAnimal;
import com.example.seSAC_Petbulance_BE.domain.treatmentAnimal.entity.TreatmentAnimal;
import com.example.seSAC_Petbulance_BE.global.common.type.AnimalType;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.querydsl.core.types.dsl.MathExpressions.*;

@RequiredArgsConstructor
@Repository
public class HospitalRepositoryImpl implements HospitalRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<HospitalMatchingResDto> findMatchingHospitals(
            String species,
            String filter,
            Double lat,
            Double lng,
            DayOfWeek today,
            LocalTime now
    ) {

        QHospital hospital = QHospital.hospital;
        QHospitalWorkTime work = QHospitalWorkTime.hospitalWorkTime;
        QTreatmentAnimal treat = QTreatmentAnimal.treatmentAnimal;

        NumberExpression<Double> distance = distanceExpression(lat, lng);

        // 종 필터 (Enum)
        BooleanExpression speciesFilter =
                treat.animaType.eq(AnimalType.valueOf(species));

        BooleanExpression todayFilter = work.id.dayOfWeek.eq(today.getValue());

        BooleanExpression isTwentyFour =
                work.openTime.eq(LocalTime.of(0, 0))
                        .and(work.closeTime.goe(LocalTime.of(23, 59)));

        BooleanExpression isOpenNow =
                work.openTime.loe(now)
                        .and(work.closeTime.goe(now))
                        .and(
                                work.breakStartTime.isNull()
                                        .or(
                                                Expressions.booleanTemplate(
                                                        "{0} not between {1} and {2}",
                                                        now, work.breakStartTime, work.breakEndTime
                                                )
                                        )
                        );

        BooleanExpression filterWhere =
                getFilterExpression(filter, todayFilter, isTwentyFour, isOpenNow);

        // 1) 일단 병원 기본 정보 가져오기
        List<HospitalMatchingResDto> result = queryFactory
                .select(
                        Projections.constructor(
                                HospitalMatchingResDto.class,
                                hospital.id,
                                hospital.image,
                                hospital.name,
                                isOpenNow,
                                distance,
                                work.closeTime,
                                hospital.phoneNumber
                        )
                )
                .from(hospital)
                .join(hospital.treatmentAnimals, treat)
                .join(hospital.hospitalWorktimes, work)
                .where(
                        speciesFilter,
                        filterWhere
                )
                .orderBy(distance.asc())
                .limit(3)
                .fetch();

        if (result.isEmpty()) return result;

        // 2) 조회된 병원 ID 리스트
        List<Long> hospitalIds = result.stream()
                .map(HospitalMatchingResDto::getHospitalId)
                .toList();

        // 3) 한 번의 쿼리로 모든 진료 가능 동물 조회
        List<TreatmentAnimal> animals = queryFactory
                .selectFrom(treat)
                .where(treat.hospital.id.in(hospitalIds))
                .fetch();

        // 4) 병원 ID -> 동물 description 리스트로 매핑
        Map<Long, List<String>> animalMap = animals.stream()
                .collect(
                        Collectors.groupingBy(
                                ta -> ta.getHospital().getId(),
                                Collectors.mapping(
                                        ta -> ta.getAnimaType().getDescription(),
                                        Collectors.toList()
                                )
                        )
                );

        // 5) 결과 DTO에 동물 리스트 주입
        result.forEach(res ->
                res.setTreatableAnimals(
                        animalMap.getOrDefault(res.getHospitalId(), new ArrayList<>())
                )
        );

        return result;
    }


    private BooleanExpression getFilterExpression(
            String filter,
            BooleanExpression today,
            BooleanExpression twentyFour,
            BooleanExpression openNow
    ) {
        return switch (filter) {
            case "DISTANCE" -> today;
            case "TWENTY_FOUR_HOUR" -> today.and(twentyFour);
            case "IS_OPEN_NOW" -> today.and(openNow);
            default -> throw new IllegalArgumentException("Invalid filter");
        };
    }

    private NumberExpression<Double> distanceExpression(Double lat, Double lng) {

        QHospital h = QHospital.hospital;

        return acos(
                cos(radians(Expressions.constant(lat)))
                        .multiply(cos(radians(h.lat)))
                        .multiply(
                                cos(
                                        radians(h.lng)
                                                .subtract(radians(Expressions.constant(lng)))
                                )
                        )
                        .add(
                                sin(radians(Expressions.constant(lat)))
                                        .multiply(sin(radians(h.lat)))
                        )
        ).multiply(6371);
    }
}
