package com.yamiapp.model.dto;

import com.yamiapp.model.Restaurant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantResposneDTO {
    private Long id;
    private String name;
    private String shortName;
    private String description;

    public RestaurantResposneDTO(Long id, String name, String shortName, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public RestaurantResposneDTO(Restaurant r) {
        this.id = r.getId();
        this.name = r.getName();
        this.description = r.getDescription();
        this.shortName = r.getShortName();
    }
}
