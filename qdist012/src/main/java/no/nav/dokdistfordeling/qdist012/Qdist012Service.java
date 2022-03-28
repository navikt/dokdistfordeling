package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistfordeling.consumer.saf.hentdokument.HentDokument;
import no.nav.dokdistfordeling.consumer.saf.hentdokument.HentDokumentResponseTo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.sts.StsRestConsumer;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.dokdistfordeling.storage.DokdistDokument;
import no.nav.dokdistfordeling.storage.JsonSerializer;
import no.nav.dokdistfordeling.storage.Storage;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.dokdistfordeling.constants.Constants.BEARER_PREFIX;
import static no.nav.dokdistfordeling.constants.Constants.DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID;
import static no.nav.dokdistfordeling.constants.ValidationConstants.FERDIGSTILT;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist012Service {

	private final HentDokument hentDokument;
	private final Storage storage;
	private final Qdist008DistribuerForsendelseMapper qdist008DistribuerForsendelseMapper;
	private SafJournalpostQueryService safJournalpostQueryService;
	private final StsRestConsumer stsRestConsumer;


	@Autowired
	public Qdist012Service(HentDokument hentDokument,
						   Storage storage,
						   Qdist008DistribuerForsendelseMapper qdist008DistribuerForsendelseMapper,
						   SafJournalpostQueryService safJournalpostQueryService,
						   StsRestConsumer stsRestConsumer) {
		this.hentDokument = hentDokument;
		this.storage = storage;
		this.qdist008DistribuerForsendelseMapper = qdist008DistribuerForsendelseMapper;
		this.safJournalpostQueryService = safJournalpostQueryService;
		this.stsRestConsumer = stsRestConsumer;
	}

	@Handler
	public DistribuerForsendelse copyDocumentsFromJoarkToDokdistmellomlagerS3Storage(HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo) {
		HentDokumenterFraJoarkTo.DistribusjonbestillingTo distribusjonbestilling = hentDokumenterFraJoarkTo.getDistribusjonbestilling();
		final String arkivId = distribusjonbestilling.getArkivInformasjon().getArkivId();

		// tilknyttVedlegg legger til vedlegg etter at dokprod forsendelse er opprettet, så legg på evt. manglende vedlegg her
		addMissingVedlegg(arkivId, hentDokumenterFraJoarkTo);

		distribusjonbestilling.getDokumenter()
				.forEach(dokumentInformasjonTo -> {
					HentDokumentResponseTo hentDokumentResponseTo = hentDokument.hentDokument(arkivId, dokumentInformasjonTo.getArkivDokumentInfoId(),
							dokumentInformasjonTo.getVariantFormat());
					final String dokumentObjektReferanse = UUID.randomUUID().toString();
					dokumentInformasjonTo.setDokumentObjektReferanse(dokumentObjektReferanse);
					storage.put(dokumentObjektReferanse, buildAndSerializeDokdistDokument(hentDokumentResponseTo.getDokument()));
				});

		return qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);
	}

	private void addMissingVedlegg(String journalpostId, HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo) {
		if (isBlank(journalpostId)) {
			return;
		}
		List<HentDokumenterFraJoarkTo.DokumentInformasjonTo> prosesserteDokumenter =
				hentDokumenterFraJoarkTo.getDistribusjonbestilling().getDokumenter();
		Set<String> prosesserteDokumentIder = prosesserteDokumenter.stream()
				.map(HentDokumenterFraJoarkTo.DokumentInformasjonTo::getArkivDokumentInfoId).collect(Collectors.toSet());

		Journalpost journalpost = safJournalpostQueryService.hentJournalpost(journalpostId, getAuthorizationHeader());
		List<Journalpost.DokumentInfo> dokumenter = journalpost.getDokumenter();
		dokumenter.stream()
				.filter(dokument -> !prosesserteDokumentIder.contains(dokument.getDokumentInfoId()))
				.filter(dokument -> isDokumentFerdigstilt(dokument.getDokumentstatus()))
				.forEach(dokument -> prosesserteDokumenter.add(mapDokumentInformasjonTo(dokument, prosesserteDokumenter.size() + 1)));
	}

	private String getAuthorizationHeader() {
		return BEARER_PREFIX + stsRestConsumer.getOidcToken();
	}

	private boolean isDokumentFerdigstilt(String dokumentStatus) {
		return isBlank(dokumentStatus) || FERDIGSTILT.equals(dokumentStatus);
	}

	private HentDokumenterFraJoarkTo.DokumentInformasjonTo mapDokumentInformasjonTo(Journalpost.DokumentInfo dokumentInfo, int rekkefolge) {
		return HentDokumenterFraJoarkTo.DokumentInformasjonTo.builder()
				.arkivDokumentInfoId(dokumentInfo.getDokumentInfoId())
				.dokumenttypeId(DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID)
				.rekkefolge(rekkefolge)
				.tilknyttetSom(TilknyttetSomCode.VEDLEGG.name())
				.variantFormat(getVariantFormat(dokumentInfo.getDokumentvarianter()))
				.build();
	}

	private String getVariantFormat(List<Journalpost.Dokumentvariant> dokumentvarianter) {
		return dokumentvarianter.stream()
				.anyMatch(dokumentvariant -> Variantformat.SLADDET.equals(dokumentvariant.getVariantformat())) ?
				Variantformat.SLADDET.name() : Variantformat.ARKIV.name();
	}

	private String buildAndSerializeDokdistDokument(byte[] document) {
		return JsonSerializer.serialize(DokdistDokument.builder().pdf(document).build());
	}
}
