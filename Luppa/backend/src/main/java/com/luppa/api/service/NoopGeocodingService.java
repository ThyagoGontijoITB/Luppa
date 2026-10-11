package com.luppa.api.service;

import org.springframework.stereotype.Service;

@Service
public class NoopGeocodingService implements GeocodingService {
    @Override
    public Coordinates geocode(String address) {
        return null;
    }
}
