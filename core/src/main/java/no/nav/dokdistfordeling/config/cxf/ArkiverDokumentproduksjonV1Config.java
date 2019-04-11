package no.nav.dokdistfordeling.config.cxf;

import no.nav.dokdistfordeling.config.alias.ArkiverDokumentproduksjonV1Alias;
import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.tjeneste.domene.brevogarkiv.arkiverdokumentproduksjon.v1.ArkiverDokumentproduksjonV1;
import org.apache.cxf.Bus;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
public class ArkiverDokumentproduksjonV1Config extends AbstractCxfEndpointConfig {

	private static final String NAMESPACE = "http://nav.no/tjeneste/domene/brevogarkiv/arkiverdokumentproduksjon/v1/";

	private static final QName SERVICE_QNAME = new QName(NAMESPACE, "ArkiverDokumentproduksjonService_v1");
	private static final QName PORT_QNAME = new QName(NAMESPACE, "ArkiverDokumentproduksjonPort_v1");

	private static final String WSDL_URL = "wsdl/no/nav/tjeneste/domene/brevogarkiv/arkiverdokumentproduksjon/v1/arkiverdokumentproduksjon.wsdl";

	@Inject
	public ArkiverDokumentproduksjonV1Config(Bus bus) {
		super(bus);
	}

	@Bean
	public ArkiverDokumentproduksjonV1 arkiverDokumentproduksjonPort(ArkiverDokumentproduksjonV1Alias arkiverDokumentproduksjonV1Alias,
																	 ServiceuserAlias serviceuserAlias) {
		setWsdlUrl(WSDL_URL);
		setServiceName(SERVICE_QNAME);
		setEndpointName(PORT_QNAME);
		setAdress(arkiverDokumentproduksjonV1Alias.getEndpointurl());
		setReceiveTimeout(arkiverDokumentproduksjonV1Alias.getReadtimeoutms());
		setConnectTimeout(arkiverDokumentproduksjonV1Alias.getConnecttimeoutms());
		addFeature(new WSAddressingFeature());
		addOutInterceptor(wss4JOutInterceptor(serviceuserAlias));
		addHandler(new MDCUsernameTokenOutHandler());

		return createPort(ArkiverDokumentproduksjonV1.class);
	}

	private WSS4JOutInterceptor wss4JOutInterceptor(ServiceuserAlias serviceuserAlias) {
		Map<String, Object> properties = new HashMap<>();
		properties.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
		properties.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
		properties.put(WSHandlerConstants.USER, serviceuserAlias.getUsername());
		properties.put(WSHandlerConstants.PW_CALLBACK_REF, new SystemuserPasswordCallback(serviceuserAlias.getPassword()));
		return new WSS4JOutInterceptor(properties);
	}
}
