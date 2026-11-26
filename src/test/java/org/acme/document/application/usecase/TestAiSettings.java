package org.acme.document.application.usecase;

import org.acme.document.application.AiSettings;

import java.time.Duration;

/** Límites de IA para los tests: 2 lecturas por día y 10 minutos de bloqueo. */
public class TestAiSettings implements AiSettings {

    @Override
    public int dailyLimit() {
        return 2;
    }

    @Override
    public Duration lock() {
        return Duration.ofMinutes(10);
    }
}
