package Handler;

import Http.HttpResponse;
import Http.HttpRequest;

/**
 * CONCEITO: Interface funcional
 */

@FunctionalInterface
public interface Handler {
	HttpResponse handle(HttpRequest request);
}
