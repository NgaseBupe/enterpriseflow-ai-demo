package com.enterpriseflow.ai.provider.mock;

import com.enterpriseflow.ai.AiDocumentExtractionService;
import com.enterpriseflow.ai.AiExtractionException;
import com.enterpriseflow.ai.DocumentInput;
import com.enterpriseflow.ai.OrderExtractionResult;
import com.enterpriseflow.ai.OrderExtractionResult.LineItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * A stand-in for a real AI model, so the whole workflow runs without an API key and tests are
 * deterministic. It returns a realistic, synthetic purchase order: the same file always gives the
 * same result.
 *
 * <p>For demonstrating failures, any file whose name contains "fail" is rejected with a provider error.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
class MockExtractionProvider implements AiDocumentExtractionService {

    static final String PROVIDER = "mock";
    static final String MODEL = "mock-v1";

    private static final Pattern DIGITS = Pattern.compile("(\\d{3,})");
    private static final List<Customer> CUSTOMERS = List.of(
            new Customer("Chanda Hardware Ltd", "orders@chanda-hardware.example", "+260 211 000 101",
                    "Plot 12, Industrial Area, Lusaka"),
            new Customer("Mwila Building Supplies", "purchasing@mwila-supplies.example", "+260 212 000 202",
                    "45 Freedom Way, Kitwe"),
            new Customer("Kasonde Engineering", "procurement@kasonde-eng.example", "+260 213 000 303",
                    "7 Mosi-oa-Tunya Road, Livingstone"));

    @Override
    public OrderExtractionResult extract(DocumentInput document) {
        String fileName = document.fileName() == null ? "" : document.fileName();
        if (fileName.toLowerCase(Locale.ROOT).contains("fail")) {
            throw new AiExtractionException(AiExtractionException.Reason.PROVIDER_ERROR,
                    "Mock provider simulated a failure for " + fileName);
        }

        int seed = Math.floorMod(Arrays.hashCode(document.content()), CUSTOMERS.size());
        Customer customer = CUSTOMERS.get(seed);
        List<LineItem> lines = List.of(
                line("HB-M8-100", "M8 hex bolts, box of 100", "12", "box", "85.00"),
                line("GW-M8-200", "Galvanised washers M8, pack of 200", "10", "pack", "42.50"),
                line("AF-10", "Anchor fasteners 10 mm", "25", "each", "18.40"));
        BigDecimal subtotal = lines.stream().map(LineItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.16")).setScale(2);

        return new OrderExtractionResult(
                poNumber(fileName),
                LocalDate.of(2026, 9, 1),
                customer.name(),
                customer.email(),
                customer.phone(),
                customer.address(),
                LocalDate.of(2026, 9, 15),
                "ZMW",
                subtotal,
                tax,
                subtotal.add(tax),
                "Deliver to the loading bay between 08:00 and 16:00.",
                lines,
                new BigDecimal("0.93"),
                PROVIDER,
                MODEL);
    }

    private static String poNumber(String fileName) {
        Matcher matcher = DIGITS.matcher(fileName);
        return matcher.find() ? "PO-" + matcher.group(1) : "PO-0001";
    }

    private static LineItem line(String code, String description, String quantity, String unit, String unitPrice) {
        BigDecimal qty = new BigDecimal(quantity);
        BigDecimal price = new BigDecimal(unitPrice);
        return new LineItem(code, description, qty, unit, price, qty.multiply(price).setScale(2));
    }

    private record Customer(String name, String email, String phone, String address) {
    }
}
