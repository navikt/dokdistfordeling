package no.nav.dokdistfordeling.dokdistdb.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@Entity
@Table(name = "DISTRIBUER_JOURNALPOST_INFO")
@AllArgsConstructor
@NoArgsConstructor
public class DistribuerJournalpostInfo {

	@Id
	@Column(name = "arkivkode", nullable = false, length = 40)
	private Long arkivkode;

	@Column(name = "dokument_id", nullable = false, length = 40)
	private String dokumentId;

	@Column(name = "opprettet_av", nullable = false, length = 100)
	private String opprettetAv;

	@Column(name = "opprettet_dato", nullable = false)
	private LocalDateTime opprettetDato;
}
