package dev.agents4j.api.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Context for observability operations including metrics, tracing, and logging.
 * Provides a unified interface for collecting telemetry data during workflow execution.
 */
public interface ObservabilityContext {
    
    /**
     * Creates a new span for distributed tracing.
     *
     * @param name The span name
     * @return A SpanBuilder for configuring the span
     */
    SpanBuilder withSpan(String name);
    
    /**
     * Creates a metric for measurement.
     *
     * @param name The metric name
     * @return A MetricBuilder for configuring the metric
     */
    MetricBuilder withMetric(String name);
    
    /**
     * Creates a structured log entry.
     *
     * @return A LogBuilder for configuring the log entry
     */
    LogBuilder withLog();
    
    /**
     * Gets the current trace ID if available.
     *
     * @return Optional trace ID
     */
    Optional<String> getTraceId();
    
    /**
     * Gets the current span ID if available.
     *
     * @return Optional span ID
     */
    Optional<String> getSpanId();
    
    /**
     * Checks if observability is enabled.
     *
     * @return true if observability is active
     */
    boolean isEnabled();
    
    /**
     * Creates an empty observability context (no-op).
     *
     * @return A no-op ObservabilityContext
     */
    static ObservabilityContext empty() {
        return NoOpObservabilityContext.INSTANCE;
    }
    
    /**
     * Creates an observability context with basic implementation.
     *
     * @return A basic ObservabilityContext
     */
    static ObservabilityContext basic() {
        return new BasicObservabilityContext();
    }
    
    /**
     * Builder for creating and configuring spans.
     */
    interface SpanBuilder {
        
        /**
         * Adds an attribute to the span.
         *
         * @param key The attribute key
         * @param value The attribute value
         * @return This builder
         */
        SpanBuilder withAttribute(String key, Object value);
        
        /**
         * Adds multiple attributes to the span.
         *
         * @param attributes The attributes to add
         * @return This builder
         */
        SpanBuilder withAttributes(Map<String, Object> attributes);
        
        /**
         * Sets the span kind.
         *
         * @param kind The span kind
         * @return This builder
         */
        SpanBuilder withKind(SpanKind kind);
        
        /**
         * Executes an operation within the span context.
         *
         * @param operation The operation to execute
         * @param <T> The return type
         * @return The result of the operation
         */
        <T> T measure(Supplier<T> operation);
        
        /**
         * Executes an operation within the span context.
         *
         * @param operation The operation to execute
         */
        void measure(Runnable operation);
        
        /**
         * Starts the span manually.
         *
         * @return A Span that must be closed
         */
        Span start();
    }
    
    /**
     * Builder for creating and configuring metrics.
     */
    interface MetricBuilder {
        
        /**
         * Adds a tag to the metric.
         *
         * @param key The tag key
         * @param value The tag value
         * @return This builder
         */
        MetricBuilder withTag(String key, String value);
        
        /**
         * Adds multiple tags to the metric.
         *
         * @param tags The tags to add
         * @return This builder
         */
        MetricBuilder withTags(Map<String, String> tags);
        
        /**
         * Records a counter increment.
         *
         * @param value The value to increment by
         */
        void increment(double value);
        
        /**
         * Records a counter increment by 1.
         */
        default void increment() {
            increment(1.0);
        }
        
        /**
         * Records a gauge value.
         *
         * @param value The gauge value
         */
        void gauge(double value);
        
        /**
         * Records a histogram value.
         *
         * @param value The histogram value
         */
        void histogram(double value);
        
        /**
         * Records a timer value.
         *
         * @param duration The duration to record
         */
        void timer(Duration duration);
        
        /**
         * Times an operation.
         *
         * @param operation The operation to time
         * @param <T> The return type
         * @return The result of the operation
         */
        <T> T time(Supplier<T> operation);
        
        /**
         * Times an operation.
         *
         * @param operation The operation to time
         */
        void time(Runnable operation);
    }
    
    /**
     * Builder for creating structured log entries.
     */
    interface LogBuilder {
        
        /**
         * Sets the log level.
         *
         * @param level The log level
         * @return This builder
         */
        LogBuilder level(LogLevel level);
        
