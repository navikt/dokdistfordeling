//package no.nav.naismal.nais.checks;
//
//import no.nav.naismal.nais.selftest.AbstractDependencyCheck;
//import no.nav.naismal.nais.selftest.ApplicationNotReadyException;
//import no.nav.naismal.nais.selftest.DependencyType;
//import no.nav.naismal.nais.selftest.Importance;
//import org.springframework.stereotype.Component;
//@Component
//public class ExampleCheck extends AbstractDependencyCheck {
//
//    private final Consumer consumer;
//
//    public ExampleCheck(Consumer consumer) {
//        super(DependencyType type, String name, String address, Importance importance);
//        this.consumer = consumer;
//    }
//
//
//    @Override
//    protected void doCheck() {
//        try {
//            consumer.ping();
//        } catch (Exception e) {
//            throw new ApplicationNotReadyException(String.format("Calling [name] failed. errorMessage=%s", getErrorMessage(e)), e);
//        }
//    }
//
//
//}
