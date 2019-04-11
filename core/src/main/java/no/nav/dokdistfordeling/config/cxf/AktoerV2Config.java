package no.nav.dokdistfordeling.config.cxf;


import no.nav.dokdistfordeling.config.sts.STSConfig;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
public class AktoerV2Config {

	@Bean
	public AktoerV2 aktoerV2(STSConfig stsConfig,
							 @Value("${aktoer.v2.endpointurl}") String aktoerV2Url) {
		JaxWsProxyFactoryBean clientFactory = new JaxWsProxyFactoryBean();
		clientFactory.setServiceClass(AktoerV2.class);
		clientFactory.setAddress(aktoerV2Url);
		clientFactory.setFeatures(Collections.singletonList(new WSAddressingFeature()));

		AktoerV2 aktoerV2 = (AktoerV2) clientFactory.create();
		stsConfig.configureSTS(aktoerV2);
		Client client = ClientProxy.getClient(aktoerV2);
		setClientTimeout(client);
		return aktoerV2;
	}

	private void setClientTimeout(Client client) {
		HTTPConduit conduit = (HTTPConduit) client.getConduit();
		HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
		httpClientPolicy.setConnectionTimeout(3000L);
		httpClientPolicy.setReceiveTimeout(3000L);
		conduit.setClient(httpClientPolicy);
	}

}