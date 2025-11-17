package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.BpmnCommonWorld;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.ru.Дано;
import io.cucumber.java.ru.Если;
import io.cucumber.java.ru.То;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.OptimisticLockingException;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;

import java.util.List;
import java.util.Map;

/**
 * WARNING: Camunda officially claims that DB deadlocks pop up if the DB isolation level is set to REPEATABLE READ. This
 * is the default for MySQL (but not for PostgreSQL - they have READ_COMMITTED). Therefore to run Camunda with MySQL,
 * the isolation level must be set to READ COMMITTED manually, which may interfere with the app's business logic.
 */
@RequiredArgsConstructor
@Slf4j
public class BpmnCommonSteps {

    // DI
    private final BpmnCommonWorld bpmnCommonWorld;

    /**
     * Loads BPMN file from classpath, e.g. 'bpmn/TestProcess.bpmn' means 'src/main/resources/bpmn/TestProcess.bpmn'.
     *
     * @param filename filename relative to classpath
     */
    @Дано("загрузить определение процесса {string} из файла {string}")
    @Given("load process definition {string} from file {string}")
    public void loadProcessDefinitionFromFile(String processDefinitionKey, String filename) {
        BpmnAwareTests
                .repositoryService()
                .createDeployment()
                .addClasspathResource(filename)
                .deploy();
        bpmnCommonWorld.setProcessDefinitionFile(filename);
        bpmnCommonWorld.setProcessDefinitionKey(processDefinitionKey);
    }

    @Дано("бизнес-ключ процесса {string}")
    @Given("process business key is {string}")
    public void setProcessBusinessKey(String processBusinessKey) {
        bpmnCommonWorld.setProcessBusinessKey(processBusinessKey);
    }

    @Если("запустить процесс")
    @When("start process")
    public void startProcess() {
        ProcessInstance process = BpmnAwareTests
                .runtimeService()
                .startProcessInstanceByKey(
                        bpmnCommonWorld.getProcessDefinitionKey(),
                        bpmnCommonWorld.getProcessBusinessKey());

        BpmnAwareTests.assertThat(process).hasProcessDefinitionKey(bpmnCommonWorld.getProcessDefinitionKey());
        BpmnAwareTests.assertThat(process).hasBusinessKey(bpmnCommonWorld.getProcessBusinessKey());
        BpmnAwareTests.assertThat(process).isStarted();
        bpmnCommonWorld.setProcess(process);
    }

    @Если("запустить процесс с входными переменными:")
    @When("start process with variables:")
    public void startProcessWithVariables(Map<String, Object> variables) {
        ProcessInstance process = BpmnAwareTests
                .runtimeService()
                .startProcessInstanceByKey(
                        bpmnCommonWorld.getProcessDefinitionKey(),
                        bpmnCommonWorld.getProcessBusinessKey(),
                        variables);

        BpmnAwareTests.assertThat(process).hasProcessDefinitionKey(bpmnCommonWorld.getProcessDefinitionKey());
        BpmnAwareTests.assertThat(process).hasBusinessKey(bpmnCommonWorld.getProcessBusinessKey());
        BpmnAwareTests.assertThat(process).isStarted();
        bpmnCommonWorld.setProcess(process);
    }

    /**
     * Sends message to process described with {@link BpmnCommonWorld#getProcessBusinessKey()}.
     *
     * @param messageName message name
     */
    @Если("послать в процесс сообщение {string}")
    @When("send process message {string}")
    public void sendMessageToProcess(String messageName) {
        BpmnAwareTests
                .runtimeService()
                .createMessageCorrelation(messageName)
                .processInstanceId(bpmnCommonWorld.getProcess().getId())
                .correlate();
    }

    /**
     * This method assumes job execution is disabled in app properties ({@code camunda.bpm.job-execution.enabled=false})
     * , therefore all async jobs (timers, service tasks with async-before) do NOT run without an explicit 'kick' (this
     * is what this method does). Therefore, if this method actually checks the current process location and then
     * ADVANCES THE PROCESS, i.e. runs Java delegates etc.
     * <p>
     * Also, this method ignores {@link OptimisticLockingException} resulting from {@link BpmnAwareTests#execute(Job)}
     * because it corresponds to retries and normally happens 1-2 times during test execution (concurrent
     * deletion/update of message/history records etc.).
     */
    @То("процесс ожидает в точке {string}")
    @Then("process awaits at activity {string}")
    public void processWaitsAt(String activityId) {
        BpmnAwareTests.assertThat(bpmnCommonWorld.getProcess())
                .isWaitingAt(activityId);

        // execute all pertinent jobs, such as: boundary timers, service tasks with async-before
        BpmnAwareTests
                .managementService()
                .createJobQuery()
                .unlimitedList()
                .forEach(job -> {
                    try {
                        BpmnAwareTests.execute(job);
                    } catch (OptimisticLockingException e) {
                        log.warn("Ignoring [{}]: {}", e.getClass().getName(), e.getMessage(), e);
                    }
                });
    }

    @То("процесс не завершается")
    @Then("process does not end")
    public void processNotEnded() {
        BpmnAwareTests.assertThat(bpmnCommonWorld.getProcess())
                .isNotEnded();
    }

    @То("процесс завершается")
    @Then("process ends")
    public void processEnded() {
        BpmnAwareTests.assertThat(bpmnCommonWorld.getProcess())
                .isEnded();
    }

    /**
     * WARNING: this step submits the LAST (as per {@link TaskService#createTaskQuery()}) user task.
     *
     * @param variables user input
     */
    @Если("юзер заполняет форму:")
    @When("user fills in form:")
    public void userEntersValue(Map<String, Object> variables) {
        List<Task> userTasks = BpmnAwareTests
                .taskService()
                .createTaskQuery()
                //.processInstanceId(bpmnWorld.getProcess().getId())
                .processInstanceBusinessKey(bpmnCommonWorld.getProcessBusinessKey())
                .list();
        Task lastTask = userTasks.getLast();

        BpmnAwareTests.taskService()
                .complete(lastTask.getId(), variables);
    }

    @То("переменная процесса {string} равна {string}")
    @Then("process variable {string} is {string}")
    public void processVariableEquals(String name, String value) {
        BpmnAwareTests
                .assertThat(bpmnCommonWorld.getProcess())
                .variables()
                .extractingByKey(name)
                .asString()
                .isEqualTo(value);
    }

    @То("переменная процесса {string} равна null")
    @Then("process variable {string} is null")
    public void processVariableEqualsNull(String name) {
        BpmnAwareTests
                .assertThat(bpmnCommonWorld.getProcess())
                .variables()
                .extractingByKey(name)
                .isNull();
    }

    @То("процесс побывал в точках:")
    @Then("process visited activities:")
    public void processPassed(List<String> activityIds) {
        BpmnAwareTests.assertThat(bpmnCommonWorld.getProcess())
                .hasPassed(activityIds.toArray(new String[0]));
    }

}
