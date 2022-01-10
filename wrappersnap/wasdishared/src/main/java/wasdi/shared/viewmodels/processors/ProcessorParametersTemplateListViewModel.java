package wasdi.shared.viewmodels.processors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ProcessorParametersTemplate List View Model
 * 
 * Wraps a list of comments view models
 * 
 * @author PetruPetrescu
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessorParametersTemplateListViewModel {

	private String templateId;
	private String userId;
	private String processorId;
	private String name;
	private String updateDate;

}
