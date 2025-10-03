package com.yamiapp.model.dto;

import java.util.Map;

public record FoodStats(Map<Integer, Long> ratingDistribution) {}
