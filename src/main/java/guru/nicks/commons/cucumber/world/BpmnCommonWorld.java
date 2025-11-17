package guru.nicks.commons.cucumber.world;

import io.cucumber.spring.ScenarioScope;
import lombok.Data;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

/**
 * Domain-specific (not feature-specific) state shared between scenario steps. Thanks to
 * {@link ScenarioScope @ScenarioScope}, each scenario gets a fresh copy.
 */
@Component
@ScenarioScope
@Data
public class BpmnCommonWorld {

    private String processDefinitionFile;
    private String processDefinitionKey;
    private String processBusinessKey;

    private ProcessInstance process;

}
