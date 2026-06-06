package com.labassist.labresult.reference;

import com.labassist.common.domain.Sex;
import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * In-memory cache of the canonical reference ranges, with selection of the most
 * specific applicable range for a given analyte code, patient sex and age.
 */
@Component
public class ReferenceRangeCatalog {

    private final ReferenceRangeRepository repository;
    private volatile Map<String, List<ReferenceRange>> byCode = Map.of();

    public ReferenceRangeCatalog(ReferenceRangeRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public synchronized void reload() {
        this.byCode = repository.findAll().stream()
                .collect(Collectors.groupingBy(ReferenceRange::getCode));
    }

    /** Returns the most specific range applicable to the patient, if any. */
    public Optional<ReferenceRange> find(String code, Sex sex, Integer age) {
        return byCode.getOrDefault(code, List.of()).stream()
                .filter(range -> applicable(range, sex, age))
                .max(Comparator.comparingInt(ReferenceRangeCatalog::specificity));
    }

    private static boolean applicable(ReferenceRange range, Sex sex, Integer age) {
        if (range.getSex() != null && range.getSex() != sex) {
            return false;
        }
        if (range.getAgeMin() != null && (age == null || age < range.getAgeMin())) {
            return false;
        }
        return range.getAgeMax() == null || (age != null && age <= range.getAgeMax());
    }

    /** Sex-specific ranges beat sex-agnostic; age-bounded beats unbounded. */
    private static int specificity(ReferenceRange range) {
        int score = 0;
        if (range.getSex() != null) {
            score += 2;
        }
        if (range.getAgeMin() != null || range.getAgeMax() != null) {
            score += 1;
        }
        return score;
    }
}
