package org.acme.service.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.acme.document.application.usecase.ExtractionReviews;
import org.acme.document.application.usecase.InMemoryDocumentRepository;
import org.acme.document.application.usecase.InMemoryExtractionResultRepository;
import org.acme.document.application.usecase.InMemoryProcessingJobRepository;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.provider.application.usecase.InMemoryProviderRepository;
import org.acme.provider.application.usecase.ProviderQueryUseCase;
import org.acme.provider.domain.model.Provider;
import org.acme.service.application.model.ServiceProofReview;
import org.acme.service.application.model.ServiceView;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.DocumentType;
import org.acme.shared.domain.model.ServiceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceExtractionUseCaseTest {

    private final InMemoryProviderRepository providers = new InMemoryProviderRepository();
    private final InMemoryDocumentRepository documents = new InMemoryDocumentRepository();
    private final InMemoryExtractionResultRepository extractions = new InMemoryExtractionResultRepository();
    private final UUID ana = UUID.randomUUID();
    private ServiceExtractionUseCase useCase;
    private Provider camuzzi;

    @BeforeEach
    void setUp() {
        camuzzi = providers.add(Provider.builder().name("Camuzzi Gas Pampeana").code("CAMUZZI").category(ServiceCategory.GAS).build());
        ExtractionReviews reviews = new ExtractionReviews(extractions, documents, new InMemoryProcessingJobRepository(),
                Clock.fixed(Instant.parse("2026-11-28T12:00:00Z"), ZoneOffset.UTC));
        useCase = new ServiceExtractionUseCase(reviews, new ProviderQueryUseCase(providers),
                new RegisterServiceUseCase(new InMemoryServiceRepository(), new ServiceDraftValidator(providers)), new ObjectMapper());
    }

    @Test
    void reconoceElProveedorAunqueVengaConLaRazonSocial() {
        UUID extraction = save(Map.of("providerName", "Camuzzi Gas Pampeana S.A.", "serviceName", "Gas", "category", "GAS",
                "customerReference", "9100447122"));

        ServiceProofReview review = useCase.review(ana, extraction);

        assertEquals(camuzzi.getId(), review.provider().getId());
        assertEquals("9100447122", review.proof().customerReference());
        assertTrue(review.warnings().isEmpty());
    }

    @Test
    void avisaSiNoEncuentraElProveedorNiElNumeroDeCliente() {
        ServiceProofReview review = useCase.review(ana, save(Map.of("providerName", "Aseguradora Inexistente")));

        assertNull(review.provider());
        assertEquals(2, review.warnings().size());
    }

    @Test
    void elAltaGuardaElServicioYDejaElComprobanteAsociado() {
        UUID extraction = save(Map.of("providerName", "Camuzzi", "serviceName", "Gas"));
        Service draft = Service.builder().providerId(camuzzi.getId()).name("Gas casa").customerReference("9100447122").category(ServiceCategory.GAS).build();

        ServiceView view = useCase.createFromProof(ana, extraction, draft);

        AiExtractionResult result = extractions.findByIdForUser(ana, extraction).orElseThrow();
        assertEquals(ExtractionValidationStatus.CONFIRMED, result.getValidationStatus());
        assertEquals(view.service().getId(), documents.findByIdForUser(ana, result.getDocumentId()).orElseThrow().getServiceId());
        assertThrows(RuntimeException.class, () -> useCase.createFromProof(ana, extraction, draft), "la propuesta ya se usó");
    }

    private UUID save(Map<String, Object> data) {
        UUID document = documents.save(Document.builder().userId(ana).documentType(DocumentType.SERVICE_PROOF).originalFilename("factura.pdf").build()).getId();
        return extractions.save(AiExtractionResult.builder().userId(ana).documentId(document).schemaVersion("service_proof/1")
                .detectedType(DocumentType.SERVICE_PROOF).extractedData(new LinkedHashMap<>(data)).validationStatus(ExtractionValidationStatus.PENDING).build()).getId();
    }
}
