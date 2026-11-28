package org.acme.assistant.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.acme.assistant.domain.model.Reference;
import org.acme.assistant.domain.model.ReferenceType;
import org.acme.card.application.model.SpendingFilter;
import org.acme.card.domain.model.CardSpendingReport;
import org.acme.card.domain.model.SpendingCategory;
import org.acme.invoice.application.model.InvoiceFilter;
import org.acme.invoice.application.model.InvoiceView;
import org.acme.invoice.domain.model.InvoiceStatus;
import org.acme.obligation.application.model.ObligationFilter;
import org.acme.obligation.application.model.ObligationView;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.domain.model.Obligation;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.acme.obligation.domain.model.EffectiveObligationStatus.*;

/**
 * Herramientas de solo lectura del asistente (SPEC 9.5). El usuario es el de la sesión, fijado al crearlas:
 * ninguna herramienta lo recibe como parámetro, así el modelo no puede pedir datos de otro usuario.
 * Cada registro devuelto queda anotado como referencia de la respuesta.
 */
public class AssistantTools {

    /** Registros por herramienta: respuestas cortas y pocos tokens. */
    static final int LIMIT = 20;
    private static final Set<EffectiveObligationStatus> OPEN = EnumSet.of(PENDING, PARTIALLY_PAID, DUE_SOON, OVERDUE);

    private final AssistantToolbox box;
    private final UUID userId;
    private final LocalDate today;
    private final Map<UUID, Reference> references = new LinkedHashMap<>();
    private final List<String> used = new ArrayList<>();

    AssistantTools(AssistantToolbox box, UUID userId, LocalDate today) {
        this.box = box;
        this.userId = userId;
        this.today = today;
    }

    public List<Reference> references() {
        return List.copyOf(references.values());
    }

    public List<String> used() {
        return List.copyOf(used);
    }

    /** Ejecuta lo que pidió el modelo. Un error vuelve como texto para que el modelo lo explique. */
    public String execute(ToolExecutionRequest request) {
        used.add(request.name());
        try {
            JsonNode args = box.json.readTree(request.arguments() == null || request.arguments().isBlank() ? "{}" : request.arguments());
            Object result = switch (request.name()) {
                case "upcomingObligations" -> upcomingObligations(args.path("days").asInt(7));
                case "overdueObligations" -> overdueObligations();
                case "unpaidInvoices" -> unpaidInvoices();
                case "monthSummary" -> monthSummary();
                case "activeServices" -> activeServices();
                case "servicePayments" -> servicePayments();
                case "cardSpending" -> cardSpending(args.path("fromMonth").asText(null), args.path("toMonth").asText(null),
                        args.path("category").asText(null));
                case "pendingInstallments" -> pendingInstallments();
                case "expiringDocuments" -> expiringDocuments();
                case "searchDocuments" -> searchDocuments(args.path("text").asText(""));
                case "loans" -> loans();
                case "subscriptions" -> subscriptions();
                default -> Map.of("error", "No existe la herramienta " + request.name());
            };
            return box.json.writeValueAsString(result);
        } catch (JsonProcessingException | RuntimeException e) {
            return "{\"error\": \"No se pudo consultar: " + e.getClass().getSimpleName() + "\"}";
        }
    }

    @Tool("Vencimientos sin pagar que vencen desde hoy hasta dentro de 'days' días (facturas, tarjetas, impuestos, alquiler…)")
    public Object upcomingObligations(@P("Cantidad de días hacia adelante, por ejemplo 7 para esta semana") int days) {
        return obligationList(new ObligationFilter(null, OPEN, null, null, today, today.plusDays(Math.max(days, 0))));
    }

    @Tool("Vencimientos que ya vencieron y no se pagaron")
    public Object overdueObligations() {
        return obligationList(new ObligationFilter(null, EnumSet.of(OVERDUE), null, null, null, null));
    }

    @Tool("Facturas de servicios que todavía no se pagaron")
    public Object unpaidInvoices() {
        List<InvoiceView> unpaid = box.invoices.list(userId, new InvoiceFilter(null, null, InvoiceStatus.CONFIRMED, null, null), new PageQuery(0, 100))
                .content().stream()
                .filter(v -> v.obligation() != null && OPEN.contains(v.obligation().effectiveStatus()))
                .toList();
        return limited(unpaid.stream().map(v -> {
            reference(ReferenceType.INVOICE, v.invoice().getId(), v.serviceName() + " " + Objects.toString(v.invoice().getBillingPeriod(), ""));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("servicio", v.serviceName());
            item.put("periodo", v.invoice().getBillingPeriod());
            item.put("vence", v.invoice().getDueDate());
            item.put("importe", v.invoice().getTotalAmount());
            item.put("moneda", v.invoice().getCurrency());
            item.put("saldo", v.obligation().balance());
            item.put("estado", v.obligation().effectiveStatus());
            return item;
        }).toList(), unpaid.size());
    }

