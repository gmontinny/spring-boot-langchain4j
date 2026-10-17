package br.com.langchain4j.service;

import br.com.langchain4j.model.Restaurant;
import br.com.langchain4j.repository.RestaurantRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvDataIngestionService {

    private final RestaurantRepository restaurantRepository;

    @Value("${csv.file-path:data/Zomato Restaurant Dataset.csv}")
    private String csvFilePath;

    @Value("${csv.auto-ingest-on-startup:true}")
    private boolean autoIngestOnStartup;

    @PostConstruct
    public void init() {
        if (autoIngestOnStartup) {
            ingestCsvData(csvFilePath);
        }
    }

    @Transactional
    public int ingestCsvData(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("Arquivo CSV não encontrado no caminho: {}. Verificando diretório raiz/data...", filePath);
            file = new File("data/Zomato Restaurant Dataset.csv");
            if (!file.exists()) {
                log.error("Arquivo CSV não localizado: {}", filePath);
                return 0;
            }
        }

        long count = restaurantRepository.count();
        if (count > 0) {
            log.info("Base de restaurantes já contém {} registros. Ingestão pulada.", count);
            return (int) count;
        }

        log.info("Iniciando ingestão do CSV: {}", file.getAbsolutePath());
        List<Restaurant> batchList = new ArrayList<>();
        int totalIngested = 0;

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1))
                .withSkipLines(1) // Pula o cabeçalho
                .build()) {

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 21) {
                    continue;
                }
                try {
                    Restaurant restaurant = parseRestaurant(line);
                    batchList.add(restaurant);

                    if (batchList.size() >= 500) {
                        restaurantRepository.saveAll(batchList);
                        totalIngested += batchList.size();
                        batchList.clear();
                        log.info("Ingeridos {} registros de restaurantes...", totalIngested);
                    }
                } catch (Exception e) {
                    log.debug("Erro ao parsear linha de restaurante: {}", e.getMessage());
                }
            }

            if (!batchList.isEmpty()) {
                restaurantRepository.saveAll(batchList);
                totalIngested += batchList.size();
            }

            log.info("Ingestão do CSV concluída com sucesso! Total de registros: {}", totalIngested);
        } catch (Exception e) {
            log.error("Erro durante o processamento do CSV: {}", e.getMessage(), e);
        }

        return totalIngested;
    }

    private Restaurant parseRestaurant(String[] line) {
        return Restaurant.builder()
                .id(parseLong(line[0]))
                .name(line[1])
                .countryCode(parseInt(line[2]))
                .city(line[3])
                .address(line[4])
                .locality(line[5])
                .localityVerbose(line[6])
                .longitude(parseBigDecimal(line[7]))
                .latitude(parseBigDecimal(line[8]))
                .cuisines(line[9])
                .averageCostForTwo(parseBigDecimal(line[10]))
                .currency(line[11])
                .hasTableBooking(parseBoolean(line[12]))
                .hasOnlineDelivery(parseBoolean(line[13]))
                .isDeliveringNow(parseBoolean(line[14]))
                .switchToOrderMenu(parseBoolean(line[15]))
                .priceRange(parseInt(line[16]))
                .aggregateRating(parseBigDecimal(line[17]))
                .ratingColor(line[18])
                .ratingText(line[19])
                .votes(parseInt(line[20]))
                .build();
    }

    private Long parseLong(String value) {
        try {
            return value != null && !value.isBlank() ? Long.parseLong(value.trim()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        try {
            return value != null && !value.isBlank() ? Integer.parseInt(value.trim()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return value != null && !value.isBlank() ? new BigDecimal(value.trim()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) return false;
        String val = value.trim().toLowerCase();
        return "yes".equals(val) || "true".equals(val) || "1".equals(val);
    }
}
