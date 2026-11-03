package com.jobflow.orchestrator.taskhandler;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Discovers every {@link TaskHandler} bean in the application context and
 * indexes it by {@link TaskHandler#taskType()}. This is the sole extension
 * point for new task types — implement the interface, register the bean,
 * and it is picked up here with no other wiring required.
 */
@Component
public class TaskHandlerRegistry {

    private final Map<String, TaskHandler> handlersByType;

    public TaskHandlerRegistry(List<TaskHandler> handlers) {
        this.handlersByType = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(TaskHandler::taskType, h -> h));
    }

    public Optional<TaskHandler> find(String taskType) {
        return Optional.ofNullable(handlersByType.get(taskType));
    }

    public boolean isKnownType(String taskType) {
        return handlersByType.containsKey(taskType);
    }

    public List<String> registeredTypes() {
        return List.copyOf(handlersByType.keySet());
    }
}
