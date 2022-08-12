package wasdi.shared.business;

import java.util.ArrayList;

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
	public static String JUPYTER_NOTEBOOK = "jupyter_notebook";
	public static String CSHARP = "csharp";
	
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
		else if (sProcessorType.equals(JUPYTER_NOTEBOOK)) return "python37-jupyter";
		else if (sProcessorType.equals(CSHARP)) return "csharp";
		return "";
	}
	
	public static ArrayList<String> getAdditionalTemplateGeneratedFiles(String sProcessorType) {
		ArrayList<String> aoFiles = new ArrayList<>();

		if (sProcessorType.equals(IDL)) {
			aoFiles.add("wasdi_wrapper.pro");
			aoFiles.add("runwasdidocker.sh");
			aoFiles.add("deploywasdidocker.sh");
			aoFiles.add("cleanwasdidocker.sh");
			aoFiles.add("call_idl.pro");
		}
		else if (sProcessorType.equals(UBUNTU_PYTHON37_SNAP)) {
			
		}
		else if (sProcessorType.equals(OCTAVE)) {
			
		}
		else if (sProcessorType.equals(CONDA)) {
			
		}
		else if (sProcessorType.equals(JUPYTER_NOTEBOOK)) {

		}
		else if (sProcessorType.equals(CSHARP)) {
			aoFiles.add("deploywasdidocker.sh");
			aoFiles.add("runwasdidocker.sh");
		}

		return aoFiles;
	}
}