        /**
         * Sets the log message.
         *
         * @param message The log message
         * @return This builder
         */
        LogBuilder message(String message);
        
        /**
         * Adds a field to the log entry.
         *
         * @param key The field key
         * @param value The field value
         * @return This builder
         */
        LogBuilder field(String key, Object value);
        
        /**
         * Adds multiple fields to the log entry.
         *
         * @param fields The fields to add
         * @return This builder
         */
        LogBuilder fields(Map<String, Object> fields);
        
        /**
         * Adds an exception to the log entry.
         *
         * @param throwable The exception
         * @return This builder
         */
        LogBuilder exception(Throwable throwable);
        
        /**
         * Emits the log entry.
         */
        void emit();
    }
    
    /**
     * Represents an active span.
     */
    interface Span extends AutoCloseable {
        
        /**
         * Adds an attribute to the span.
         *
         * @param key The attribute key
         * @param value The attribute value
         * @return This span
         */
        Span setAttribute(String key, Object value);
        
        /**
         * Adds an event to the span.
         *
         * @param name The event name
         * @return This span
         */
        Span addEvent(String name);
        
        /**
         * Adds an event with attributes to the span.
         *
         * @param name The event name
         * @param attributes The event attributes
         * @return This span
         */
        Span addEvent(String name, Map<String, Object> attributes);
        
        /**
         * Records an exception in the span.
         *
         * @param throwable The exception
         * @return This span
         */
        Span recordException(Throwable throwable);
        
        /**
         * Sets the span status.
         *
         * @param status The span status
         * @return This span
         */
        Span setStatus(SpanStatus status);
        
        /**
         * Ends the span.
         */
        void end();
        
        @Override
        default void close() {
            end();
        }
    }
    
    /**
     * Span kinds for categorizing spans.
     */
    enum SpanKind {
        INTERNAL, SERVER, CLIENT, PRODUCER, CONSUMER
    }
    
    /**
     * Span status values.
     */
    enum SpanStatus {
        OK, ERROR, UNSET
    }
    
    /**
     * Log levels.
     */
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}

/**
 * No-op implementation of ObservabilityContext.
 */
final class NoOpObservabilityContext implements ObservabilityContext {
    
    static final NoOpObservabilityContext INSTANCE = new NoOpObservabilityContext();
    
    private NoOpObservabilityContext() {}
    
    @Override
    public SpanBuilder withSpan(String name) {
        return NoOpSpanBuilder.INSTANCE;
    }
    
    @Override
    public MetricBuilder withMetric(String name) {
        return NoOpMetricBuilder.INSTANCE;
    }
    
    @Override
    public LogBuilder withLog() {
        return NoOpLogBuilder.INSTANCE;
    }
    
    @Override
    public Optional<String> getTraceId() {
        return Optional.empty();
    }
    
    @Override
    public Optional<String> getSpanId() {
        return Optional.empty();
    }
    
    @Override
    public boolean isEnabled() {
        return false;
    }
}

/**
 * Basic implementation of ObservabilityContext for testing and simple use cases.
 */
final class BasicObservabilityContext implements ObservabilityContext {
    
    private final Map<String, Object> context = new ConcurrentHashMap<>();
    private volatile String traceId;
    private volatile String spanId;
    
    @Override
    public SpanBuilder withSpan(String name) {
        return new BasicSpanBuilder(name, this);
    }
    
    @Override
    public MetricBuilder withMetric(String name) {
        return new BasicMetricBuilder(name);
    }
    
    @Override
    public LogBuilder withLog() {
        return new BasicLogBuilder();
    }
    
    @Override
    public Optional<String> getTraceId() {
        return Optional.ofNullable(traceId);
    }
    
    @Override
    public Optional<String> getSpanId() {
        return Optional.ofNullable(spanId);
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    void setSpanId(String spanId) {
        this.spanId = spanId;
    }
}

// No-op implementations for all builders and spans
final class NoOpSpanBuilder implements ObservabilityContext.SpanBuilder {
    static final NoOpSpanBuilder INSTANCE = new NoOpSpanBuilder();
    
