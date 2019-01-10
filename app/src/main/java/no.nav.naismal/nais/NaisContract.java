package no.nav.naismal.nais;


import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NaisContract {

    public static final String APPLICATION_ALIVE = "Application is alive!";
    public static final String APPLICATION_READY = "Application is ready for traffic!";

    @GetMapping("/isAlive")
    public String isAlive() {
        return APPLICATION_ALIVE;
    }

    @RequestMapping(value = "/isReady", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity isReady() throws Exception {
        return new ResponseEntity<>(APPLICATION_READY, HttpStatus.OK);
    }
}
