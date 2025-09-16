package no.nav.dokdistfordeling.consumer.dokdistkanal;

import lombok.Builder;

@Builder
public record BestemDistribusjonskanalRequest(
		String brukerId,
		String dokumenttypeId,
		boolean erArkivert,
		Integer forsendelseStoerrelse,
		String mottakerId,
		String tema,
		Integer antallDokumenter,
		String forsendelseMetadataType
) {

	public BestemDistribusjonskanalRequest withForsendelseMetadataType(String forsendelseMetadataType) {
		return new BestemDistribusjonskanalRequest(this.brukerId, this.dokumenttypeId, this.erArkivert, this.forsendelseStoerrelse,
				this.mottakerId, this.tema, this.antallDokumenter, forsendelseMetadataType);
	}
}