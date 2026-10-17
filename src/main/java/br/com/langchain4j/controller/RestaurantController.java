package br.com.langchain4j.controller;

import br.com.langchain4j.model.Restaurant;
import br.com.langchain4j.payload.request.RestaurantRequest;
import br.com.langchain4j.payload.response.MessageResponse;
import br.com.langchain4j.payload.response.RestaurantResponse;
import br.com.langchain4j.repository.RestaurantRepository;
import br.com.langchain4j.service.CsvDataIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Restaurantes", description = "Endpoints para consulta, cadastro, atualização, remoção e ingestão de restaurantes")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantRepository restaurantRepository;
    private final CsvDataIngestionService csvDataIngestionService;

    @Operation(summary = "Listar e filtrar restaurantes", description = "Retorna uma página de restaurantes com filtros opcionais por cidade, culinária e nome, além de ordenação e paginação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado / Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "429", description = "Muitas requisições (Rate Limit atingido)")
    })
    @GetMapping
    public ResponseEntity<Page<RestaurantResponse>> getAllRestaurants(
            @Parameter(description = "Filtrar por cidade (case-insensitive)") @RequestParam(required = false) String city,
            @Parameter(description = "Filtrar por tipo de culinária (case-insensitive)") @RequestParam(required = false) String cuisine,
            @Parameter(description = "Filtrar por nome do restaurante (case-insensitive)") @RequestParam(required = false) String name,
            @Parameter(description = "Número da página (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de registros por página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo para ordenação") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Direção da ordenação (asc ou desc)") @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Restaurant> restaurantsPage = restaurantRepository.searchRestaurants(city, cuisine, name, pageable);
        Page<RestaurantResponse> responsePage = restaurantsPage.map(this::mapToResponse);

        return ResponseEntity.ok(responsePage);
    }

    @Operation(summary = "Obter restaurante por ID", description = "Busca detalhes de um restaurante específico pelo seu identificador único.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Restaurante encontrado"),
            @ApiResponse(responseCode = "404", description = "Restaurante não encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autorizado / Token JWT ausente ou inválido")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getRestaurantById(
            @Parameter(description = "ID do restaurante", required = true) @PathVariable Long id) {
        return restaurantRepository.findById(id)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Criar novo restaurante", description = "Cadastra manualmente um novo restaurante. Requer perfil ROLE_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Restaurante criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer ROLE_ADMIN)")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantResponse> createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        Restaurant restaurant = mapToEntity(request);
        Restaurant saved = restaurantRepository.save(restaurant);
        return new ResponseEntity<>(mapToResponse(saved), HttpStatus.CREATED);
    }

    @Operation(summary = "Atualizar restaurante existente", description = "Atualiza todas as informações de um restaurante existente pelo seu ID. Requer perfil ROLE_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Restaurante atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Restaurante não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer ROLE_ADMIN)")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRestaurant(
            @Parameter(description = "ID do restaurante", required = true) @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest request) {
        return restaurantRepository.findById(id).map(existing -> {
            existing.setName(request.getName());
            existing.setCountryCode(request.getCountryCode());
            existing.setCity(request.getCity());
            existing.setAddress(request.getAddress());
            existing.setLocality(request.getLocality());
            existing.setLocalityVerbose(request.getLocalityVerbose());
            existing.setLongitude(request.getLongitude());
            existing.setLatitude(request.getLatitude());
            existing.setCuisines(request.getCuisines());
            existing.setAverageCostForTwo(request.getAverageCostForTwo());
            existing.setCurrency(request.getCurrency());
            existing.setHasTableBooking(request.getHasTableBooking());
            existing.setHasOnlineDelivery(request.getHasOnlineDelivery());
            existing.setIsDeliveringNow(request.getIsDeliveringNow());
            existing.setSwitchToOrderMenu(request.getSwitchToOrderMenu());
            existing.setPriceRange(request.getPriceRange());
            existing.setAggregateRating(request.getAggregateRating());
            existing.setRatingColor(request.getRatingColor());
            existing.setRatingText(request.getRatingText());
            existing.setVotes(request.getVotes());

            Restaurant saved = restaurantRepository.save(existing);
            return ResponseEntity.ok(mapToResponse(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Remover restaurante por ID", description = "Deleta um restaurante existente pelo seu ID. Requer perfil ROLE_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Restaurante deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Restaurante não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer ROLE_ADMIN)")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRestaurant(
            @Parameter(description = "ID do restaurante", required = true) @PathVariable Long id) {
        if (!restaurantRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        restaurantRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Restaurante deletado com sucesso!"));
    }

    @Operation(summary = "Disparar ingestão do arquivo CSV Zomato", description = "Lê o arquivo CSV informado e faz o upsert dos registros na tabela de restaurantes do PostgreSQL. Requer perfil ROLE_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ingestão executada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer ROLE_ADMIN)")
    })
    @PostMapping("/ingest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerIngest(
            @Parameter(description = "Caminho do arquivo CSV relativo à raiz do projeto")
            @RequestParam(defaultValue = "data/Zomato Restaurant Dataset.csv") String filePath) {
        int count = csvDataIngestionService.ingestCsvData(filePath);
        return ResponseEntity.ok(new MessageResponse("Ingestão processada. Total de registros na base/ingeridos: " + count));
    }

    private RestaurantResponse mapToResponse(Restaurant r) {
        return RestaurantResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .countryCode(r.getCountryCode())
                .city(r.getCity())
                .address(r.getAddress())
                .locality(r.getLocality())
                .localityVerbose(r.getLocalityVerbose())
                .longitude(r.getLongitude())
                .latitude(r.getLatitude())
                .cuisines(r.getCuisines())
                .averageCostForTwo(r.getAverageCostForTwo())
                .currency(r.getCurrency())
                .hasTableBooking(r.getHasTableBooking())
                .hasOnlineDelivery(r.getHasOnlineDelivery())
                .isDeliveringNow(r.getIsDeliveringNow())
                .switchToOrderMenu(r.getSwitchToOrderMenu())
                .priceRange(r.getPriceRange())
                .aggregateRating(r.getAggregateRating())
                .ratingColor(r.getRatingColor())
                .ratingText(r.getRatingText())
                .votes(r.getVotes())
                .build();
    }

    private Restaurant mapToEntity(RestaurantRequest r) {
        return Restaurant.builder()
                .id(r.getId())
                .name(r.getName())
                .countryCode(r.getCountryCode())
                .city(r.getCity())
                .address(r.getAddress())
                .locality(r.getLocality())
                .localityVerbose(r.getLocalityVerbose())
                .longitude(r.getLongitude())
                .latitude(r.getLatitude())
                .cuisines(r.getCuisines())
                .averageCostForTwo(r.getAverageCostForTwo())
                .currency(r.getCurrency())
                .hasTableBooking(r.getHasTableBooking())
                .hasOnlineDelivery(r.getHasOnlineDelivery())
                .isDeliveringNow(r.getIsDeliveringNow())
                .switchToOrderMenu(r.getSwitchToOrderMenu())
                .priceRange(r.getPriceRange())
                .aggregateRating(r.getAggregateRating())
                .ratingColor(r.getRatingColor())
                .ratingText(r.getRatingText())
                .votes(r.getVotes())
                .build();
    }
}
