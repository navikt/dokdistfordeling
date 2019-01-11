package no.nav.naismal.prometheus;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
public class PrometheusLabels {
	
	public static final String TYPE_TECHNICAL_EXCEPTION = "technical";
	public static final String TYPE_FUNCTIONAL_EXCEPTION = "functional";
	
	public static final String LABEL_PROCESS = "process";
	public static final String LABEL_EVENT = "event";
	public static final String LABEL_PROCESS_CALLED = "process_called";
	public static final String LABEL_ERROR_TYPE = "error_type";
	public static final String LABEL_EXCEPTION_NAME = "exception_name";
	public static final String LABEL_DOKUMENTTYPE_ID = "dokumenttypeId";
	public static final String LABEL_TILKNYTNING = "tilknytning";
	public static final String LABEL_NAME = "name";
	
	public static final String EVENT_RECEIVED = "Kall mottatt";
	public static final String EVENT_PROCESSED = "Kall ok behandlet";
	public static final String EVENT_SENT_TO_QMOT100 = "Sendt til QMOT100";
	
	public static final String TILKNYTNING_HOVEDDOKUMENT = "hoveddokument";
	public static final String TILKNYTNING_VEDLEGG = "vedlegg";
	
	public static final String PROCESS_NAME_UNKNOWN = "ukjent prosessNavn";
	public static final String DOKUMENTTYPEID_NULL = "dokumenttypeId er null";
}
