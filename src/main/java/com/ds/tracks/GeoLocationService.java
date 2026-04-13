package com.ds.tracks;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class GeoLocationService {

    private final WebClient webClient = WebClient.create();

    public double[] getLatLng(String ip) {
        // Skip for localhost
        if (ip == null || ip.equals("127.0.0.1") || ip.equals("::1") || ip.startsWith("192.168") || ip.startsWith("10.")) {
            return new double[]{0.0, 0.0};
        }

        try {
            // Free API — no key needed (rate limit: 45 req/min)
            String url = "http://ip-api.com/json/" + ip + "?fields=lat,lon,status";

            Map response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && "success".equals(response.get("status"))) {
                double lat = ((Number) response.get("lat")).doubleValue();
                double lon = ((Number) response.get("lon")).doubleValue();
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            // log if needed
        }

        return new double[]{0.0, 0.0};
    }
}