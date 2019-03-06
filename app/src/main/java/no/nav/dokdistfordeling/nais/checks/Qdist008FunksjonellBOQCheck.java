package no.nav.dokdistfordeling.nais.checks;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistfordeling.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistfordeling.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistfordeling.nais.selftest.DependencyType;
import no.nav.dokdistfordeling.nais.selftest.Importance;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Queue;

@Component
public class Qdist008FunksjonellBOQCheck extends AbstractDependencyCheck {

	private final Queue qdist008FunksjonellFeil;
	private final JmsTemplate jmsTemplate;

	@Inject
	public Qdist008FunksjonellBOQCheck(MeterRegistry registry, Queue qdist008FunksjonellFeil, JmsTemplate jmsTemplate) throws JMSException {
		super(DependencyType.QUEUE, "qdist008FunksjonellFeilQueue", qdist008FunksjonellFeil.getQueueName(), Importance.CRITICAL, registry);
		this.qdist008FunksjonellFeil = qdist008FunksjonellFeil;
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	protected void doCheck() {
		try {
			checkQueue(qdist008FunksjonellFeil);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("JMS Queue Browser failed to get queue: " + qdist008FunksjonellFeil, e);
		}
	}

	private void checkQueue(final Queue queue) {
		jmsTemplate.browse(queue,
				(session, browser) -> {
					browser.getQueue();
					return null;
				}
		);
	}


}
