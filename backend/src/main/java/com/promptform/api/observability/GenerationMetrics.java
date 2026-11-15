package com.promptform.api.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.DistributionSummary;
import com.promptform.api.generation.GeminiProperties;
import org.springframework.stereotype.Component;

@Component
public class GenerationMetrics {
    private final MeterRegistry registry;
    private final Counter attempts;
    private final Counter successes;
    private final Counter failures;
    private final Counter repairs;
    private final Timer latency;
    private final DistributionSummary totalTokens;
    private final Counter estimatedCost;

    public GenerationMetrics(MeterRegistry registry, GeminiProperties properties) {
        this.registry = registry;
        this.attempts = Counter.builder("promptform.generation.attempts")
                .description("Number of model generation attempts")
                .register(registry);
        this.successes = Counter.builder("promptform.generation.successes")
                .description("Number of validated form generations")
                .register(registry);
        this.failures = Counter.builder("promptform.generation.failures")
                .description("Number of failed form generations")
                .register(registry);
        this.repairs = Counter.builder("promptform.generation.repairs")
                .description("Number of repair prompts sent to the model")
                .register(registry);
        this.latency = Timer.builder("promptform.generation.latency")
                .description("End-to-end form generation latency")
                .publishPercentiles(0.5, 0.95)
                .register(registry);
        this.totalTokens = DistributionSummary.builder("promptform.generation.tokens.total")
                .description("Total tokens reported by the model")
                .register(registry);
        this.estimatedCost = Counter.builder("promptform.generation.cost.usd")
                .description("Estimated generation cost in USD")
                .register(registry);
        this.costPerMillionTokens = properties.costPerMillionTokens();
    }

    private final double costPerMillionTokens;

    public Timer.Sample start() {
        attempts.increment();
        return Timer.start(registry);
    }

    public void recordSuccess(Timer.Sample sample, int repairAttempts) {
        successes.increment();
        repairs.increment(repairAttempts);
        sample.stop(latency);
    }

    public void recordFailure(Timer.Sample sample, int repairAttempts) {
        failures.increment();
        repairs.increment(repairAttempts);
        sample.stop(latency);
    }

    public void recordTokenUsage(long totalTokenCount) {
        if (totalTokenCount <= 0) return;
        totalTokens.record(totalTokenCount);
        estimatedCost.increment(totalTokenCount * costPerMillionTokens / 1_000_000d);
    }

}
