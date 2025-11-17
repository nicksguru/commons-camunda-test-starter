package guru.nicks.commons.cucumber.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;

@Component
public class ValidateFormDelegate extends AbstractDelegate {

    @Override
    protected void run(DelegateExecution execution) {
        // null/'' are both accepted here because they're not numbers
        String value = getVariable(execution, "orderTotal", String.class, false);
        boolean formValid;

        try {
            // the number of decimal positions must be limited by 2, with Banker's rounding to minimize losses
            var money = BigDecimal
                    .valueOf(Double.parseDouble(value))
                    .setScale(2, RoundingMode.HALF_EVEN);
            check(money.signum(), "order total").positiveOrZero();
            formValid = true;
        }
        // formally it's NullPointerException or NumberFormatException, but let's be more generic
        catch (Exception e) {
            formValid = false;
        }

        execution.setVariable("isFormValid", formValid);
    }

}
