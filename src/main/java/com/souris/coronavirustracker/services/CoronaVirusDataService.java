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
    private final String provinceHeader = "Province/State";
    private final String countryHeader = "Country/Region";
    private HttpClient client;
    private List<LocationStats> allStats = new ArrayList<>();

    public CoronaVirusDataService(SSLContext sslContext) {
        this.client = HttpClient.newBuilder().sslContext(sslContext).build();
    }

    @PostConstruct
    @Scheduled(cron = "* * 1 * * *")
    public void fetchVirusData() {
        try {
            String virusData = downloadVirusData();
            Iterable<CSVRecord> records = convertDataToCsv(virusData);
            populateListData(records);
        } catch (NullPointerException | NumberFormatException | InterruptedException | IOException e) {
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
    private String downloadVirusData() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VIRUS_DATA_URL))
                .build();
        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

        return httpResponse.body();
    }

    /**
     *
     * @param virusData
     * @return
     * @throws IOException
     */
    private Iterable<CSVRecord> convertDataToCsv(@NonNull String virusData) throws IOException {
        StringReader csvBodyReader = new StringReader(virusData);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvBodyReader);

        return records;
    }

    /**
     *
     * @param records
     * @throws NumberFormatException
     */
    private void populateListData(Iterable<CSVRecord> records) throws NumberFormatException {
        List<LocationStats> tmpAllStats = new ArrayList<>();
        for (CSVRecord record : records) {
            int lastEntry = record.size() - 1;
            int secondToLastEntry = record.size() - 2;

            String state = record.get(provinceHeader);
            String country = record.get(countryHeader);
            
            int total = record.get(lastEntry) == null ? 0 : Integer.parseInt(record.get(lastEntry));
            int preTotal = record.get(lastEntry) == null ? 0 : Integer.parseInt(record.get(secondToLastEntry));
            LocationStats locationStat = LocationStats.builder()
                    .state(state)
                    .country(country)
                    .latestTotalCases(total)
                    .diffFromPreDay(total - preTotal)
                    .build();
            tmpAllStats.add(locationStat);
        }
        allStats = tmpAllStats;
    }

}

