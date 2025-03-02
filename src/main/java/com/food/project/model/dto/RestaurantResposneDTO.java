package com.food.project.model.dto;

import com.food.project.model.Restaurant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantResposneDTO {
    private Long id;
    private String name;
    private String description;

    public RestaurantResposneDTO(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public RestaurantResposneDTO(Restaurant r) {
        this.id = r.getId();
        this.name = r.getName();
        this.description = r.getDescription();
    }
}
