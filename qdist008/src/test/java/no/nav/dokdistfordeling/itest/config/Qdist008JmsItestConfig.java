package no.nav.dokdistfordeling.itest.config;


import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Queue;

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
	public Queue qdist008Bq() {
		return new ActiveMQQueue("qdist008Bq");
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
	public Queue qdist013(@Value("${dokdisteformidling_qdist013_dist_trygderetten.queuename}") String qdist013QueueName) {
		return new ActiveMQQueue(qdist013QueueName);
	}

	@Bean
	public Queue qdist015(@Value("${dokdistdpo_qdist015_dist_til_dpo.queuename}") String qdist015QueueName) {
		return new ActiveMQQueue(qdist015QueueName);
	}

	@Bean
	public Queue qdist016(@Value("${dokdistdpv_qdist016_dist_til_dpv.queuename}") String qdist016QueueName) {
		return new ActiveMQQueue(qdist016QueueName);
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public EmbeddedActiveMQ activeMQServer() {
		EmbeddedActiveMQ embeddedActiveMQ = new EmbeddedActiveMQ();
		embeddedActiveMQ.setConfigResourcePath("artemis-server.xml");
		return embeddedActiveMQ;
	}

	// avhengig av EmbeddedActiveMQ slik at server er startet før klient forsøker lage koblinger
	@Bean
	public ConnectionFactory activemqConnectionFactory(EmbeddedActiveMQ embeddedActiveMQ) {
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("vm://0");
		JmsPoolConnectionFactory pooledFactory = new JmsPoolConnectionFactory();
		pooledFactory.setConnectionFactory(activeMQConnectionFactory);
		pooledFactory.setMaxConnections(1);
		return pooledFactory;
	}
}

