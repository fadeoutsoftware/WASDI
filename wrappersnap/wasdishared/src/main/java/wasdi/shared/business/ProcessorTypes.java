package wasdi.shared.business;

/**
 * Processor Type static definition
 * @author p.campanella
 *
 */
public class ProcessorTypes {
	
	private ProcessorTypes() {
		// / private constructor to hide the public implicit one 
	}
	public static String UBUNTU_PYTHON27_SNAP = "ubuntu_python_snap";
	public static String UBUNTU_PYTHON37_SNAP = "ubuntu_python37_snap";
	public static String IDL = "ubuntu_idl372";
	public static String OCTAVE = "octave";
	
	public static String getTemplateFolder(String sProcessorType) {
		if (sProcessorType.equals(IDL)) return "idl";
		else if (sProcessorType.equals(UBUNTU_PYTHON27_SNAP)) return "python27";
		else if (sProcessorType.equals(UBUNTU_PYTHON37_SNAP)) return "python37";
		else if (sProcessorType.equals(OCTAVE)) return "octave";
		return "";
	}
}
