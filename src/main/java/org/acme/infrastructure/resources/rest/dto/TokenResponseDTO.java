package org.acme.infrastructure.resources.rest.dto;

public record TokenResponseDTO(
        Long id,
        String nombre,
        String email,
        String provider,
        Boolean active,
        String token
) {}