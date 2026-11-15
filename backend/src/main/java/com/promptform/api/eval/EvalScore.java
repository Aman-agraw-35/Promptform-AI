package com.promptform.api.eval;

import java.math.BigDecimal;

public record EvalScore(boolean schemaValid, boolean rendersWithoutErrors,
                        BigDecimal requirementScore, boolean passed) {
}
