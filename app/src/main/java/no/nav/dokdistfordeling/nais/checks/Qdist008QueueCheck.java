package no.nav.dokdistfordeling.nais.checks;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistfordeling.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistfordeling.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistfordeling.nais.selftest.DependencyType;
import no.nav.dokdistfordeling.nais.selftest.Importance;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import javax.jms.JMSException;
import javax.jms.Queue;

@Component
public class Qdist008QueueCheck extends AbstractDependencyCheck {

	private final Queue qdist008;
	private final JmsTemplate jmsTemplate;

	@Autowired
	public Qdist008QueueCheck(MeterRegistry registry, Queue qdist008, JmsTemplate jmsTemplate) throws JMSException {
		super(DependencyType.QUEUE, "Qdist008Queue", qdist008.getQueueName(), Importance.CRITICAL, registry);
		this.qdist008 = qdist008;
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	protected void doCheck() {
		try {
			checkQueue(qdist008);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("JMS Queue Browser failed to get queue: " + qdist008, e);
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
