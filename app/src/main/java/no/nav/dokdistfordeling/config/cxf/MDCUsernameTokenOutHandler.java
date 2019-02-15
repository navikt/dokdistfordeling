package no.nav.dokdistfordeling.config.cxf;

import lombok.extern.slf4j.Slf4j;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.HashSet;
import java.util.Set;

/**
 * Soap-handler that appends CallId and AppId to SOAP Header for outgoing requests
 *
 * @author Roar Bjurstrom, Visma Consulting.
 */
@Slf4j
//TODO FixMe
public class MDCUsernameTokenOutHandler implements SOAPHandler<SOAPMessageContext> {

	public static final String MDC_CALL_ID = "callId";
	public static final String MDC_USER_ID = "userId";
	public static final String MDC_CONSUMER_ID = "consumerId";

	public static final String URI_NO_NAV_APPLIKASJONSRAMMEVERK = "uri:no.nav.applikasjonsrammeverk";
	//	private static final QName APP_ID_QNAME = new QName(URI_NO_NAV_APPLIKASJONSRAMMEVERK, ApplicationConstants.APP_ID);
	private static final QName CALLID_QNAME = new QName(URI_NO_NAV_APPLIKASJONSRAMMEVERK, MDC_CALL_ID);
	private static final QName CONSUMER_ID_QNAME = new QName(URI_NO_NAV_APPLIKASJONSRAMMEVERK, MDC_USER_ID);
	private static final QName USER_ID_QNAME = new QName(URI_NO_NAV_APPLIKASJONSRAMMEVERK, MDC_CONSUMER_ID);

	@Override
	public boolean handleMessage(SOAPMessageContext context) {
		Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
//
//		if (outbound) {
//			String callId = MDC.get(DCOperations.MDC_CALL_ID);
//			if (isEmpty(callId)) {
//				log.debug("Callid is null/empty, generating");
//				callId = MDCOperations.generateCallId();
//			}
//			appendToSoapHeader(context, CALLID_QNAME, callId);
//
//			String appId = MDCOperations.getFromMDC(ApplicationConstants.APP_ID);
//			if (isEmpty(appId)) {
//				log.debug("appId is null, using default");
//				appId = DEFAULT_APP_ID;
//			}
//			appendToSoapHeader(context, APP_ID_QNAME, appId);
//
//			String consumerId = MDCOperations.getFromMDC(MDCOperations.MDC_CONSUMER_ID);
//			appendToSoapHeader(context, CONSUMER_ID_QNAME, consumerId);
//
//			String userId = MDCOperations.getFromMDC(MDCOperations.MDC_USER_ID);
//			appendToSoapHeader(context, USER_ID_QNAME, userId);
//		}
		return true;
	}

	private void appendToSoapHeader(SOAPMessageContext context, QName qName, String value) {
		try {
			SOAPHeader header = context.getMessage().getSOAPPart().getEnvelope().getHeader();
			SOAPElement element = header.addChildElement(qName);
			element.setValue(value == null ? "" : value);
		} catch (SOAPException e) {
			log.error(e.getMessage());
			throw new ProtocolException(e);
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
		return new HashSet<QName>() {
			{
//				add(APP_ID_QNAME);
				add(CALLID_QNAME);
				add(CONSUMER_ID_QNAME);
				add(USER_ID_QNAME);
			}
		};
	}

}
