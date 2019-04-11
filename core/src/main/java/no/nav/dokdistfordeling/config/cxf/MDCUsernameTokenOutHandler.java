package no.nav.dokdistfordeling.config.cxf;

import static no.nav.dokdistfordeling.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistfordeling.constants.MdcConstants.USER_ID;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.HashSet;
import java.util.Set;

/**
 * Soap-handler that appends CallId and UserId to SOAP Header for outgoing requests
 *
 * @author Sigurd Midttun, Visma Consulting.
 */
@Slf4j
public class MDCUsernameTokenOutHandler implements SOAPHandler<SOAPMessageContext> {

	private static final String DOKDISTFORDELING_USER_ID = "srvdokdistfordeling";
	private static final String URI_NO_NAV_APPLIKASJONSRAMMEVERK = "uri:no.nav.applikasjonsrammeverk";

	private static final QName CALL_ID_QNAME = new QName(URI_NO_NAV_APPLIKASJONSRAMMEVERK, CALL_ID);
	private static final QName USER_ID_QNAME = new QName(URI_NO_NAV_APPLIKASJONSRAMMEVERK, USER_ID);

	@Override
	public boolean handleMessage(SOAPMessageContext context) {
		Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		if (outbound) {
			appendToSoapHeader(context, CALL_ID_QNAME, MDC.get(CALL_ID));
			appendToSoapHeader(context, USER_ID_QNAME, DOKDISTFORDELING_USER_ID);
		}
		return true;
	}

	private void appendToSoapHeader(SOAPMessageContext context, QName qName, String value) {
		try {
			SOAPHeader header = context.getMessage().getSOAPPart().getEnvelope().getHeader();
			SOAPElement element = header.addChildElement(qName);
			element.setValue(value == null ? "" : value);
		} catch (SOAPException e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		return true;
	}

	@Override
	public void close(MessageContext context) {
		//Nothing to close
	}

	@Override
	public Set<QName> getHeaders() {
		HashSet<QName> headers = new HashSet<>();
		headers.add(CALL_ID_QNAME);
		headers.add(USER_ID_QNAME);
		return headers;
	}

}
