package no.nav.naismal.prometheus;


import static no.nav.naismal.prometheus.PrometheusLabels.LABEL_DOKUMENTTYPE_ID;
import static no.nav.naismal.prometheus.PrometheusLabels.LABEL_ERROR_TYPE;
import static no.nav.naismal.prometheus.PrometheusLabels.LABEL_EVENT;
import static no.nav.naismal.prometheus.PrometheusLabels.LABEL_EXCEPTION_NAME;
import static no.nav.naismal.prometheus.PrometheusLabels.LABEL_NAME;
import static no.nav.naismal.prometheus.PrometheusLabels.LABEL_PROCESS;
import static no.nav.naismal.prometheus.PrometheusLabels.LABEL_PROCESS_CALLED;
import static no.nav.naismal.prometheus.PrometheusLabels.LABEL_TILKNYTNING;
import static no.nav.naismal.prometheus.PrometheusLabels.PROCESS_NAME_UNKNOWN;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
public class PrometheusMetrics {
	
	private static final String DOK_NAMESPACE = "dok";
	
	// Health checks
	public static final Gauge isReady = Gauge.build()
			.namespace(DOK_NAMESPACE)
			.name("app_is_ready")
			.help("App is ready to receive traffic")
			.register();

	public static final Gauge dependencyPingable = Gauge.build()
			.namespace(DOK_NAMESPACE)
			.name("dependency_ping")
			.help("Dependency is pingable")
			.labelNames(LABEL_NAME)
			.register();
	
	// Requests
	public static final Histogram requestLatency = Histogram.build()
			.namespace(DOK_NAMESPACE)
			.name("request_latency_seconds_histogram")
			.help("Timing of external and internal calls")
			.labelNames(LABEL_PROCESS, LABEL_PROCESS_CALLED)
			.register();
	
	public static final Histogram requestSize = Histogram.build()
			.namespace(DOK_NAMESPACE)
			.name("internal_request_size_bytes_histogram")
			.help("Stores request sizes")
			.labelNames(LABEL_PROCESS, LABEL_PROCESS_CALLED)
			.exponentialBuckets(2000, 2, 13)
			.register();
	
	public static final Counter requestCounter = Counter.build()
			.namespace(DOK_NAMESPACE)
			.name("request_total_counter")
			.help("Counts total number of requests received per event.")
			.labelNames(LABEL_PROCESS, LABEL_EVENT)
			.register();
	
	public static final Counter dokumenttypeIdCounter = Counter.build()
			.namespace(DOK_NAMESPACE)
			.name("dokumenttype_id_counter")
			.help("Counts number of distinct dokumenttypeId's.")
			.labelNames(LABEL_PROCESS, LABEL_DOKUMENTTYPE_ID, LABEL_TILKNYTNING)
			.register();
	
	public static final Counter requestExceptionCounter = Counter.build()
			.namespace(DOK_NAMESPACE)
			.name("request_exception_total_counter")
			.help("Exception counter.")
			.labelNames(LABEL_PROCESS, LABEL_ERROR_TYPE, LABEL_EXCEPTION_NAME)
			.register();
	
	public static String getProcessName(String mdcProcessName){
		if (mdcProcessName == null || mdcProcessName.isEmpty()){
			return PROCESS_NAME_UNKNOWN;
		} else{
			return mdcProcessName;
		}
	}
}