    @Override
    public ObservabilityContext.SpanBuilder withAttribute(String key, Object value) { return this; }
    
    @Override
    public ObservabilityContext.SpanBuilder withAttributes(Map<String, Object> attributes) { return this; }
    
    @Override
    public ObservabilityContext.SpanBuilder withKind(ObservabilityContext.SpanKind kind) { return this; }
    
    @Override
    public <T> T measure(Supplier<T> operation) { return operation.get(); }
    
    @Override
    public void measure(Runnable operation) { operation.run(); }
    
    @Override
    public ObservabilityContext.Span start() { return NoOpSpan.INSTANCE; }
}

final class NoOpMetricBuilder implements ObservabilityContext.MetricBuilder {
    static final NoOpMetricBuilder INSTANCE = new NoOpMetricBuilder();
    
    @Override
    public ObservabilityContext.MetricBuilder withTag(String key, String value) { return this; }
    
    @Override
    public ObservabilityContext.MetricBuilder withTags(Map<String, String> tags) { return this; }
    
    @Override
    public void increment(double value) {}
    
    @Override
    public void gauge(double value) {}
    
    @Override
    public void histogram(double value) {}
    
    @Override
    public void timer(Duration duration) {}
    
    @Override
    public <T> T time(Supplier<T> operation) { return operation.get(); }
    
    @Override
    public void time(Runnable operation) { operation.run(); }
}

final class NoOpLogBuilder implements ObservabilityContext.LogBuilder {
    static final NoOpLogBuilder INSTANCE = new NoOpLogBuilder();
    
    @Override
    public ObservabilityContext.LogBuilder level(ObservabilityContext.LogLevel level) { return this; }
    
    @Override
    public ObservabilityContext.LogBuilder message(String message) { return this; }
    
    @Override
    public ObservabilityContext.LogBuilder field(String key, Object value) { return this; }
    
    @Override
    public ObservabilityContext.LogBuilder fields(Map<String, Object> fields) { return this; }
    
    @Override
    public ObservabilityContext.LogBuilder exception(Throwable throwable) { return this; }
    
    @Override
    public void emit() {}
}

final class NoOpSpan implements ObservabilityContext.Span {
    static final NoOpSpan INSTANCE = new NoOpSpan();
    
    @Override
    public ObservabilityContext.Span setAttribute(String key, Object value) { return this; }
    
    @Override
    public ObservabilityContext.Span addEvent(String name) { return this; }
    
    @Override
    public ObservabilityContext.Span addEvent(String name, Map<String, Object> attributes) { return this; }
    
    @Override
    public ObservabilityContext.Span recordException(Throwable throwable) { return this; }
    
    @Override
    public ObservabilityContext.Span setStatus(ObservabilityContext.SpanStatus status) { return this; }
    
    @Override
    public void end() {}
}

// Basic implementations for testing
final class BasicSpanBuilder implements ObservabilityContext.SpanBuilder {
    private final String name;
    private final BasicObservabilityContext context;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    
    BasicSpanBuilder(String name, BasicObservabilityContext context) {
        this.name = name;
        this.context = context;
    }
    
    @Override
    public ObservabilityContext.SpanBuilder withAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }
    
    @Override
    public ObservabilityContext.SpanBuilder withAttributes(Map<String, Object> attrs) {
        attributes.putAll(attrs);
        return this;
    }
    
    @Override
    public ObservabilityContext.SpanBuilder withKind(ObservabilityContext.SpanKind kind) {
        attributes.put("span.kind", kind);
        return this;
    }
    
    @Override
    public <T> T measure(Supplier<T> operation) {
        try (var span = start()) {
            return operation.get();
        }
    }
    
    @Override
    public void measure(Runnable operation) {
        try (var span = start()) {
            operation.run();
        }
    }
    
    @Override
    public ObservabilityContext.Span start() {
        return new BasicSpan(name, attributes, context);
    }
}

final class BasicSpan implements ObservabilityContext.Span {
    private final String name;
    private final Map<String, Object> attributes;
    private final BasicObservabilityContext context;
    private final Instant startTime;
    private final String spanId;
    
