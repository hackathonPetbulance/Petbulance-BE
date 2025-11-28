package com.example.seSAC_Petbulance_BE.domain.hospital.repository;

import com.example.seSAC_Petbulance_BE.domain.hospital.dto.response.DetailHospitalResDto;
import com.example.seSAC_Petbulance_BE.domain.hospital.dto.response.HospitalMatchingResDto;
import com.example.seSAC_Petbulance_BE.domain.hospital.entity.QHospital;
import com.example.seSAC_Petbulance_BE.domain.hospitalWorktime.entity.HospitalWorkTime;
import com.example.seSAC_Petbulance_BE.domain.hospitalWorktime.entity.QHospitalWorkTime;
import com.example.seSAC_Petbulance_BE.domain.treatmentAnimal.entity.QTreatmentAnimal;
import com.example.seSAC_Petbulance_BE.domain.treatmentAnimal.entity.TreatmentAnimal;
import com.example.seSAC_Petbulance_BE.global.common.type.AnimalType;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Override
    public DetailHospitalResDto findHospitalDetail(Long hospitalId, Double userLat, Double userLng) {

        QHospital h = QHospital.hospital;
        QHospitalWorkTime w = QHospitalWorkTime.hospitalWorkTime;
        QTreatmentAnimal t = QTreatmentAnimal.treatmentAnimal;

        NumberExpression<Double> distanceExp = distanceExpression(userLat, userLng);

        // ======================
        // 1) 병원 기본 정보 조회
        // ======================
        Tuple base = queryFactory
                .select(
                        h.id,
                        h.name,
                        h.phoneNumber,
                        h.address,
                        h.lat,
                        h.lng,
                        distanceExp
                )
                .from(h)
                .where(h.id.eq(hospitalId))
                .fetchOne();

        if (base == null) return null;

        Long id = base.get(h.id);
        String name = base.get(h.name);
        String phone = base.get(h.phoneNumber);
        String address = base.get(h.address);
        Double lat = base.get(h.lat);
        Double lng = base.get(h.lng);
        Double distance = base.get(distanceExp);

        // ======================
        // 2) 치료 가능 동물 조회
        // ======================
        List<String> acceptedAnimals = queryFactory
                .select(t.animaType)
                .from(t)
                .where(t.hospital.id.eq(hospitalId))
                .fetch()
                .stream()
                .map(AnimalType::getDescription)
                .collect(Collectors.toList());

        // ======================
        // 3) 요일별 worktime 조회
        // ======================
        List<HospitalWorkTime> weekly = queryFactory
                .select(w)
                .from(w)
                .where(w.hospital.id.eq(hospitalId))
                .fetch();

        // 오늘 요일
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now = LocalTime.now();

        HospitalWorkTime todayWork = weekly.stream()
                .filter(x -> x.getId().getDayOfWeek() == today.getValue())
                .findFirst()
                .orElse(null);

        boolean openNow = false;
        LocalTime todayCloseTime = null;

        if (todayWork != null && Boolean.TRUE.equals(todayWork.getIsOpen())) {

            boolean inBusinessHours =
                    !now.isBefore(todayWork.getOpenTime()) &&
                            !now.isAfter(todayWork.getCloseTime());

            boolean inBreak = false;
            if (todayWork.getBreakStartTime() != null && todayWork.getBreakEndTime() != null) {
                inBreak =
                        !now.isBefore(todayWork.getBreakStartTime()) &&
                                !now.isAfter(todayWork.getBreakEndTime());
            }

            openNow = inBusinessHours && !inBreak;
            todayCloseTime = todayWork.getCloseTime();
        }

        // ======================
        // 4) openHours 변환
        // ======================
        List<DetailHospitalResDto.OpenHour> openHours =
                weekly.stream()
                        .map(work -> {
                            String hours;
                            if (!Boolean.TRUE.equals(work.getIsOpen())) {
                                hours = "휴진";
                            } else {
                                hours = work.getOpenTime() + "-" + work.getCloseTime();
                            }

                            return new DetailHospitalResDto.OpenHour(
                                    convertDay(work.getId().getDayOfWeek()),
                                    hours
                            );
                        })
                        .collect(Collectors.toList());

        // ======================
        // 5) DTO 빌드
        // ======================
        return DetailHospitalResDto.builder()
                .hospitalId(id)
                .name(name)
                .phone(phone)
                .reviewAvg(4.8)       // 임시 상수값
                .reviewCount(234)     // 임시 상수값
                .openNow(openNow)
                .todayCloseTime(todayCloseTime)
                .distanceKm(distance)
                .acceptedAnimals(acceptedAnimals)
                .location(new DetailHospitalResDto.Location(
                        address,
                        lat,
                        lng
                ))
                .openHours(openHours)
                .build();
    }

    private String convertDay(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "MON";
            case 2 -> "TUE";
            case 3 -> "WED";
            case 4 -> "THU";
            case 5 -> "FRI";
            case 6 -> "SAT";
            case 7 -> "SUN";
            default -> "UNKNOWN";
        };
    }

}
