package com.ringcentral.dsg.rules;

import com.ringcentral.dsg.directory.DirectoryUser;
import java.util.List;

public final class SelectionExpressionEvaluator {

    private SelectionExpressionEvaluator() {}

    public static boolean matches(DirectoryUser user, String selectionExpressionJson) {
        SelectionExpression expression = SelectionExpressionParser.parse(selectionExpressionJson);
        return matches(user, expression);
    }

    public static boolean matches(DirectoryUser user, SelectionExpression expression) {
        List<SelectionCriterion> criteria = expression.criteria();
        if (criteria == null || criteria.isEmpty()) {
            return true;
        }
        for (SelectionCriterion criterion : criteria) {
            if (!matchesCriterion(user, criterion)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesCriterion(DirectoryUser user, SelectionCriterion criterion) {
        if (!"EQ".equals(criterion.operator())) {
            throw new UnsupportedOperationException("Unsupported operator: " + criterion.operator());
        }
        String actual = DirectoryAttributePathResolver.resolve(user, criterion.attribute());
        return criterion.value() != null && criterion.value().equals(actual);
    }
}
