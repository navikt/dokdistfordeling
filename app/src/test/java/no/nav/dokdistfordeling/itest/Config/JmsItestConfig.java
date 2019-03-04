package no.nav.dokdistfordeling.itest.Config;


import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile("itest")
public class JmsItestConfig {

	@Bean(name = "qdist008")
	public Queue qdist008(@Value("${dokdistfordeling_qdist008_dist_forsendels.queuename}") String qdist008QueueName) {
		return new ActiveMQQueue(qdist008QueueName);
	}

	@Bean(name = "qdist008FunksjonellFeilQueue")
	public Queue qdist008FunksjonellFeilQueue(@Value("${dokdistfordeling_qdist008_funk_feil.queuename}") String qdist008FunksjonellFeil) {
		return new ActiveMQQueue(qdist008FunksjonellFeil);
	}

	@Bean(name = "qdist008TekniskFeilQueue")
	public Queue qdist008TekniskFeilQueue(@Value("${dokdistfordeling_qdist008_tekn_feil.queuename}") String qdist008TekniskFeil) {
		return new ActiveMQQueue(qdist008TekniskFeil);
	}

	@Bean(name = "qdist008ResultQueue")
	public Queue resultQueue(@Value("${dokdistfordeling_qdist008_result.queuename}") String qdist008Result) {
		return new ActiveMQQueue(qdist008Result);
	}

	@Bean
	public Queue backoutQueue() {
		return new ActiveMQQueue("ActiveMQ.DLQ");
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public BrokerService broker() throws Exception {
		BrokerService service = new BrokerService();
		service.setPersistent(false);

		return service;
	}

	@Bean
	public ConnectionFactory activemqConnectionFactory() {
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("vm://localhost?create=false");
		RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
		redeliveryPolicy.setMaximumRedeliveries(0);
		activeMQConnectionFactory.setRedeliveryPolicy(redeliveryPolicy);
		return activeMQConnectionFactory;
	}
}

