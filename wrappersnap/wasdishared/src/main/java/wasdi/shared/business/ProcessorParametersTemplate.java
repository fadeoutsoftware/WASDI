package wasdi.shared.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parameters template for processor by user Cross-table for users and
 * processors (applications). It enables a specific user to run a specific
 * processor with a specific set of parameters. The user can have more such
 * parameter templates for a specific processor.
 * 
 * @author PetruPetrescu
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessorParametersTemplate {

	/** Identifier of the template */
	private String templateId;

	/** User owner of the processor */
	private String userId;

	/** Identifier of the processor */
	private String processorId;

	/** Processor Name */
	private String name;

	/** Processor Description */
	private String description;

	/** Sample JSON Parameter */
	private String jsonParameters;

}
