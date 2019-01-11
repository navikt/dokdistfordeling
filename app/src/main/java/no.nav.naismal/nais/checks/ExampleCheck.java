package no.nav.naismal.nais.checks;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.naismal.nais.selftest.AbstractDependencyCheck;
import no.nav.naismal.nais.selftest.ApplicationNotReadyException;
import no.nav.naismal.nais.selftest.DependencyType;
import no.nav.naismal.nais.selftest.Importance;
import org.springframework.stereotype.Component;
@Component
public class ExampleCheck extends AbstractDependencyCheck {


    public ExampleCheck(MeterRegistry registry) {
        super(DependencyType.REST, "exampleConsumer", "exampleEndpoint", Importance.WARNING, registry);
    }

    @Override
    protected void doCheck() {
        try {
//            consumer.ping();
        } catch (Exception e) {
            throw new ApplicationNotReadyException(String.format("Calling [name] failed. errorMessage=%s", getErrorMessage(e)), e);
        }
    }


}
