package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.delegate.ValidateFormDelegate;
import guru.nicks.commons.cucumber.world.BpmnCommonWorld;
import guru.nicks.commons.test.PostgreSqlContainerRunner;

import io.cucumber.spring.CucumberContextConfiguration;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Initializes Spring Context shared by all scenarios. Mocking is done inside step definition classes to let each
 * scenario program a different behavior. However, purely default mocks can be declared here (using annotations), but
 * remember to not alter their behavior in step classes.
 * <p>
 * Please keep in mind that mocked Spring beans ({@link MockitoBean @MockitoBean}) declared in step definition classes
 * conflict with each other because all the steps are part of the same test suite i.e. Spring context. POJO mocks
 * ({@link Mock @Mock}) do not conflict with each other.
 */
@CucumberContextConfiguration
@ContextConfiguration(classes = {
        // scenario-scoped states
        BpmnCommonWorld.class,
        // delegate beans
        ValidateFormDelegate.class
}, initializers = PostgreSqlContainerRunner.class)
@TestPropertySource(properties = {
        //"logging.level.root=DEBUG",

        // needed for component scanners, such as Spring Data
        "app.rootPackage=guru.nicks",

        // disable async jobs, otherwise Camunda tests will stop at the first async boundary
        "camunda.bpm.job-execution.enabled.false=",
        "camunda.bpm.generic-properties.properties.historyTimeToLive=P1D"
})
@Import({CamundaBpmAutoConfiguration.class})
@DataJpaTest(excludeAutoConfiguration = TestDatabaseAutoConfiguration.class)
@EnableJpaRepositories(basePackages = "${app.rootPackage}")
@EnableTransactionManagement
public class CucumberBootstrap {

    /**
     * Real bean. Needed for {@link BpmnSteps#validateFormDelegateWasCalled()}.
     */
    @MockitoSpyBean
    private ValidateFormDelegate validateFormDelegate;

    /*
     * It's not mandatory to instantiate all Java delegate beans. Feel free to mock any of them. However, this state
     * (real bean / mocked bean) is the same for ALL Cucumber tests.
     @MockitoBean private SomeOtherDelegate mockedBean;
     */

}
