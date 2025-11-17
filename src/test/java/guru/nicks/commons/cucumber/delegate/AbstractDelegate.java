package guru.nicks.commons.cucumber.delegate;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

@Slf4j
public abstract class AbstractDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        try {
            run(execution);
        }
        // WARNING: wrapping everything in BpmnError blocks retries in Camunda (depleted retries are shown as incidents
        // in Camunda Cockpit). On the other hand, only BpmnError's are routed to error event subprocesses and boundary
        // error event listeners.
        catch (Exception e) {
            // TODO: possibly send email / to some messenger / ...
            log.error("Delegate fatal error: {}", e.getMessage(), e);
            throw new BpmnError("TODO_ADD_SOME_BUSINESS_ERROR_CODE", e.getMessage(), e);
        }
    }

    protected abstract void run(DelegateExecution execution);

    /**
     * Retrieves process variable.
     *
     * @param execution    current process execution
     * @param variableName variable name
     * @param clazz        variable class
     * @param required     if {@code true}, strings must be non-blank, other objects must not be {@code null}
     * @param <T>          variable type
     * @return variable value
     * @throws NullPointerException     if variable is {@code null} (only if {@code required} is {@code true})
     * @throws IllegalArgumentException if variable is {@code null} (only if {@code required} is {@code true})
     */
    protected <T> T getVariable(DelegateExecution execution, String variableName, Class<T> clazz, boolean required) {
        T value = clazz.cast(execution.getVariable(variableName));

        if (required) {
            if (value instanceof String) {
                Validate.notBlank((String) value, "Process variable '" + variableName + "' is a blank string");
            } else {
                Validate.notNull(value, "Process variable '" + variableName + "' is null");
            }
        }

        return value;
    }

}
