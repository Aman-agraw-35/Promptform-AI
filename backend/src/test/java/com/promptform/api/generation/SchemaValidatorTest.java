package com.promptform.api.generation;

import com.promptform.api.generation.GenerationModels.FormOption;
import com.promptform.api.generation.GenerationModels.UiNode;
import com.promptform.api.generation.GenerationModels.UiSchema;
import com.promptform.api.generation.GenerationModels.ValidationRules;
import com.promptform.api.generation.GenerationModels.VisibleWhen;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaValidatorTest {
    private final SchemaValidator validator = new SchemaValidator();

    @Test
    void acceptsValidConditionalField() {
        UiNode vehicleType = field("vehicleType", "select", null, null, List.of(
                new FormOption("car", "Car"),
                new FormOption("bike", "Bike")
        ));
        UiNode engineCapacity = field(
                "engineCc",
                "number",
                new VisibleWhen("vehicleType", "bike", null, null),
                new ValidationRules(50.0, 2000.0, null),
                null
        );

        assertDoesNotThrow(() -> validator.validate(schema(vehicleType, engineCapacity)));
    }

    @Test
    void rejectsMissingConditionReference() {
        UiNode conditional = field(
                "engineCc",
                "number",
                new VisibleWhen("vehicleType", "bike", null, null),
                null,
                null
        );

        assertThrows(GenerationException.class, () -> validator.validate(schema(conditional)));
    }

    @Test
    void rejectsCircularConditions() {
        UiNode first = field("first", "text", new VisibleWhen("second", "yes", null, null), null, null);
        UiNode second = field("second", "text", new VisibleWhen("first", "yes", null, null), null, null);

        assertThrows(GenerationException.class, () -> validator.validate(schema(first, second)));
    }

    @Test
    void rejectsDuplicateOptionValues() {
        UiNode select = field("vehicleType", "select", null, null, List.of(
                new FormOption("car", "Car"),
                new FormOption("car", "Another car")
        ));

        assertThrows(GenerationException.class, () -> validator.validate(schema(select)));
    }

    @Test
    void rejectsInvalidValidationRules() {
        UiNode field = field("age", "number", null, new ValidationRules(100.0, 10.0, null), null);

        assertThrows(GenerationException.class, () -> validator.validate(schema(field)));
    }

    private UiSchema schema(UiNode... nodes) {
        return new UiSchema("Test form", null, List.of(nodes));
    }

    private UiNode field(
            String name,
            String type,
            VisibleWhen visibleWhen,
            ValidationRules validation,
            List<FormOption> options
    ) {
        return new UiNode(type, name, name, null, true, null, options, null, visibleWhen, validation);
    }
}
