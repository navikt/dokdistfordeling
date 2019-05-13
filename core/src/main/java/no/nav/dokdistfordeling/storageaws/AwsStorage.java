package no.nav.dokdistfordeling.storageaws;

import java.util.Optional;

public interface AwsStorage {

	void put(String key, String value);

	Optional<String> get(String key);

	void delete(String key);
}
