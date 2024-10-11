package wasdi.shared.business.processors;

/**
 * Processor Type static definition
 * @author p.campanella
 *
 */
public class ProcessorTypes {
	
	private ProcessorTypes() {
		// private constructor to hide the public implicit one 
	}
	
	public static String UBUNTU_PYTHON37_SNAP = "ubuntu_python37_snap";
	public static String IDL = "ubuntu_idl372";
	public static String OCTAVE = "octave";
	public static String CONDA = "conda";
	public static String JUPYTER_NOTEBOOK = "jupyter-notebook";
	public static String CSHARP = "csharp";
	public static String EOEPCA = "eoepca";
	public static String PYTHON_PIP_2 = "python_pip_2";
	public static String PIP_ONESHOT = "pip_oneshot";
	public static String PYTHON_PIP_2_UBUNTU_20 = "python_pip_2_ubuntu_20";
	public static String JAVA_17_UBUNTU_22 = "java_17_Ubuntu_22";
	
	/**
	 * Obtains the name of the subfolder where the docker template is stored.
	 * @param sProcessorType Processor Type
	 * @return Folder Name (that has to be concatenated to the base processors path)
	 */
	public static String getTemplateFolder(String sProcessorType) {
		if (sProcessorType.equals(IDL)) return "idl";
		else if (sProcessorType.equals(UBUNTU_PYTHON37_SNAP)) return "python37";
		else if (sProcessorType.equals(OCTAVE)) return "octave";
		else if (sProcessorType.equals(CONDA)) return "conda";
		else if (sProcessorType.equals(JUPYTER_NOTEBOOK)) return "jupyter-notebook";
		else if (sProcessorType.equals(CSHARP)) return "csharp";
		else if (sProcessorType.equals(EOEPCA)) return "eoepca";
		else if (sProcessorType.equals(PYTHON_PIP_2)) return "python_pip_2";
		else if (sProcessorType.equals(PIP_ONESHOT)) return "pip_oneshot";
		else if (sProcessorType.equals(PYTHON_PIP_2_UBUNTU_20)) return "wasdiUbuntuFocalPython";
		else if (sProcessorType.equals(JAVA_17_UBUNTU_22)) return "wasdiJava17Docker";
		return "";
	}
	
}
