package no.nav.dokdistfordeling.consumer.regoppslag;

import no.nav.dokdistfordeling.consumer.regoppslag.to.PostadresseRequest;
import no.nav.dokdistfordeling.consumer.regoppslag.to.PostadresseResponse;
import org.springframework.stereotype.Component;

@Component
public class PostadresseService {

    private final RegoppslagPostadresseConsumer regoppslagPostadresseConsumer;

    PostadresseService(RegoppslagPostadresseConsumer regoppslagPostadresseConsumer) {
        this.regoppslagPostadresseConsumer = regoppslagPostadresseConsumer;
    }

    public PostadresseResponse.Adresse hentAdresse(String ident) {
        PostadresseResponse postadresseResponse = regoppslagPostadresseConsumer.hentPostadresse(PostadresseRequest.builder()
                .ident(ident)
                .build());
        return postadresseResponse == null ? null : postadresseResponse.adresse();
    }
}
