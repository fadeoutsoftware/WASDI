package wasdi.shared.business.processors;

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
	public static String TRAEFIK_NOTEBOOK = "traefik-notebook";
	public static String CSHARP = "csharp";
	public static String EOEPCA = "eoepca";
	public static String PYTHON_PIP_2 = "python_pip_2";
	public static String PIP_ONESHOT = "pip_oneshot";
	
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
		else if (sProcessorType.equals(JUPYTER_NOTEBOOK)) return "jupyter_notebook";
		else if (sProcessorType.equals(TRAEFIK_NOTEBOOK)) return "traefik-notebook";
		else if (sProcessorType.equals(CSHARP)) return "csharp";
		else if (sProcessorType.equals(EOEPCA)) return "eoepca";
		else if (sProcessorType.equals(PYTHON_PIP_2)) return "python_pip_2";
		else if (sProcessorType.equals(PIP_ONESHOT)) return "pip_oneshot";
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
			aoFiles.add("packagesInfo.json");
			aoFiles.add("runwasdidocker.sh");
			aoFiles.add("deploywasdidocker.sh");
			aoFiles.add("cleanwasdidocker.sh");			
		}
		else if (sProcessorType.equals(OCTAVE)) {
			
		}
		else if (sProcessorType.equals(CONDA)) {
			aoFiles.add("packagesInfo.json");
			aoFiles.add("runwasdidocker.sh");
			aoFiles.add("deploywasdidocker.sh");
			aoFiles.add("cleanwasdidocker.sh");			
		}
		else if (sProcessorType.equals(JUPYTER_NOTEBOOK)) {
			
		}
		else if (sProcessorType.equals(TRAEFIK_NOTEBOOK)) {

		}
		else if (sProcessorType.equals(CSHARP)) {
			aoFiles.add("deploywasdidocker.sh");
			aoFiles.add("runwasdidocker.sh");
		}
		else if (sProcessorType.equals(PYTHON_PIP_2)) {
			aoFiles.add("packagesInfo.json");
			aoFiles.add("runwasdidocker.sh");
			aoFiles.add("deploywasdidocker.sh");
			aoFiles.add("cleanwasdidocker.sh");			
		}
		else if (sProcessorType.equals(PIP_ONESHOT)) {
			aoFiles.add("installUserPackage.sh");
		}
		else if (sProcessorType.equals(EOEPCA)) {
			aoFiles.add("installUserPackage.sh");
		}		

		return aoFiles;
	}
}