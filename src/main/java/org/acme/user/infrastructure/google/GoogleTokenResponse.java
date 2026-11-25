package org.acme.user.infrastructure.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Respuesta del token endpoint de Google. Solo interesa el id_token (identidad del usuario). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleTokenResponse(@JsonProperty("id_token") String idToken) {}
