package ru.practicum.main.util;

import lombok.experimental.UtilityClass;

import java.time.format.DateTimeFormatter;

@UtilityClass
public final class EwmConstants {
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    public static final int MIN_TITLE_LENGTH = 3;
    public static final int MAX_TITLE_LENGTH = 120;

    public static final int MIN_ANNOTATION_LENGTH = 20;
    public static final int MAX_ANNOT_LENGTH = 2000;

    public static final int MIN_DESCRIPTION_LENGTH = 20;
    public static final int MAX_DESC_LENGTH = 7000;

    public static final int MAX_REVIEW_COMMENT_LENGTH = 200;
    public static final int MIN_REVIEW_SCORE = 1;
    public static final int MAX_REVIEW_SCORE = 5;

    public static final int HOURS_OFFSET_FOR_REVIEWS_CREATING = 2;
}
