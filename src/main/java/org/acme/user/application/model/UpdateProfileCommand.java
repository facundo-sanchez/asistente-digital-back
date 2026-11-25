package org.acme.user.application.model;

public record UpdateProfileCommand(String name, String timezone, String locale, String defaultCurrency) {}
