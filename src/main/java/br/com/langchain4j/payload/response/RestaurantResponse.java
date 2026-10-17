package br.com.langchain4j.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {

    private Long id;
    private String name;
    private Integer countryCode;
    private String city;
    private String address;
    private String locality;
    private String localityVerbose;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String cuisines;
    private BigDecimal averageCostForTwo;
    private String currency;
    private Boolean hasTableBooking;
    private Boolean hasOnlineDelivery;
    private Boolean isDeliveringNow;
    private Boolean switchToOrderMenu;
    private Integer priceRange;
    private BigDecimal aggregateRating;
    private String ratingColor;
    private String ratingText;
    private Integer votes;
}