    @Tool("Resumen del mes actual: total a pagar, pagado y pendiente por moneda, vencidos y vencimientos sin importe")
    public Object monthSummary() {
        var summary = box.dashboard.summary(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mes", summary.monthStart() + " a " + summary.monthEnd());
        result.put("porMoneda", summary.month());
        result.put("vencidos", summary.overdue());
        result.put("sinImporte", summary.withoutAmount());
        result.put("vencenEn7Dias", summary.dueSoon());
        return result;
    }

    @Tool("Servicios activos del usuario (luz, gas, internet, seguros…) con su proveedor")
    public Object activeServices() {
        var page = box.services.list(userId, null, null, true, new PageQuery(0, LIMIT));
        return limited(page.content().stream().map(v -> {
            reference(ReferenceType.SERVICE, v.service().getId(), v.service().getName());
            return Map.of("nombre", v.service().getName(), "proveedor", v.providerName(), "categoria", v.service().getCategory());
        }).toList(), page.totalElements());
    }

    @Tool("Lo pagado en vencimientos por mes (los últimos 6 meses) y lo pagado este mes por categoría de servicio")
    public Object servicePayments() {
        return box.dashboard.spending(userId);
    }

    @Tool("Consumos de tarjeta de crédito por rubro, mes y comercio. Meses en formato yyyy-MM; sin meses, los últimos 6")
    public Object cardSpending(@P(value = "Primer mes, yyyy-MM", required = false) String fromMonth,
                               @P(value = "Último mes, yyyy-MM", required = false) String toMonth,
                               @P(value = "Rubro en inglés (SUPERMARKET, FUEL, RESTAURANTS…) o vacío para todos", required = false) String category) {
        CardSpendingReport report = box.cards.report(userId, new SpendingFilter(month(fromMonth), month(toMonth), null, category(category), null));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("desde", report.from());
        result.put("hasta", report.to());
        result.put("totales", report.totals());
        result.put("porRubro", report.byCategory());
        result.put("porMes", report.byMonth());
        result.put("comercios", report.topMerchants());
        return result;
    }

    @Tool("Compras en cuotas con tarjeta que todavía no terminaron: cuánto falta pagar de cada una")
    public Object pendingInstallments() {
        return box.cards.pending(userId);
    }

    @Tool("Documentos que vencen pronto (pólizas de seguro, garantías, VTV…)")
    public Object expiringDocuments() {
        return box.dashboard.summary(userId).expiringDocuments().stream().map(d -> {
            reference(ReferenceType.DOCUMENT, d.id(), d.title());
            return Map.of("titulo", d.title(), "tipo", d.documentType(), "vence", d.expiresOn());
        }).toList();
    }

    @Tool("Busca documentos por nombre o título (por ejemplo 'seguro auto', 'garantía heladera')")
    public Object searchDocuments(@P("Texto a buscar") String text) {
        var page = box.documents.list(userId, text, null, null, new PageQuery(0, LIMIT));
        return limited(page.content().stream().map(v -> {
            String title = v.document().getTitle() != null ? v.document().getTitle() : v.document().getOriginalFilename();
            reference(ReferenceType.DOCUMENT, v.document().getId(), title);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("titulo", title);
            item.put("tipo", v.document().getDocumentType());
            item.put("servicio", v.serviceName());
            item.put("vence", v.document().getExpiresOn());
            return item;
        }).toList(), page.totalElements());
    }

    @Tool("Préstamos que tomó el usuario: cuotas pagadas y pendientes, lo que falta pagar, saldo de capital según el banco y la próxima cuota")
    public Object loans() {
        return box.loans.list(userId).stream().map(v -> {
            reference(ReferenceType.LOAN, v.loan().getId(), v.loan().getName() + " " + v.loan().getLender());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("prestamo", v.loan().getName());
            item.put("entidad", v.loan().getLender());
            item.put("estado", v.effectiveStatus());
            item.put("moneda", v.loan().getCurrency());
            item.put("cuotasPagadas", v.paidCount());
            item.put("cuotasTotales", v.loan().getInstallmentsCount());
            item.put("faltaPagarEstimado", v.remaining());
            item.put("saldoCapitalBanco", v.loan().getBankBalance());
            v.next().ifPresent(n -> {
                item.put("proximaCuota", n.obligation().getInstallmentNumber());
                item.put("proximoVencimiento", n.obligation().getDueDate());
                item.put("proximoImporte", n.balance());
            });
            return item;
        }).toList();
    }

    @Tool("Suscripciones que se cobran todos los meses en las tarjetas (Netflix, Spotify, gimnasio…): último importe, desde cuándo, aumento y total por mes por moneda")
    public Object subscriptions() {
        var result = box.subscriptions.list(userId, null);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("suscripciones", result.items());
        item.put("totalPorMes", result.totals());
        return item;
    }

    private Object obligationList(ObligationFilter filter) {
        PageResult<ObligationView> page = box.obligations.list(userId, filter, new PageQuery(0, LIMIT));
        return limited(page.content().stream().map(v -> {
            Obligation o = v.summary().obligation();
            reference(ReferenceType.OBLIGATION, o.getId(), o.getTitle());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("titulo", o.getTitle());
            item.put("servicio", v.serviceName());
            item.put("vence", o.getDueDate());
            item.put("importe", o.getAmount());
            item.put("moneda", o.getCurrency());
            item.put("saldo", v.summary().balance());
            item.put("estado", v.summary().effectiveStatus());
            return item;
        }).toList(), page.totalElements());
    }

    /** La lista y, si había más de las que se mandan, cuántas hay en total. */
    private static Map<String, Object> limited(List<?> items, long total) {
        return Map.of("items", items, "total", total);
    }

    private void reference(ReferenceType type, UUID id, String label) {
        references.putIfAbsent(id, new Reference(type, id, label));
    }

    private static LocalDate month(String value) {
        try {
            return value == null || value.isBlank() ? null : YearMonth.parse(value.trim()).atDay(1);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static SpendingCategory category(String value) {
        try {
            return value == null || value.isBlank() ? null : SpendingCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
