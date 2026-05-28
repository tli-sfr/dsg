package com.ringcentral.dsg.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public final class SelectionExpressionParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SelectionExpressionParser() {}

    public static SelectionExpression parse(String selectionExpressionJson) {
        if (selectionExpressionJson == null || selectionExpressionJson.isBlank()) {
            return new SelectionExpression("ALL", List.of());
        }
        try {
            return MAPPER.readValue(selectionExpressionJson, SelectionExpression.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid selection expression JSON", ex);
        }
    }
}
