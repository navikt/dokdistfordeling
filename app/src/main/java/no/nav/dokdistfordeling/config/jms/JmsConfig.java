
package no.nav.dokdistfordeling.config.jms;

import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import com.ibm.msg.client.wmq.WMQConstants;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.props.SrvAppserverProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;

/**
 * @author Sigurd Midttun, Visma Consulting AS
 */
@Configuration
@Profile("nais")
public class JmsConfig {

	private static final int UTF_8_WITH_PUA = 1208;

	@Bean(name="qdist008")
	public Queue qdist008(@Value("${dokdistfordeling_qdist008_dist_forsendels.queuename}") String qdist008QueueName) throws JMSException {
		return new MQQueue(qdist008QueueName);
	}

	@Bean(name="qdist008FunksjonellFeilQueue")
	public Queue qdist008FunksjonellFeil(@Value("${dokdistfordeling_qdist008_funk_feil.queuename}") String qdist008FunksjonellFeilQueueName) throws JMSException {
		return new MQQueue(qdist008FunksjonellFeilQueueName);
	}

//	Todo: Uncomment when created
//	@Bean
//	public Queue qdist008FunksjonellFeil(@Value("${dokdistfordeling_qdist008_funksjonell_feil.queuename}") String qdist008FunksjonellFeil) throws JMSException {
//		return new MQQueue(qdist008FunksjonellFeil);
//	}

	@Bean
	public ConnectionFactory wmqConnectionFactory(final MqGatewayAlias mqGatewayAlias,
												  final @Value("${dokdistfordeling_channel.name}") String channelName,
												  final SrvAppserverProperties srvAppserverProperties) throws JMSException {
		return createConnectionFactory(mqGatewayAlias, channelName, srvAppserverProperties);
	}

	private UserCredentialsConnectionFactoryAdapter createConnectionFactory(final MqGatewayAlias mqGatewayAlias,
																			final String channelName,
																			final SrvAppserverProperties srvAppserverProperties) throws JMSException {
		MQConnectionFactory connectionFactory = new MQConnectionFactory();
		connectionFactory.setHostName(mqGatewayAlias.getHostname());
		connectionFactory.setPort(mqGatewayAlias.getPort());
		connectionFactory.setChannel(channelName);
		connectionFactory.setQueueManager(mqGatewayAlias.getName());
		connectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
		connectionFactory.setCCSID(UTF_8_WITH_PUA);
		connectionFactory.setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQConstants.MQENC_NATIVE);
		connectionFactory.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA);
		UserCredentialsConnectionFactoryAdapter adapter = new UserCredentialsConnectionFactoryAdapter();
		adapter.setTargetConnectionFactory(connectionFactory);
		adapter.setUsername(srvAppserverProperties.getUsername());
		adapter.setPassword(srvAppserverProperties.getPassword());
		return adapter;
	}
}
