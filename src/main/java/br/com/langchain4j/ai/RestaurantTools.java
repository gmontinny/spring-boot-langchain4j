package br.com.langchain4j.ai;

import br.com.langchain4j.model.Restaurant;
import br.com.langchain4j.repository.RestaurantRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantTools {

    private final RestaurantRepository restaurantRepository;

    @Tool("Busca restaurantes por cidade ou culinária no banco de dados")
    public String searchRestaurants(String city, String cuisine) {
        log.info("Tool invocada: searchRestaurants city={}, cuisine={}", city, cuisine);
        var page = restaurantRepository.searchRestaurants(
                city != null && !city.isBlank() ? city : null,
                cuisine != null && !cuisine.isBlank() ? cuisine : null,
                null,
                PageRequest.of(0, 10)
        );

        if (page.isEmpty()) {
            return "Nenhum restaurante encontrado para a cidade: " + city + " e culinária: " + cuisine;
        }

        return page.getContent().stream()
                .map(this::formatRestaurantSummary)
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool("Retorna os top 10 restaurantes com melhores avaliações")
    public String getTopRatedRestaurants() {
        log.info("Tool invocada: getTopRatedRestaurants");
        List<Restaurant> topList = restaurantRepository.findTop10Rated();
        if (topList.isEmpty()) {
            return "Nenhum restaurante registrado no banco.";
        }
        return topList.stream()
                .map(this::formatRestaurantSummary)
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool("Retorna estatísticas gerais da base de restaurantes (total cadastrado)")
    public String getRestaurantStats() {
        long count = restaurantRepository.count();
        return "Total de restaurantes cadastrados na base de dados: " + count;
    }

    private String formatRestaurantSummary(Restaurant r) {
        return String.format("ID: %d | Nome: %s | Cidade: %s | Culinária: %s | Nota: %s (%s) | Custo Médio para 2: %s %s | Votos: %d",
                r.getId(),
                r.getName(),
                r.getCity(),
                r.getCuisines(),
                r.getAggregateRating() != null ? r.getAggregateRating().toString() : "N/A",
                r.getRatingText(),
                r.getAverageCostForTwo() != null ? r.getAverageCostForTwo().toString() : "N/A",
                r.getCurrency() != null ? r.getCurrency() : "",
                r.getVotes() != null ? r.getVotes() : 0);
    }
}
