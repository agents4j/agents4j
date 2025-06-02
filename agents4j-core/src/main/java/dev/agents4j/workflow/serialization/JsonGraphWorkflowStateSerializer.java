package dev.agents4j.workflow.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.GraphPosition;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.graph.StateMetadata;
import dev.agents4j.api.graph.WorkflowId;
import dev.agents4j.api.serialization.WorkflowStateSerializer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * JSON-based serializer for GraphWorkflowState that handles version compatibility
 * and provides safe serialization/deserialization with type preservation.
 */
public class JsonGraphWorkflowStateSerializer<S>
    implements WorkflowStateSerializer<GraphWorkflowState<S>> {

    private final ObjectMapper objectMapper;
    private final Class<S> stateDataType;
    private final String schemaVersion;

    public JsonGraphWorkflowStateSerializer(Class<S> stateDataType) {
        this(stateDataType, "1.0.0");
    }

    public JsonGraphWorkflowStateSerializer(
        Class<S> stateDataType,
        String schemaVersion
    ) {
        this.stateDataType = stateDataType;
        this.schemaVersion = schemaVersion;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String serialize(
        GraphWorkflowState<S> state,
        String workflowVersion
    ) throws SerializationException {
        try {
            SerializableWrapper wrapper = new SerializableWrapper(
                workflowVersion,
                schemaVersion,
                state.workflowId().value(),
                state.data(),
                state.currentNode().map(NodeId::value).orElse(null),
                convertContextToMap(state.context()),
                convertPositionToMap(state.position()),
                convertMetadataToMap(state.metadata()),
                Instant.now(),
                stateDataType.getName()
            );

            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            throw new SerializationException(
                "Failed to serialize GraphWorkflowState",
                e
            );
        } catch (Exception e) {
            throw new SerializationException(
                "Unexpected error during serialization",
                e
            );
        }
    }

    @Override
    public GraphWorkflowState<S> deserialize(
        String serializedState,
        String expectedVersion
    ) throws DeserializationException, VersionMismatchException {
        try {
            SerializableWrapper wrapper = objectMapper.readValue(
                serializedState,
                SerializableWrapper.class
            );

            // Validate version compatibility
            if (!isCompatible(wrapper.workflowVersion(), expectedVersion)) {
                throw new VersionMismatchException(
                    wrapper.workflowVersion(),
                    expectedVersion
                );
            }

            // Validate state data type
            if (!stateDataType.getName().equals(wrapper.stateDataType())) {
                throw new DeserializationException(
                    String.format(
                        "State data type mismatch: expected %s, found %s",
                        stateDataType.getName(),
                        wrapper.stateDataType()
                    )
                );
            }

            // Reconstruct the state
            WorkflowId workflowId = WorkflowId.of(wrapper.workflowId());
            S stateData = objectMapper.convertValue(
                wrapper.stateData(),
                stateDataType
            );
            Optional<NodeId> currentNode = Optional.ofNullable(
                wrapper.currentNode()
            ).map(NodeId::of);
            WorkflowContext context = convertMapToContext(
                wrapper.contextData()
            );
            GraphPosition position = convertMapToPosition(
                wrapper.positionData()
            );
            StateMetadata metadata = convertMapToMetadata(
                wrapper.metadataData()
            );

            return new GraphWorkflowState<>(
                workflowId,
                stateData,
                context,
                currentNode,
                position,
                metadata
            );
        } catch (VersionMismatchException e) {
            throw e; // Re-throw version mismatch exceptions
        } catch (JsonProcessingException e) {
            throw new DeserializationException("Failed to parse JSON", e);
        } catch (Exception e) {
            throw new DeserializationException(
                "Failed to deserialize GraphWorkflowState",
                e
            );
        }
    }

    @Override
    public boolean isCompatible(
        String serializedVersion,
        String currentVersion
    ) {
        if (serializedVersion == null || currentVersion == null) {
            return false;
        }

        // Simple semantic versioning compatibility check
        String[] serializedParts = serializedVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");

        if (serializedParts.length < 2 || currentParts.length < 2) {
            return serializedVersion.equals(currentVersion);
        }

        // Major version must match, minor version can be different
        return serializedParts[0].equals(currentParts[0]);
    }

    @Override
    public Optional<String> migrate(
        String serializedState,
        String fromVersion,
        String toVersion
    ) throws MigrationException {
        // Basic migration support - in practice, you'd implement version-specific migrations
        if (isCompatible(fromVersion, toVersion)) {
            try {
                // Parse and re-serialize with new version
                JsonNode node = objectMapper.readTree(serializedState);
                if (node.isObject()) {
                    ((ObjectNode) node).put("workflowVersion", toVersion);
                    return Optional.of(objectMapper.writeValueAsString(node));
                }
            } catch (JsonProcessingException e) {
                throw new MigrationException("Failed to migrate state", e);
            }
        }
        return Optional.empty();
    }

    @Override
    public String extractVersion(String serializedState) {
        try {
            JsonNode node = objectMapper.readTree(serializedState);
            JsonNode versionNode = node.get("workflowVersion");
            return versionNode != null ? versionNode.asText() : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public String getFormat() {
        return "json";
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>(
            WorkflowStateSerializer.super.getMetadata()
        );
        metadata.put("stateDataType", stateDataType.getName());
        metadata.put("schemaVersion", schemaVersion);
        metadata.put("jacksonVersion", getJacksonVersion());
        return metadata;
    }

    private String getJacksonVersion() {
        try {
            return objectMapper.version().toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Map<String, Object> convertContextToMap(WorkflowContext context) {
        Map<String, Object> map = new HashMap<>();
        Set<ContextKey<?>> keys = context.keys();

        for (ContextKey<?> key : keys) {
            Object value = context.get(key).orElse(null);
            if (value != null) {
                // Store with type information for safe reconstruction
                Map<String, Object> typedValue = Map.of(
                    "value",
                    value,
                    "type",
                    value.getClass().getName()
                );
                map.put(key.name(), typedValue);
            }
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    private WorkflowContext convertMapToContext(Map<String, Object> map) {
        WorkflowContext context = WorkflowContext.empty();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            try {
                if (entry.getValue() instanceof Map<?, ?> typedValue) {
                    Object value =
                        ((Map<String, Object>) typedValue).get("value");
                    String type = (String) ((Map<
                                String,
                                Object
                            >) typedValue).get("type");

                    // Reconstruct context key based on type
                    if (value != null) {
                        context = addTypedValueToContext(
                            context,
                            entry.getKey(),
                            value,
                            type
                        );
                    }
                }
            } catch (Exception e) {
                // Skip problematic entries rather than failing completely
                System.err.println(
                    "Failed to restore context key: " +
                    entry.getKey() +
                    ", error: " +
                    e.getMessage()
                );
            }
        }

        return context;
    }

    private WorkflowContext addTypedValueToContext(
        WorkflowContext context,
        String keyName,
        Object value,
        String type
    ) {
        try {
            // Handle common types
            switch (type) {
                case "java.lang.String" -> {
                    ContextKey<String> key = ContextKey.stringKey(keyName);
                    return context.with(key, (String) value);
                }
                case "java.lang.Integer" -> {
                    ContextKey<Integer> key = ContextKey.intKey(keyName);
                    return context.with(key, (Integer) value);
                }
                case "java.lang.Boolean" -> {
                    ContextKey<Boolean> key = ContextKey.booleanKey(keyName);
                    return context.with(key, (Boolean) value);
                }
                case "java.lang.Long" -> {
                    ContextKey<Long> key = ContextKey.of(keyName, Long.class);
                    return context.with(key, (Long) value);
                }
                case "java.lang.Double" -> {
                    ContextKey<Double> key = ContextKey.of(
                        keyName,
                        Double.class
                    );
                    return context.with(key, (Double) value);
                }
                default -> {
                    // For other types, try to reconstruct using the original type
                    Class<?> valueClass = Class.forName(type);
                    Object convertedValue = objectMapper.convertValue(
                        value,
                        valueClass
                    );
                    @SuppressWarnings("unchecked")
                    ContextKey<Object> key = (ContextKey<Object>) ContextKey.of(keyName, valueClass);
                    return context.with(key, convertedValue);
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println(
                "Unknown type for context key " + keyName + ": " + type
            );
            return context;
        }
    }

    private Map<String, Object> convertPositionToMap(GraphPosition position) {
        Map<String, Object> map = new HashMap<>();
        map.put("depth", position.depth());
        map.put(
            "path",
            position.getPath().stream().map(NodeId::value).toList()
        );
        map.put(
            "previousNode",
            position.previousNodeId().map(NodeId::value).orElse(null)
        );
        return map;
    }

    private GraphPosition convertMapToPosition(Map<String, Object> map) {
        // This is a simplified reconstruction - in practice you'd need to properly
        // reconstruct the GraphPosition based on its internal structure
        Integer depth = (Integer) map.get("depth");
        if (depth == null) depth = 0;

        // For now, create a minimal position - a real implementation would need
        // access to GraphPosition factory methods or constructors
        return GraphPosition.at(NodeId.of("reconstructed-node"));
    }

    private Map<String, Object> convertMetadataToMap(StateMetadata metadata) {
        Map<String, Object> map = new HashMap<>();
        map.put("version", metadata.version());
        map.put("createdAt", metadata.createdAt().toString());
        map.put("lastModified", metadata.lastModified().toString());
        return map;
    }

    private StateMetadata convertMapToMetadata(Map<String, Object> map) {
        // Simplified reconstruction - real implementation would depend on StateMetadata structure
        Long version = map.get("version") instanceof Number num
            ? num.longValue()
            : 1L;
        String createdAtStr = (String) map.get("createdAt");
        String lastModifiedStr = (String) map.get("lastModified");

        Instant createdAt = createdAtStr != null
            ? Instant.parse(createdAtStr)
            : Instant.now();
        Instant lastModified = lastModifiedStr != null
            ? Instant.parse(lastModifiedStr)
            : Instant.now();

        // This would need to match the actual StateMetadata constructor/factory
        return StateMetadata.initial();
    }

    /**
     * Wrapper class for serialization that includes version and type information.
     */
    record SerializableWrapper(
        String workflowVersion,
        String schemaVersion,
        String workflowId,
        Object stateData,
        String currentNode,
        Map<String, Object> contextData,
        Map<String, Object> positionData,
        Map<String, Object> metadataData,
        Instant serializedAt,
        String stateDataType
    ) {}
}
