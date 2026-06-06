package com.labassist.labresult.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.labassist.common.domain.Sex;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferenceRangeCatalogTest {

    @Mock
    private ReferenceRangeRepository repository;

    private ReferenceRangeCatalog catalog;

    private ReferenceRange range(String code, Sex sex, String low, String high) {
        ReferenceRange range = new ReferenceRange();
        range.setCode(code);
        range.setSex(sex);
        range.setLow(new BigDecimal(low));
        range.setHigh(new BigDecimal(high));
        return range;
    }

    @BeforeEach
    void setUp() {
        when(repository.findAll()).thenReturn(List.of(
                range("HGB", Sex.M, "13.5", "17.5"),
                range("HGB", Sex.F, "12.0", "15.5"),
                range("K", null, "3.5", "5.1")));
        catalog = new ReferenceRangeCatalog(repository);
        catalog.reload();
    }

    @Test
    void selectsSexSpecificRange() {
        assertThat(catalog.find("HGB", Sex.M, 40)).get()
                .extracting(ReferenceRange::getLow).isEqualTo(new BigDecimal("13.5"));
        assertThat(catalog.find("HGB", Sex.F, 40)).get()
                .extracting(ReferenceRange::getLow).isEqualTo(new BigDecimal("12.0"));
    }

    @Test
    void sexAgnosticRangeAppliesToAnyPatient() {
        assertThat(catalog.find("K", Sex.M, 70)).isPresent();
        assertThat(catalog.find("K", Sex.F, 20)).isPresent();
    }

    @Test
    void unknownCodeReturnsEmpty() {
        assertThat(catalog.find("XYZ", Sex.M, 40)).isEmpty();
    }
}
