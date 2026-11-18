package wasdi.shared.utils.docker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class DockerPushResponseTest {

	@Test
	public void acceptsConfirmedTag() throws IOException {
		assertTrue(DockerUtils.isSuccessfulPushResponse("{\"status\":\"Pushing\"}\n"
				+ "{\"status\":\"1: digest: sha256:abc size: 10\"}\n"));
		assertTrue(DockerUtils.isSuccessfulPushResponse("{\"aux\":{\"Tag\":\"1\",\"Digest\":\"sha256:abc\"}}"));
	}

	@Test
	public void rejectsErrorsAndUnconfirmedPushes() throws IOException {
		assertFalse(DockerUtils.isSuccessfulPushResponse("{\"status\":\"Pushing\"}\n"
				+ "{\"errorDetail\":{\"message\":\"denied\"},\"error\":\"denied\"}\n"));
		assertFalse(DockerUtils.isSuccessfulPushResponse("{\"status\":\"Pushed\"}\n"));
		assertFalse(DockerUtils.isSuccessfulPushResponse(""));
	}
}