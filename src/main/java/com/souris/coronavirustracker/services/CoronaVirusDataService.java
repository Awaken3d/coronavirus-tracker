package com.souris.coronavirustracker.services;

import com.souris.coronavirustracker.models.LocationStats;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Log
@Service
@Getter
public class CoronaVirusDataService {

    @Value("${corona.url}")
    private String VIRUS_DATA_URL;

    private HttpClient client;
    private List<LocationStats> allStats = new ArrayList<>();

    public CoronaVirusDataService(SSLContext sslContext) {
        this.client = HttpClient.newBuilder().sslContext(sslContext).build();
    }

    @PostConstruct
    @Scheduled(cron = "* * 1 * * *")
    public void fetchVirusData() {
        try {
            HttpResponse<String> httpResponse = getVirusData();
            Iterable<CSVRecord> records = getCsvRecords(httpResponse);
            allStats.clear();
            populateData(records);
        } catch (NullPointerException | InterruptedException | IOException e) {
            log.severe("could not parse data, will not update: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private HttpResponse<String> getVirusData() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VIRUS_DATA_URL))
                .build();
        HttpResponse<String> httpResponse = null;
        httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

        return httpResponse;
    }

    /**
     *
     * @param httpResponse
     * @return
     * @throws IOException
     */
    private Iterable<CSVRecord> getCsvRecords(@NonNull HttpResponse<String> httpResponse) throws IOException {
        StringReader csvBodyReader = new StringReader(httpResponse.body());
        Iterable<CSVRecord> records = null;
        records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvBodyReader);

        return records;
    }

    /**
     *
     * @param records
     */
    private void populateData(Iterable<CSVRecord> records) {
        for (CSVRecord record : records) {
            String state = record.get("Province/State");
            String country = record.get("Country/Region");
            int total = record.get(record.size() - 1) == null ? 0 : Integer.parseInt(record.get(record.size() - 1));
            int preTotal = record.get(record.size() - 1) == null ? 0 : Integer.parseInt(record.get(record.size() - 2));
            LocationStats locationStat = LocationStats.builder()
                    .state(state)
                    .country(country)
                    .latestTotalCases(total)
                    .diffFromPreDay(total - preTotal)
                    .build();
            allStats.add(locationStat);
        }
    }

}

