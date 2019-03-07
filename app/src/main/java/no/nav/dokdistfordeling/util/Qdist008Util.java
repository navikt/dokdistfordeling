package no.nav.dokdistfordeling.util;

import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import no.nav.dokdistfordeling.qdist008.DistribuerForsendelseTo;

import java.util.stream.Collectors;

public class Qdist008Util {
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
				.map(DistribuerForsendelseTo.DokumentInformasjonTo::getDokumenttypeId)
				.collect(Collectors.toList())
				.get(0);
	}
}
