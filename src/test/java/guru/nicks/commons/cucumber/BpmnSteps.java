package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.delegate.ValidateFormDelegate;

import io.cucumber.java.en.Then;
import io.cucumber.java.ru.То;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;

/**
 * @see BpmnCommonSteps
 */
@RequiredArgsConstructor
public class BpmnSteps {

    // DI
    private final ValidateFormDelegate validateFormDelegate;

    @То("делегат validateFormDelegate был вызван")
    @Then("delegate validateFormDelegate was called")
    public void validateFormDelegateWasCalled() throws Exception {
        Mockito.verify(validateFormDelegate, atLeast(1))
                .execute(any(DelegateExecution.class));
    }

    @То("делегат validateFormDelegate не был вызван")
    @Then("delegate validateFormDelegate was not called")
    public void validateFormDelegateWasNeverCalled() {
        Mockito.verifyNoInteractions(validateFormDelegate);
    }

}
