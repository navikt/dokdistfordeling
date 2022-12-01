package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DokDistKanalRequest {
	private String mottakerId;
	private String dokumentTypeId;
	private String mottakerType;
	private String brukerId;
	private boolean erArkivert;
	private String tema;
}
