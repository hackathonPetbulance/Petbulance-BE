package com.example.seSAC_Petbulance_BE.domain.hospitalWorktime.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HospitalWorkTimeKey implements Serializable {

    private Long hospitalId;

    private Integer dayOfWeek;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HospitalWorkTimeKey)) return false;
        HospitalWorkTimeKey that = (HospitalWorkTimeKey) o;
        return Objects.equals(hospitalId, that.hospitalId) &&
                Objects.equals(dayOfWeek, that.dayOfWeek);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hospitalId, dayOfWeek);
    }
}
