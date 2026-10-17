package br.com.langchain4j.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "country_code")
    private Integer countryCode;

    private String city;

    @Column(length = 500)
    private String address;

    private String locality;

    @Column(name = "locality_verbose", length = 500)
    private String localityVerbose;

    @Column(precision = 12, scale = 8)
    private BigDecimal longitude;

    @Column(precision = 12, scale = 8)
    private BigDecimal latitude;

    @Column(length = 500)
    private String cuisines;

    @Column(name = "average_cost_for_two", precision = 10, scale = 2)
    private BigDecimal averageCostForTwo;

    private String currency;

    @Column(name = "has_table_booking")
    private Boolean hasTableBooking;

    @Column(name = "has_online_delivery")
    private Boolean hasOnlineDelivery;

    @Column(name = "is_delivering_now")
    private Boolean isDeliveringNow;

    @Column(name = "switch_to_order_menu")
    private Boolean switchToOrderMenu;

    @Column(name = "price_range")
    private Integer priceRange;

    @Column(name = "aggregate_rating", precision = 3, scale = 1)
    private BigDecimal aggregateRating;

    @Column(name = "rating_color")
    private String ratingColor;

    @Column(name = "rating_text")
    private String ratingText;

    private Integer votes;
}
