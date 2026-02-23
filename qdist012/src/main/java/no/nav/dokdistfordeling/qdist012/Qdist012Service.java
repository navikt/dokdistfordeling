package no.nav.dokdistfordeling.qdist012;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.saf.hentdokument.SafHentDokumentConsumer;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJournalpostQueryService;
import no.nav.dokdistfordeling.dokdistdb.DistribuerJournalpostIdempotencyHandler;
import no.nav.dokdistfordeling.dokdistdb.DistribuerJournalpostInfoResponse;
import no.nav.dokdistfordeling.exception.functional.JournalpostErAlleredeDistribuertException;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.DistribusjonbestillingTo;
import no.nav.dokdistfordeling.storage.BucketStorage;
import no.nav.dokdistfordeling.storage.DokdistDokument;
import no.nav.dokdistfordeling.storage.JsonSerializer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.dokdistfordeling.constants.Constants.DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID;
import static no.nav.dokdistfordeling.constants.ValidationConstants.FERDIGSTILT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.ARKIV;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.SLADDET;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
public class Qdist012Service {

	private final SafHentDokumentConsumer safHentDokumentConsumer;
	private final BucketStorage bucketStorage;
	private final Qdist008DistribuerForsendelseMapper qdist008DistribuerForsendelseMapper;
	private final SafJournalpostQueryService safJournalpostQueryService;
	private final DistribuerJournalpostIdempotencyHandler distribuerJournalpostIdempotencyHandler;

	public Qdist012Service(SafHentDokumentConsumer safHentDokumentConsumer,
						   BucketStorage bucketStorage,
						   Qdist008DistribuerForsendelseMapper qdist008DistribuerForsendelseMapper,
						   SafJournalpostQueryService safJournalpostQueryService,
						   DistribuerJournalpostIdempotencyHandler distribuerJournalpostIdempotencyHandler) {
		this.safHentDokumentConsumer = safHentDokumentConsumer;
		this.bucketStorage = bucketStorage;
		this.qdist008DistribuerForsendelseMapper = qdist008DistribuerForsendelseMapper;
		this.safJournalpostQueryService = safJournalpostQueryService;
		this.distribuerJournalpostIdempotencyHandler = distribuerJournalpostIdempotencyHandler;
	}

	@SuppressWarnings("unused")
	@Handler
	public DistribuerForsendelse copyDocumentsFromJoarkToDokdistmellomlagerBucketStorage(HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo) {
		final DistribusjonbestillingTo distribusjonbestilling = hentDokumenterFraJoarkTo.getDistribusjonbestilling();
		final String arkivId = distribusjonbestilling.getArkivInformasjon().getArkivId();

		avvisAlleredeDistribuertForsendelse(distribusjonbestilling.getBestillingsId(), arkivId);

		// tilknyttVedlegg legger til vedlegg etter at dokprod forsendelse er opprettet, så legg på evt. manglende vedlegg her
		addMissingVedlegg(arkivId, hentDokumenterFraJoarkTo);
		distribusjonbestilling.getDokumenter()
				.forEach(dokumentInformasjonTo -> {
					byte[] dokument = safHentDokumentConsumer.hentDokument(arkivId, dokumentInformasjonTo.getArkivDokumentInfoId(), dokumentInformasjonTo.getVariantFormat());
					final String dokumentObjektReferanse = UUID.randomUUID().toString();
					dokumentInformasjonTo.setDokumentObjektReferanse(dokumentObjektReferanse);
					bucketStorage.upload(dokumentObjektReferanse, buildAndSerializeDokdistDokument(dokument), distribusjonbestilling.getBestillingsId());
				});

		return qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);
	}

	private void avvisAlleredeDistribuertForsendelse(String bestillingsId, String journalpostId) {
		DistribuerJournalpostInfoResponse eksisterende = distribuerJournalpostIdempotencyHandler.hentDistribuerJournalpostInfo(Long.parseLong(journalpostId));

		if (eksisterende != null && !eksisterende.bestillingsId().equals(bestillingsId)) {
			throw new JournalpostErAlleredeDistribuertException(
					"Journalpost med journalpostId=%s er allerede distribuert i distribusjon med bestillingsId=%s, avbryter behandling."
							.formatted(journalpostId, eksisterende.bestillingsId()));
		}
	}

	private void addMissingVedlegg(String journalpostId, HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo) {
		if (isBlank(journalpostId)) {
			return;
		}
		List<HentDokumenterFraJoarkTo.DokumentInformasjonTo> prosesserteDokumenter =
				hentDokumenterFraJoarkTo.getDistribusjonbestilling().getDokumenter();
		Set<String> prosesserteDokumentIder = prosesserteDokumenter.stream()
				.map(HentDokumenterFraJoarkTo.DokumentInformasjonTo::getArkivDokumentInfoId).collect(Collectors.toSet());

		Journalpost journalpost = safJournalpostQueryService.hentJournalpost(journalpostId);
		List<Journalpost.DokumentInfo> dokumenter = journalpost.getDokumenter();
		dokumenter.stream()
				.filter(dokument -> !prosesserteDokumentIder.contains(dokument.getDokumentInfoId()))
				.filter(dokument -> isDokumentFerdigstilt(dokument.getDokumentstatus()))
				.forEach(dokument -> prosesserteDokumenter.add(mapDokumentInformasjonTo(dokument, prosesserteDokumenter.size() + 1)));
	}

	private boolean isDokumentFerdigstilt(String dokumentStatus) {
		return isBlank(dokumentStatus) || FERDIGSTILT.equals(dokumentStatus);
	}

	private HentDokumenterFraJoarkTo.DokumentInformasjonTo mapDokumentInformasjonTo(Journalpost.DokumentInfo dokumentInfo, int rekkefolge) {
		return HentDokumenterFraJoarkTo.DokumentInformasjonTo.builder()
				.arkivDokumentInfoId(dokumentInfo.getDokumentInfoId())
				.dokumenttypeId(DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID)
				.rekkefolge(rekkefolge)
				.tilknyttetSom(VEDLEGG.name())
				.variantFormat(getVariantFormat(dokumentInfo.getDokumentvarianter()))
				.build();
	}

	private String getVariantFormat(List<Journalpost.Dokumentvariant> dokumentvarianter) {
		return dokumentvarianter.stream()
				.anyMatch(dokumentvariant -> SLADDET.equals(dokumentvariant.getVariantformat())) ?
				SLADDET.name() : ARKIV.name();
	}

	private String buildAndSerializeDokdistDokument(byte[] document) {
		return JsonSerializer.serialize(DokdistDokument.builder().pdf(document).build());
	}
}
