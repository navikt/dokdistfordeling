package no.nav.dokdistfordeling.itest.config;


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
public class Qdist012JmsItestConfig {

	@Bean
	public Queue qdist012(@Value("${dokdistfordeling_qdist012_hent_dok_fra_joark.queuename}") String qdist008QueueName) {
		return new ActiveMQQueue(qdist008QueueName);
	}

	@Bean
	public Queue qdist012FunksjonellFeil(@Value("${dokdistfordeling_qdist012_funk_feil.queuename}") String qdist008FunksjonellFeil) {
		return new ActiveMQQueue(qdist008FunksjonellFeil);
	}

	@Bean
	public Queue qdist008(@Value("${dokdistfordeling_qdist008_dist_forsendels.queuename}") String qdist008QueueName) {
		return new ActiveMQQueue(qdist008QueueName);
	}

	@Bean
	public Queue backoutQueue() {
		return new ActiveMQQueue("ActiveMQ.DLQ");
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public BrokerService broker() {
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

