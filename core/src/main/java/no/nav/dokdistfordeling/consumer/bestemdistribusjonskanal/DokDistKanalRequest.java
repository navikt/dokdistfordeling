package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DokDistKanalRequest {
	private String brukerId;
	private String dokumentTypeId;
	private boolean erArkivert;
	private Integer forsendelseStoerrelse;
	private String mottakerId;
	private String tema;
}
