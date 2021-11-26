package wasdi.shared.viewmodels.processors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ProcessorParametersTemplate Detail View Model
 * 
 * Represents a ProcessorParametersTemplate
 * 
 * @author PetruPetrescu
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessorParametersTemplateDetailViewModel {

	private String templateId;
	private String userId;
	private String processorId;
	private String name;
	private String description;
	private String jsonParameters;
    private String creationDate;
	private String updateDate;

}
