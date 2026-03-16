package com.example.pg.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 날짜/시간 문자열 파싱 유틸.
 */
public final class DateTimeParseUtil {

    private static final DateTimeFormatter[] ISO_FORMATTERS = {
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private DateTimeParseUtil() {}

    /**
     * ISO 형식 문자열을 LocalDateTime으로 파싱한다.
     * ISO_DATE_TIME, ISO_LOCAL_DATE_TIME 순으로 시도하며, 모두 실패하면 null을 반환한다.
     */
    public static LocalDateTime parseIsoOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : ISO_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // next formatter
            }
        }
        return null;
    }
}
