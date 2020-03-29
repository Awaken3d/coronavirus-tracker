package com.souris.coronavirustracker.models;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class LocationStats {
    @NonNull
    private String state;
    @NonNull
    private String country;
    @NonNull
    private int latestTotalCases;
    @NonNull
    private int diffFromPreDay;
}
