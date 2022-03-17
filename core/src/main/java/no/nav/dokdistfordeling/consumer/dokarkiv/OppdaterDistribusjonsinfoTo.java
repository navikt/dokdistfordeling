package no.nav.dokdistfordeling.consumer.dokarkiv;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OppdaterDistribusjonsinfoTo {
	boolean settStatusEkspedert;
	String utsendingsKanal;
}
