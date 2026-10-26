package com.rohith.javaprojects.airbnb.advice;

import java.util.Map;

public record ApiError(
        int status,
        String message,
        Map<String, String> fieldErrors
) {
}
