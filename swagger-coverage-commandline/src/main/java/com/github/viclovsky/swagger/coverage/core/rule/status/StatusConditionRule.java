package com.github.viclovsky.swagger.coverage.core.rule.status;

import com.github.viclovsky.swagger.coverage.core.model.Condition;
import com.github.viclovsky.swagger.coverage.core.rule.core.ConditionRule;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Base rule for status
 */
public abstract class StatusConditionRule extends ConditionRule {

    public abstract Condition processStatus(String statusCode, String description);

    public List<Condition> createCondition(Operation operation) {
        if (operation.getResponses() == null) {
            return null;
        }
        return operation.getResponses()
                .entrySet()
                .stream()
                .map(entry -> {
                    String statusCode = entry.getKey();
                    ApiResponse response = entry.getValue();
                    String description = response != null && response.getDescription() != null ? response.getDescription() : "";
                    return processStatus(statusCode, description);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
