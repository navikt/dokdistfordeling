
package no.nav.dokdistfordeling.config.jms;

import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import com.ibm.mq.jakarta.jms.MQQueue;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;

import javax.net.ssl.SSLSocketFactory;

@Profile({"nais", "local"})
@Configuration
public class JmsConfig {

	private static final int UTF_8_WITH_PUA = 1208;
	private static final String ANY_TLS13_OR_HIGHER = "*TLS13ORHIGHER";

	@Bean
	public Queue qdist008(@Value("${dokdistfordeling_qdist008_dist_forsendels.queuename}") String qdist008QueueName) throws JMSException {
		return new MQQueue(qdist008QueueName);
	}

	@Bean
	public Queue qdist008FunksjonellFeil(@Value("${dokdistfordeling_qdist008_funk_feil.queuename}") String qdist008FunksjonellFeilQueueName) throws JMSException {
		return new MQQueue(qdist008FunksjonellFeilQueueName);
	}

	@Bean
	public Queue qdist009(@Value("${dokdistsentralprint_qdist009_dist_s_print.queuename}") String qdist009QueueName) throws JMSException {
		return new MQQueue(qdist009QueueName);
	}

	@Bean
	public Queue qdist010(@Value("${dokdistdittnav_qdist010_dist_ditt_nav.queuename}") String qdist010QueueName) throws JMSException {
		return new MQQueue(qdist010QueueName);
	}

	@Bean
	public Queue qdist011(@Value("${dokdistdpi_qdist011_dist_til_dpi.queuename}") String qdist011QueueName) throws JMSException {
		return new MQQueue(qdist011QueueName);
	}

	@Bean
	public Queue qdist012(@Value("${dokdistfordeling_qdist012_hent_dok_joark.queuename}") String qdist012QueueName) throws JMSException {
		return new MQQueue(qdist012QueueName);
	}

	@Bean
	public Queue qdist012FunksjonellFeil(@Value("${dokdistfordeling_qdist012_funk_feil.queuename}") String qdist012FunksjonellFeilQueueName) throws JMSException {
		return new MQQueue(qdist012FunksjonellFeilQueueName);
	}

	@Bean
	public Queue qdist013(@Value("${dokdisteformidling_qdist013_dist_trygderetten.queuename}") String qdist013QueueName) throws JMSException {
		return new MQQueue(qdist013QueueName);
	}

	@Bean
	public Queue qdist015(@Value("${dokdistdpo_qdist015_dist_til_dpo.queuename}") String qdist015QueueName) throws JMSException {
		return new MQQueue(qdist015QueueName);
	}

	@Bean
	public Queue qdist016(@Value("${dokdistdpv_qdist016_dist_til_dpv.queuename}") String qdist016QueueName) throws JMSException {
		return new MQQueue(qdist016QueueName);
	}

	@Bean
	public ConnectionFactory connectionFactory(final MqGatewayAlias mqGatewayAlias,
											   final DokdistfordelingProperties dokdistfordelingProperties) throws JMSException {
		return createConnectionFactory(mqGatewayAlias, dokdistfordelingProperties.getServiceuser());
	}

	private JmsPoolConnectionFactory createConnectionFactory(final MqGatewayAlias mqGatewayAlias,
															 final DokdistfordelingProperties.Serviceuser serviceuser) throws JMSException {
		MQConnectionFactory connectionFactory = new MQConnectionFactory();
		connectionFactory.setHostName(mqGatewayAlias.getHostname());
		connectionFactory.setPort(mqGatewayAlias.getPort());
		connectionFactory.setQueueManager(mqGatewayAlias.getName());
		connectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
		connectionFactory.setCCSID(UTF_8_WITH_PUA);
		connectionFactory.setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQConstants.MQENC_NATIVE);
		connectionFactory.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA);

		connectionFactory.setSSLCipherSuite(ANY_TLS13_OR_HIGHER);
		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		connectionFactory.setSSLSocketFactory(factory);
		connectionFactory.setChannel(mqGatewayAlias.getChannel().getSecurename());

		UserCredentialsConnectionFactoryAdapter adapter = new UserCredentialsConnectionFactoryAdapter();
		adapter.setTargetConnectionFactory(connectionFactory);
		adapter.setUsername(serviceuser.getUsername());
		adapter.setPassword(serviceuser.getPassword());

		JmsPoolConnectionFactory pooledFactory = new JmsPoolConnectionFactory();
		pooledFactory.setConnectionFactory(adapter);
		pooledFactory.setMaxConnections(10);
		pooledFactory.setMaxSessionsPerConnection(10);
		return pooledFactory;
	}
}
