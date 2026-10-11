package com.luppa.api.service;

import java.math.BigDecimal;

public interface GeocodingService {
    Coordinates geocode(String address);

    record Coordinates(BigDecimal latitude, BigDecimal longitude) {
    }
}
