package com.yamiapp.model.projection;

public record ReviewLikedProjection(
  Long reviewId,
  boolean liked
)
{ }
