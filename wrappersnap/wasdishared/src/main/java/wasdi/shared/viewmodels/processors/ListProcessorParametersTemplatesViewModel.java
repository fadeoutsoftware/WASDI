package wasdi.shared.viewmodels.processors;

import java.util.ArrayList;
import java.util.List;

/**
 * List ProcessorParametersTemplate View Model
 * 
 * Wraps a list of comments view models
 * 
 * @author PetruPetrescu
 *
 */
public class ListProcessorParametersTemplatesViewModel {

	private List<ProcessorParametersTemplateViewModel> processorParametersTemplates = new ArrayList<>();

	public List<ProcessorParametersTemplateViewModel> getProcessorParametersTemplates() {
		return processorParametersTemplates;
	}

	public void setProcessorParametersTemplates(List<ProcessorParametersTemplateViewModel> processorParametersTemplates) {
		this.processorParametersTemplates = processorParametersTemplates;
	}

}
