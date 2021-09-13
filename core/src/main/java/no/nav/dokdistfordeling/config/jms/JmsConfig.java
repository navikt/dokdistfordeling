
package no.nav.dokdistfordeling.config.jms;

import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import com.ibm.msg.client.jms.JmsConstants;
import com.ibm.msg.client.wmq.WMQConstants;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.config.props.SrvAppserverProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.apache.activemq.jms.pool.PooledConnectionFactory;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;

/**
 * @author Sigurd Midttun, Visma Consulting AS
 */
@Profile({"nais", "local"})
@Configuration
public class JmsConfig {

    private static final int UTF_8_WITH_PUA = 1208;

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
    public Queue qdist012Backout(@Value("${dokdistfordeling_qdist012_backout_queuename}") String qdist012BackoutQueueName) throws JMSException {
        return new MQQueue(qdist012BackoutQueueName);
    }

    @Bean
    public Queue qdist013(@Value("${dokdisteformidling_qdist013_dist_trygderetten.queuename}") String qdist013QueueName) throws JMSException {
        return new MQQueue(qdist013QueueName);
    }

    @Bean
    public ConnectionFactory wmqConnectionFactory(final MqGatewayAlias mqGatewayAlias,
                                                  final @Value("${dokdistfordeling_channel.name}") String channelName,
                                                  final SrvAppserverProperties srvAppserverProperties,
                                                  final ServiceuserAlias serviceuserAlias) throws JMSException {
        return createConnectionFactory(mqGatewayAlias, channelName, srvAppserverProperties, serviceuserAlias);
    }

    private PooledConnectionFactory createConnectionFactory(final MqGatewayAlias mqGatewayAlias,
                                                                            final String channelName,
                                                                            final SrvAppserverProperties srvAppserverProperties,
                                                                            final ServiceuserAlias serviceuserAlias) throws JMSException {
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

        PooledConnectionFactory pooledFactory = new PooledConnectionFactory();
        pooledFactory.setConnectionFactory(adapter);
        pooledFactory.setMaxConnections(10);
        pooledFactory.setMaximumActiveSessionPerConnection(10);

        if (mqGatewayAlias.isTlsbroker()) {
            // Konfigurasjon for IBM MQ broker med TLS og autorisasjon med serviceuser mot onpremise Active Directory.
            adapter.setUsername(serviceuserAlias.getUsername());
            adapter.setPassword(serviceuserAlias.getPassword());
        } else {
            // Legacy IBM MQ broker
            connectionFactory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, false);
            adapter.setUsername(srvAppserverProperties.getUsername());
            adapter.setPassword(srvAppserverProperties.getPassword());
        }
        return pooledFactory;
    }
}
