package org.acme.user.application.model;

public record RegisterCommand(String name, String email, String password) {}
