package br.com.langchain4j.repository;

import br.com.langchain4j.model.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Page<Restaurant> findByCityIgnoreCase(String city, Pageable pageable);

    Page<Restaurant> findByCuisinesContainingIgnoreCase(String cuisines, Pageable pageable);

    @Query("SELECT r FROM Restaurant r WHERE " +
           "(cast(:city as string) IS NULL OR LOWER(r.city) LIKE LOWER(CONCAT('%', cast(:city as string), '%'))) AND " +
           "(cast(:cuisine as string) IS NULL OR LOWER(r.cuisines) LIKE LOWER(CONCAT('%', cast(:cuisine as string), '%'))) AND " +
           "(cast(:name as string) IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', cast(:name as string), '%')))")
    Page<Restaurant> searchRestaurants(
            @Param("city") String city,
            @Param("cuisine") String cuisine,
            @Param("name") String name,
            Pageable pageable
    );

    @Query("SELECT r FROM Restaurant r ORDER BY r.aggregateRating DESC LIMIT 10")
    List<Restaurant> findTop10Rated();
}
