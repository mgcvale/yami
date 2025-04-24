package com.yamiapp.model.dto;

import java.util.Map;

public record UserStats(Double averageRating, Map<Integer, Long> ratingDistribution) {}
