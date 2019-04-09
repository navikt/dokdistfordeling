package no.nav.dokdistfordeling.config.cxf;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;

import javax.xml.namespace.QName;
import java.net.URL;
import java.util.HashMap;

/**
 * Abstract helper class for Cxf Endpoints
 *
 * @author Andreas Skomedal, Visma Consulting.
 */
public abstract class AbstractCxfEndpointConfig {
	private static final int DEFAULT_TIMEOUT = 30_000;

	private int receiveTimeout = DEFAULT_TIMEOUT;
	private int connectTimeout = DEFAULT_TIMEOUT;
	private final JaxWsProxyFactoryBean factoryBean;

	AbstractCxfEndpointConfig(Bus bus) {
		factoryBean = new JaxWsProxyFactoryBean();
		factoryBean.setProperties(new HashMap<>());
		factoryBean.setBus(bus);
	}

	void setAdress(String aktoerUrl) {
		factoryBean.setAddress(aktoerUrl);
	}

	void setWsdlUrl(String classPathResourceWsdlUrl) {
		factoryBean.setWsdlURL(getUrlFromClasspathResource(classPathResourceWsdlUrl));
	}

	void setEndpointName(QName endpointName) {
		factoryBean.setEndpointName(endpointName);
	}

	void setServiceName(QName serviceName) {
		factoryBean.setServiceName(serviceName);
	}

	void addFeature(Feature feature) {
		factoryBean.getFeatures().add(feature);
	}

	protected void addOutInterceptor(Interceptor<? extends Message> interceptor) {
		factoryBean.getOutInterceptors().add(interceptor);
	}

	protected void addHandler(javax.xml.ws.handler.Handler handler) {
		factoryBean.getHandlers().add(handler);
	}

	<T> T createPort(Class<T> portType) {
		factoryBean.getFeatures().add(new TimeoutFeature(receiveTimeout, connectTimeout));
		return factoryBean.create(portType);
	}

	private static String getUrlFromClasspathResource(String classpathResource) {
		URL url = AbstractCxfEndpointConfig.class.getClassLoader().getResource(classpathResource);
		if (url != null) {
			return url.toString();
		}
		throw new IllegalStateException("Failed to find resource: " + classpathResource);
	}

	void setReceiveTimeout(int receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

}
