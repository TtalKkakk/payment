package com.example.pg.payment.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentId")
class PaymentIdTest {

    private static final String ID_VALUE = "payment-uuid-123";

    @Nested
    @DisplayName("generate")
    class Generate {

        @Test
        @DisplayName("호출 시마다 서로 다른 UUID 기반 값 생성")
        void createsUniqueValue() {
            PaymentId id1 = PaymentId.generate();
            PaymentId id2 = PaymentId.generate();

            assertThat(id1.getValue()).isNotBlank();
            assertThat(id2.getValue()).isNotBlank();
            assertThat(id1.getValue()).isNotEqualTo(id2.getValue());
        }
    }

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("문자열로 인스턴스 생성")
        void createsFromString() {
            PaymentId id = PaymentId.from(ID_VALUE);
            assertThat(id.getValue()).isEqualTo(ID_VALUE);
        }
    }

    @Nested
    @DisplayName("equals / hashCode")
    class EqualsHashCode {

        @Test
        @DisplayName("같은 value면 동등")
        void sameValueEquals() {
            PaymentId id1 = PaymentId.from(ID_VALUE);
            PaymentId id2 = PaymentId.from(ID_VALUE);
            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("다른 value면 비동등")
        void differentValueNotEquals() {
            PaymentId id1 = PaymentId.from("a");
            PaymentId id2 = PaymentId.from("b");
            assertThat(id1).isNotEqualTo(id2);
        }
    }
}
