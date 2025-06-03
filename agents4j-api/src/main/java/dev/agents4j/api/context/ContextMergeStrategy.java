package dev.agents4j.api.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Strategy for merging workflow contexts during resume operations.
 * Defines how to handle conflicts when the resume context contains
 * values that differ from the suspended context.
 */
public enum ContextMergeStrategy {
    /**
     * Resume context values override suspended context values.
     * New keys from resume context are added.
     * No conflicts are reported.
     */
    RESUME_WINS {
        @Override
        public ContextMergeResult merge(
            WorkflowContext suspended,
            WorkflowContext resume
        ) {
            WorkflowContext merged = suspended.merge(resume);
            return new ContextMergeResult(merged, List.of(), List.of());
        }
    },

    /**
     * Suspended context values take precedence over resume context values.
     * Only new keys from resume context that don't exist in suspended context are added.
     */
    SUSPENDED_WINS {
        @Override
        public ContextMergeResult merge(
            WorkflowContext suspended,
            WorkflowContext resume
        ) {
            WorkflowContext merged = suspended;
            List<ContextConflict> conflicts = new ArrayList<>();

            // Add only non-conflicting keys from resume context
            for (ContextKey<?> key : resume.keys()) {
                if (!suspended.contains(key)) {
                    merged = addToContext(merged, key, resume);
                } else {
                    // Record conflict but keep suspended value
                    Object suspendedValue = suspended.get(key).orElse(null);
                    Object resumeValue = resume.get(key).orElse(null);
                    if (!Objects.equals(suspendedValue, resumeValue)) {
                        conflicts.add(
                            new ContextConflict(
                                key,
                                suspendedValue,
                                resumeValue,
                                suspendedValue
                            )
                        );
                    }
                }
            }

            return new ContextMergeResult(merged, conflicts, List.of());
        }
    },

    /**
     * Merge non-conflicting values, but fail if conflicts are detected.
     * A conflict occurs when both contexts have the same key with different values.
     */
    MERGE_SAFE {
        @Override
        public ContextMergeResult merge(
            WorkflowContext suspended,
            WorkflowContext resume
        ) {
            WorkflowContext merged = suspended;
            List<ContextConflict> conflicts = new ArrayList<>();

            for (ContextKey<?> key : resume.keys()) {
                if (!suspended.contains(key)) {
                    // No conflict, add the key
                    merged = addToContext(merged, key, resume);
                } else {
                    // Check for conflicts
                    Object suspendedValue = suspended.get(key).orElse(null);
                    Object resumeValue = resume.get(key).orElse(null);

                    if (!Objects.equals(suspendedValue, resumeValue)) {
                        conflicts.add(
                            new ContextConflict(
                                key,
                                suspendedValue,
                                resumeValue,
                                null
                            )
                        );
                    }
                }
            }

            return new ContextMergeResult(merged, conflicts, List.of());
        }
    },

    /**
     * Latest (resume) values win, but conflicts are logged as warnings.
     * All keys from both contexts are included in the final result.
     */
    MERGE_LATEST {
        @Override
        public ContextMergeResult merge(
            WorkflowContext suspended,
            WorkflowContext resume
        ) {
            WorkflowContext merged = suspended.merge(resume);
            List<ContextConflict> conflicts = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            for (ContextKey<?> key : resume.keys()) {
                if (suspended.contains(key)) {
                    Object suspendedValue = suspended.get(key).orElse(null);
                    Object resumeValue = resume.get(key).orElse(null);

                    if (!Objects.equals(suspendedValue, resumeValue)) {
                        conflicts.add(
                            new ContextConflict(
                                key,
                                suspendedValue,
                                resumeValue,
                                resumeValue
                            )
                        );
                        warnings.add(
                            String.format(
                                "Context conflict for key '%s': suspended=%s, resume=%s (using resume value)",
                                key.name(),
                                suspendedValue,
                                resumeValue
                            )
                        );
                    }
                }
            }

            return new ContextMergeResult(merged, conflicts, warnings);
        }
    };

    /**
     * Merges two workflow contexts according to this strategy.
     *
     * @param suspended The context from the suspended workflow
     * @param resume The context provided during resume
     * @return The merge result containing the merged context and any conflicts
     */
    public abstract ContextMergeResult merge(
        WorkflowContext suspended,
        WorkflowContext resume
    );

    /**
     * Helper method to safely add a key-value pair to a context.
     */
    @SuppressWarnings("unchecked")
    private static <T> WorkflowContext addToContext(
        WorkflowContext context,
        ContextKey<T> key,
        WorkflowContext source
    ) {
        Optional<T> value = source.get(key);
        return value.map(t -> context.with(key, t)).orElse(context);
    }

    /**
     * Represents a conflict between suspended and resume context values.
     */
    public static final class ContextConflict {

