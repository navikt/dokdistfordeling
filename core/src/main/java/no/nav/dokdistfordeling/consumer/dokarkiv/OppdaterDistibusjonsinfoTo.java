package no.nav.dokdistfordeling.consumer.dokarkiv;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OppdaterDistibusjonsinfoTo {
	boolean settStatusEkspedert;
	String utsendingsKanal;
}
