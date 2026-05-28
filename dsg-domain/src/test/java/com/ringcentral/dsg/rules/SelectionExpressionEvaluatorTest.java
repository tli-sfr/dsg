package com.ringcentral.dsg.rules;

import com.ringcentral.dsg.directory.DirectoryUser;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionExpressionEvaluatorTest {

    private final DirectoryUser salesUser = new DirectoryUser(
            "u1", "u1@example.com", Map.of("department", "Sales", "group", "sales-group"));
    private final DirectoryUser engineeringUser = new DirectoryUser(
            "u2", "u2@example.com", Map.of("department", "Engineering", "group", "eng-group"));

    @Test
    void matchesAllUsersWhenExpressionNullOrEmptyCriteria() {
        assertTrue(SelectionExpressionEvaluator.matches(salesUser, (String) null));
        assertTrue(SelectionExpressionEvaluator.matches(engineeringUser, "{\"match\":\"ALL\"}"));
        assertTrue(SelectionExpressionEvaluator.matches(salesUser, "{\"match\":\"ALL\",\"criteria\":[]}"));
    }

    @Test
    void matchesSingleEqCriterion() {
        String expr = """
                {"match":"ALL","criteria":[{"attribute":"user.department","operator":"EQ","value":"Sales"}]}
                """;
        assertTrue(SelectionExpressionEvaluator.matches(salesUser, expr));
        assertFalse(SelectionExpressionEvaluator.matches(engineeringUser, expr));
    }

    @Test
    void requiresAllCriteriaWithAndLogic() {
        String expr = """
                {"match":"ALL","criteria":[
                  {"attribute":"user.department","operator":"EQ","value":"Sales"},
                  {"attribute":"user.group","operator":"EQ","value":"sales-group"}
                ]}
                """;
        assertTrue(SelectionExpressionEvaluator.matches(salesUser, expr));
        assertFalse(SelectionExpressionEvaluator.matches(engineeringUser, expr));
    }

    @Test
    void firstMatchWinsByPriority() {
        var rules = java.util.List.of(
                new ProvisioningRuleMatch(2, "Engineering", 2, """
                        {"match":"ALL","criteria":[{"attribute":"user.department","operator":"EQ","value":"Engineering"}]}
                        """),
                new ProvisioningRuleMatch(1, "Sales", 1, """
                        {"match":"ALL","criteria":[{"attribute":"user.department","operator":"EQ","value":"Sales"}]}
                        """));
        assertTrue(ProvisioningRuleSelector.selectFirstMatch(rules, salesUser).map(ProvisioningRuleMatch::ruleId).orElse(-1L) == 1L);
        assertTrue(ProvisioningRuleSelector.selectFirstMatch(rules, engineeringUser).map(ProvisioningRuleMatch::ruleId).orElse(-1L) == 2L);
    }
}
