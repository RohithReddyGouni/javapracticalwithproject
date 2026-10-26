package com.rohith.javaprojects.airbnb.payment;

import com.google.gson.*;
import com.rohith.javaprojects.airbnb.dto.StripeSessionSnapshot;
import com.rohith.javaprojects.airbnb.dto.WebhookClaim;
import com.rohith.javaprojects.airbnb.exception.InvalidWebhookEventException;
import com.stripe.exception.StripeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StripeWebhookSessionLoader {

    private final StripeCheckoutGateway gateway;

    public StripeWebhookSessionLoader(StripeCheckoutGateway gateway) {
        this.gateway = gateway;
    }

    @Transactional(propagation = Propagation.NEVER)
    public StripeSessionSnapshot load(
            WebhookClaim claim) throws StripeException {

        String sessionId = extractSessionId(claim);

        return gateway.retrieveSession(sessionId);
    }

    private String extractSessionId(WebhookClaim claim) {
        JsonElement parsed;

        try {
            parsed = JsonParser.parseString(claim.payload());
        } catch (JsonParseException exception) {
            throw new InvalidWebhookEventException(
                    "Stored event contains invalid JSON");
        }

        if (!parsed.isJsonObject()) {
            throw new InvalidWebhookEventException(
                    "Stored event must be a JSON object");
        }

        JsonObject event = parsed.getAsJsonObject();

        if (!claim.eventId().equals(requiredString(event, "id"))
                || !claim.eventType().equals(
                requiredString(event, "type"))) {
            throw new InvalidWebhookEventException(
                    "Stored event identifiers do not match the claim");
        }

        JsonObject data = requiredObject(event, "data");
        JsonObject session = requiredObject(data, "object");

        if (!"checkout.session".equals(
                requiredString(session, "object"))) {
            throw new InvalidWebhookEventException(
                    "Event does not contain a checkout session");
        }

        return requiredString(session, "id");
    }

    private JsonObject requiredObject(
            JsonObject parent, String field) {

        JsonElement value = parent.get(field);

        if (value == null || !value.isJsonObject()) {
            throw new InvalidWebhookEventException(
                    "Missing or invalid object: " + field);
        }

        return value.getAsJsonObject();
    }

    private String requiredString(
            JsonObject parent, String field) {

        JsonElement value = parent.get(field);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw new InvalidWebhookEventException(
                    "Missing or invalid string: " + field);
        }

        return value.getAsString();
    }
}
