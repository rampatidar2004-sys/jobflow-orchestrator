package com.jobflow.orchestrator.workflow;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates a workflow's step graph before it is persisted:
 * <ol>
 *   <li>no two steps share a step key (duplicate-step validation)</li>
 *   <li>every {@code dependsOn} reference points at a step key that exists
 *       in the same workflow (unknown-dependency validation)</li>
 *   <li>the dependency graph is acyclic, verified with Kahn's algorithm —
 *       repeatedly removing zero-indegree nodes; if nodes remain once no
 *       more can be removed, a cycle exists</li>
 * </ol>
 */
@Component
public class WorkflowDagValidator {

    public record StepSpec(String stepKey, Set<String> dependsOn) {
    }

    public static class WorkflowValidationException extends RuntimeException {
        public WorkflowValidationException(String message) {
            super(message);
        }
    }

    /**
     * @return the steps in a valid topological order (useful for display /
     *         deterministic seeding); throws if the graph is invalid.
     */
    public List<String> validateAndTopoSort(List<StepSpec> steps) {
        validateNoDuplicateKeys(steps);
        validateNoUnknownDependencies(steps);
        return topoSortOrThrowOnCycle(steps);
    }

    private void validateNoDuplicateKeys(List<StepSpec> steps) {
        Set<String> seen = new HashSet<>();
        for (StepSpec step : steps) {
            if (!seen.add(step.stepKey())) {
                throw new WorkflowValidationException("duplicate step key: '" + step.stepKey() + "'");
            }
        }
    }

    private void validateNoUnknownDependencies(List<StepSpec> steps) {
        Set<String> known = new HashSet<>();
        for (StepSpec step : steps) {
            known.add(step.stepKey());
        }
        for (StepSpec step : steps) {
            for (String dep : step.dependsOn()) {
                if (!known.contains(dep)) {
                    throw new WorkflowValidationException(
                            "step '" + step.stepKey() + "' depends on unknown step '" + dep + "'");
                }
                if (dep.equals(step.stepKey())) {
                    throw new WorkflowValidationException(
                            "step '" + step.stepKey() + "' cannot depend on itself");
                }
            }
        }
    }

    /** Kahn's algorithm: repeatedly peel off nodes with zero remaining indegree. */
    private List<String> topoSortOrThrowOnCycle(List<StepSpec> steps) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>(); // dep -> steps that depend on it

        for (StepSpec step : steps) {
            indegree.putIfAbsent(step.stepKey(), 0);
            dependents.putIfAbsent(step.stepKey(), new ArrayList<>());
        }
        for (StepSpec step : steps) {
            indegree.put(step.stepKey(), step.dependsOn().size());
            for (String dep : step.dependsOn()) {
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(step.stepKey());
            }
        }

        Deque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.add(e.getKey());
            }
        }

        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String node = queue.poll();
            order.add(node);
            for (String dependent : dependents.getOrDefault(node, List.of())) {
                int updated = indegree.merge(dependent, -1, Integer::sum);
                if (updated == 0) {
                    queue.add(dependent);
                }
            }
        }

        if (order.size() != steps.size()) {
            Set<String> unresolved = new HashSet<>(indegree.keySet());
            unresolved.removeAll(order);
            throw new WorkflowValidationException(
                    "workflow contains a dependency cycle involving step(s): " + unresolved);
        }

        return order;
    }
}