    BasicSpan(String name, Map<String, Object> attributes, BasicObservabilityContext context) {
        this.name = name;
        this.attributes = new ConcurrentHashMap<>(attributes);
        this.context = context;
        this.startTime = Instant.now();
        this.spanId = java.util.UUID.randomUUID().toString().substring(0, 8);
        
        if (context.getTraceId().isEmpty()) {
            context.setTraceId(java.util.UUID.randomUUID().toString().substring(0, 16));
        }
        context.setSpanId(spanId);
    }
    
    @Override
    public ObservabilityContext.Span setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }
    
    @Override
    public ObservabilityContext.Span addEvent(String eventName) {
        return addEvent(eventName, Map.of());
    }
    
    @Override
    public ObservabilityContext.Span addEvent(String eventName, Map<String, Object> attrs) {
        System.out.printf("[SPAN:%s] Event: %s %s%n", spanId, eventName, attrs);
        return this;
    }
    
    @Override
    public ObservabilityContext.Span recordException(Throwable throwable) {
        System.out.printf("[SPAN:%s] Exception: %s%n", spanId, throwable.getMessage());
        return this;
    }
    
    @Override
    public ObservabilityContext.Span setStatus(ObservabilityContext.SpanStatus status) {
        attributes.put("span.status", status);
        return this;
    }
    
    @Override
    public void end() {
        Duration duration = Duration.between(startTime, Instant.now());
        System.out.printf("[SPAN:%s] %s completed in %d ms%n", spanId, name, duration.toMillis());
    }
}

final class BasicMetricBuilder implements ObservabilityContext.MetricBuilder {
    private final String name;
    private final Map<String, String> tags = new ConcurrentHashMap<>();
    
    BasicMetricBuilder(String name) {
        this.name = name;
    }
    
    @Override
    public ObservabilityContext.MetricBuilder withTag(String key, String value) {
        tags.put(key, value);
        return this;
    }
    
    @Override
    public ObservabilityContext.MetricBuilder withTags(Map<String, String> newTags) {
        tags.putAll(newTags);
        return this;
    }
    
    @Override
    public void increment(double value) {
        System.out.printf("[METRIC] %s.increment = %.2f %s%n", name, value, tags);
    }
    
    @Override
    public void gauge(double value) {
        System.out.printf("[METRIC] %s.gauge = %.2f %s%n", name, value, tags);
    }
    
    @Override
    public void histogram(double value) {
        System.out.printf("[METRIC] %s.histogram = %.2f %s%n", name, value, tags);
    }
    
    @Override
    public void timer(Duration duration) {
        System.out.printf("[METRIC] %s.timer = %d ms %s%n", name, duration.toMillis(), tags);
    }
    
    @Override
    public <T> T time(Supplier<T> operation) {
        Instant start = Instant.now();
        try {
            return operation.get();
        } finally {
            timer(Duration.between(start, Instant.now()));
        }
    }
    
    @Override
    public void time(Runnable operation) {
        time(() -> {
            operation.run();
            return null;
        });
    }
}

final class BasicLogBuilder implements ObservabilityContext.LogBuilder {
    private ObservabilityContext.LogLevel level = ObservabilityContext.LogLevel.INFO;
    private String message = "";
    private final Map<String, Object> fields = new ConcurrentHashMap<>();
    private Throwable throwable;
    
    @Override
    public ObservabilityContext.LogBuilder level(ObservabilityContext.LogLevel level) {
        this.level = level;
        return this;
    }
    
    @Override
    public ObservabilityContext.LogBuilder message(String message) {
        this.message = message;
        return this;
    }
    
    @Override
    public ObservabilityContext.LogBuilder field(String key, Object value) {
        fields.put(key, value);
        return this;
    }
    
    @Override
    public ObservabilityContext.LogBuilder fields(Map<String, Object> newFields) {
        fields.putAll(newFields);
        return this;
    }
    
    @Override
    public ObservabilityContext.LogBuilder exception(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }
    
    @Override
    public void emit() {
        System.out.printf("[LOG:%s] %s %s%n", level, message, fields);
        if (throwable != null) {
            System.out.printf("[LOG:%s] Exception: %s%n", level, throwable.getMessage());
        }
    }
}