package no.nav.dokdistfordeling.itest.config;


import com.ibm.mq.jms.MQQueue;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile("itest")
public class Qdist008JmsItestConfig {

	@Bean
	public Queue qdist008(@Value("${dokdistfordeling_qdist008_dist_forsendels.queuename}") String qdist008QueueName) {
		return new ActiveMQQueue(qdist008QueueName);
	}

	@Bean
	public Queue qdist008FunksjonellFeil(@Value("${dokdistfordeling_qdist008_funk_feil.queuename}") String qdist008FunksjonellFeil) {
		return new ActiveMQQueue(qdist008FunksjonellFeil);
	}

	@Bean
	public Queue qdist009(@Value("${dokdistsentralprint_qdist009_dist_s_print.queuename}") String qdist009QueueName) {
		return new ActiveMQQueue(qdist009QueueName);
	}

	@Bean
	public Queue qdist010(@Value("${dokdistdittnav_qdist010_dist_ditt_nav.queuename}") String qdist010QueueName) {
		return new ActiveMQQueue(qdist010QueueName);
	}

	@Bean
	public Queue qdist011(@Value("${dokdistdpi_qdist011_dist_til_dpi.queuename}") String qdist011QueueName) {
		return new ActiveMQQueue(qdist011QueueName);
	}

	@Bean
	public Queue qdist012(@Value("${dokdistfordeling_qdist012_hent_dok_joark.queuename}") String qdist012QueueName) {
		return new ActiveMQQueue(qdist012QueueName);
	}

	@Bean
	public Queue qdist012FunksjonellFeil(@Value("${dokdistfordeling_qdist012_funk_feil.queuename}") String qdist012FunksjonellFeilQueueName) {
		return new ActiveMQQueue(qdist012FunksjonellFeilQueueName);
	}

	@Bean
	public Queue qdist013(@Value("${dokdisteformidling_qdist013_dist_trygderetten.queuename}") String qdist013QueueName) {
		return new ActiveMQQueue(qdist013QueueName);
	}

	@Bean
	public Queue qdist016(@Value("${dokdistdpv_qdist016_dist_til_dpv.queuename}") String qdist016QueueName) throws JMSException {
		return new MQQueue(qdist016QueueName);
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
		PooledConnectionFactory pooledFactory = new PooledConnectionFactory();
		pooledFactory.setConnectionFactory(activeMQConnectionFactory);
		pooledFactory.setMaxConnections(1);
		return pooledFactory;
	}
}

