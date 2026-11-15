package com.promptform.api.eval;

import java.util.List;

public record EvalCase(String id, String prompt, List<String> requiredFields) {
}