        private final ContextKey<?> key;
        private final Object suspendedValue;
        private final Object resumeValue;
        private final Object resolvedValue;

        /**
         * Creates a new context conflict.
         * 
         * @param key the context key that has a conflict
         * @param suspendedValue the value from the suspended context
         * @param resumeValue the value from the resume context
         * @param resolvedValue the resolved value (null if unresolved)
         */
        public ContextConflict(
            ContextKey<?> key,
            Object suspendedValue,
            Object resumeValue,
            Object resolvedValue
        ) {
            this.key = Objects.requireNonNull(key, "Key cannot be null");
            this.suspendedValue = suspendedValue;
            this.resumeValue = resumeValue;
            this.resolvedValue = resolvedValue;
        }

        /**
         * Gets the context key that has a conflict.
         * 
         * @return the context key
         */
        public ContextKey<?> getKey() {
            return key;
        }

        /**
         * Gets the value from the suspended context.
         * 
         * @return the suspended value
         */
        public Object getSuspendedValue() {
            return suspendedValue;
        }

        /**
         * Gets the value from the resume context.
         * 
         * @return the resume value
         */
        public Object getResumeValue() {
            return resumeValue;
        }

        /**
         * Gets the resolved value for this conflict.
         * 
         * @return the resolved value, or null if unresolved
         */
        public Object getResolvedValue() {
            return resolvedValue;
        }

        /**
         * Checks if this conflict has been resolved.
         * 
         * @return true if the conflict is resolved, false otherwise
         */
        public boolean isResolved() {
            return resolvedValue != null;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            ContextConflict that = (ContextConflict) obj;
            return (
                Objects.equals(key, that.key) &&
                Objects.equals(suspendedValue, that.suspendedValue) &&
                Objects.equals(resumeValue, that.resumeValue) &&
                Objects.equals(resolvedValue, that.resolvedValue)
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                key,
                suspendedValue,
                resumeValue,
                resolvedValue
            );
        }

        @Override
        public String toString() {
            return String.format(
                "ContextConflict{key=%s, suspended=%s, resume=%s, resolved=%s}",
                key.name(),
                suspendedValue,
                resumeValue,
                resolvedValue
            );
        }
    }

    /**
     * Result of a context merge operation.
     */
    public static final class ContextMergeResult {

        private final WorkflowContext mergedContext;
        private final List<ContextConflict> conflicts;
        private final List<String> warnings;

        /**
         * Creates a new context merge result.
         * 
         * @param mergedContext the merged workflow context
         * @param conflicts list of conflicts found during merge
         * @param warnings list of warnings generated during merge
         */
        public ContextMergeResult(
            WorkflowContext mergedContext,
            List<ContextConflict> conflicts,
            List<String> warnings
        ) {
            this.mergedContext = Objects.requireNonNull(
                mergedContext,
                "Merged context cannot be null"
            );
            this.conflicts = List.copyOf(
                Objects.requireNonNull(
                    conflicts,
                    "Conflicts list cannot be null"
                )
            );
            this.warnings = List.copyOf(
                Objects.requireNonNull(warnings, "Warnings list cannot be null")
            );
        }

        /**
         * Gets the merged workflow context.
         * 
         * @return the merged context
         */
        public WorkflowContext getMergedContext() {
            return mergedContext;
        }

        /**
         * Gets the list of conflicts found during merge.
         * 
         * @return the conflicts list
         */
        public List<ContextConflict> getConflicts() {
            return conflicts;
        }

        /**
         * Gets the list of warnings generated during merge.
         * 
         * @return the warnings list
         */
        public List<String> getWarnings() {
            return warnings;
        }

        /**
         * Checks if there are any conflicts.
         * 
         * @return true if conflicts exist, false otherwise
         */
        public boolean hasConflicts() {
            return !conflicts.isEmpty();
        }

        /**
         * Checks if there are any warnings.
         * 
         * @return true if warnings exist, false otherwise
         */
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        /**
         * Checks if there are any unresolved conflicts.
         * 
         * @return true if any conflicts remain unresolved, false otherwise
         */
        public boolean hasUnresolvedConflicts() {
            return conflicts
                .stream()
                .anyMatch(conflict -> !conflict.isResolved());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            ContextMergeResult that = (ContextMergeResult) obj;
            return (
                Objects.equals(mergedContext, that.mergedContext) &&
                Objects.equals(conflicts, that.conflicts) &&
                Objects.equals(warnings, that.warnings)
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(mergedContext, conflicts, warnings);
        }

        @Override
        public String toString() {
            return String.format(
                "ContextMergeResult{conflicts=%d, warnings=%d, contextSize=%d}",
                conflicts.size(),
                warnings.size(),
                mergedContext.size()
            );
        }
    }
}
