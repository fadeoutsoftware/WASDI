package wasdi.shared.business.processors;

/**
 * Source types supported by WASDI processors.
 */
public class ProcessorSourceTypes {

	private ProcessorSourceTypes() {
		// Private constructor to hide the public implicit one
	}

	public static final String UPLOAD = "UPLOAD";
	public static final String GIT = "GIT";
}