package no.nav.dokdistfordeling.util;


import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;

import java.util.stream.Collectors;

public class Qdist008Util {

	private Qdist008Util() {
	}

	public static int countHoveddokument(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		return (int) distribusjonbestilling.getDokumenter().stream()
				.filter(dokumentInformasjonTo -> dokumentInformasjonTo.getTilknyttetSom()
						.equals(TilknyttetSomCode.HOVEDDOKUMENT))
				.count();
	}

	public static int countVedlegg(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		return (int) distribusjonbestilling.getDokumenter().stream()
				.filter(dokumentInformasjonTo -> dokumentInformasjonTo.getTilknyttetSom()
						.equals(TilknyttetSomCode.VEDLEGG))
				.count();
	}

	public static String getDokumenttypeIdHoveddokument(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		return distribusjonbestilling.getDokumenter().stream()
				.filter(dokumentInformasjonTo -> dokumentInformasjonTo.getTilknyttetSom()
						.equals(TilknyttetSomCode.HOVEDDOKUMENT))
				.map(DistribuerForsendelseTo.DokumentInformasjonTo::getDokumenttypeId).toList()
				.get(0);
	}
}
