package org.acme.service.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.document.application.usecase.ExtractionReviews;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.provider.application.usecase.ProviderQueryUseCase;
import org.acme.provider.domain.model.Provider;
import org.acme.service.application.model.ExtractedServiceProof;
import org.acme.service.application.model.ServiceProofReview;
import org.acme.service.application.model.ServiceView;
import org.acme.service.domain.model.Service;
import org.acme.shared.application.model.PageQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Alta de un servicio desde un comprobante (factura, alta, póliza) leído por la IA. La IA propone
 * proveedor, nombre, categoría y nro. de cliente; el usuario siempre revisa antes de guardar.
 */
@ApplicationScoped
public class ServiceExtractionUseCase {

    private final ExtractionReviews reviews;
    private final ProviderQueryUseCase providers;
    private final RegisterServiceUseCase register;
    private final ObjectMapper json;

    @Inject
    public ServiceExtractionUseCase(ExtractionReviews reviews, ProviderQueryUseCase providers, RegisterServiceUseCase register, ObjectMapper json) {
        this.reviews = reviews;
        this.providers = providers;
        this.register = register;
        this.json = json;
    }

    public ServiceProofReview review(UUID userId, UUID extractionId) {
        AiExtractionResult result = reviews.find(userId, extractionId, ExtractedServiceProof.SCHEMA_VERSION);
        ExtractedServiceProof proof = json.convertValue(result.getExtractedData(), ExtractedServiceProof.class);
        Provider provider = matchProvider(userId, proof.providerName()).orElse(null);
        List<String> warnings = new ArrayList<>();
        if (provider == null) {
            warnings.add(proof.providerName() == null
                    ? "No pudimos leer el proveedor: elegilo de la lista."
                    : "No encontramos \"" + proof.providerName() + "\" en la lista de proveedores: elegilo o crealo.");
        }
        if (proof.customerReference() == null) {
            warnings.add("No encontramos el número de cliente o suministro: si lo tenés, completalo.");
        }
        return new ServiceProofReview(result.getId(), result.getDocumentId(), result.getValidationStatus(), proof, provider, warnings);
    }

    /** Guarda el servicio revisado; el comprobante queda asociado a él y la propuesta, cerrada. */
    @Transactional
    public ServiceView createFromProof(UUID userId, UUID extractionId, Service draft) {
        AiExtractionResult result = reviews.pending(userId, extractionId, ExtractedServiceProof.SCHEMA_VERSION);
        ServiceView view = register.register(userId, draft);
        reviews.close(userId, result, ExtractionValidationStatus.CONFIRMED, view.service().getId());
        return view;
    }

    /** Busca el proveedor por el nombre completo y, si no aparece, por su primera palabra ("Camuzzi Gas Pampeana S.A." → "Camuzzi"). */
    Optional<Provider> matchProvider(UUID userId, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String clean = name.replaceAll("(?i)\\b(s\\.?a\\.?|s\\.?r\\.?l\\.?|s\\.?a\\.?u\\.?)\\s*$", "").trim();
        Optional<Provider> full = first(userId, clean);
        if (full.isPresent()) {
            return full;
        }
        String firstWord = clean.split("\\s+")[0];
        return firstWord.length() >= 4 ? first(userId, firstWord) : Optional.empty();
    }

    private Optional<Provider> first(UUID userId, String text) {
        return providers.list(userId, text, null, new PageQuery(0, 1)).content().stream().findFirst();
    }
}
