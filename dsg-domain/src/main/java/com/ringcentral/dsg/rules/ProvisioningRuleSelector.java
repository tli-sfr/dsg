package com.ringcentral.dsg.rules;

import com.ringcentral.dsg.directory.DirectoryUser;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ProvisioningRuleSelector {

    private ProvisioningRuleSelector() {}

    /**
     * Rules are evaluated in ascending {@code priority}; first match wins.
     */
    public static Optional<ProvisioningRuleMatch> selectFirstMatch(
            List<ProvisioningRuleMatch> rules, DirectoryUser user) {
        return rules.stream()
                .sorted(Comparator.comparingInt(ProvisioningRuleMatch::priority))
                .filter(rule -> SelectionExpressionEvaluator.matches(user, rule.selectionExpressionJson()))
                .findFirst();
    }
}
