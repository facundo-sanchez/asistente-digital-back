package org.acme.shared.application.model;

/**
 * Página pedida por el cliente. page empieza en 0. Los límites ya vienen validados
 * desde REST; el record los vuelve a acotar para que ningún caso de uso pida de más.
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), MAX_SIZE);
    }
}
