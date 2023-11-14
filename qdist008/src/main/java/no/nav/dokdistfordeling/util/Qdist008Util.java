package no.nav.dokdistfordeling.util;


import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo;

import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;

public class Qdist008Util {

	private Qdist008Util() {
	}

	public static int countHoveddokument(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		return (int) distribusjonbestilling.getDokumenter().stream()
				.filter(dokumentInformasjonTo -> dokumentInformasjonTo.getTilknyttetSom().equals(HOVEDDOKUMENT))
				.count();
	}

	public static String getDokumenttypeIdHoveddokument(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		return distribusjonbestilling.getDokumenter().stream()
				.filter(dokumentInformasjonTo -> dokumentInformasjonTo.getTilknyttetSom().equals(HOVEDDOKUMENT))
				.map(DistribuerForsendelseTo.DokumentInformasjonTo::getDokumenttypeId).toList()
				.get(0);
	}
}
