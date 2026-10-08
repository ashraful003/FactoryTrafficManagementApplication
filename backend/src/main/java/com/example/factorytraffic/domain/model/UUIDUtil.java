package com.example.factorytraffic.domain.model;

import java.util.UUID;

public final class UUIDUtil {
    private UUIDUtil() {}

    public static String randomId() {
        return UUID.randomUUID().toString();
    }
}
