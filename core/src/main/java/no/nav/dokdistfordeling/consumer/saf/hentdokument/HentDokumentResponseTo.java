package no.nav.dokdistfordeling.consumer.saf.hentdokument;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HentDokumentResponseTo {

	byte[] dokument;
}
