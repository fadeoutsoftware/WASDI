package wasdi.shared.business.processors;

/**
 * Entity that represents the UI of a processor.
 * User Interfaces are a JSON file.
 * 
 * This JSON has the goal to associate to each processor parameter a user control.
 * The web app is able from this JSON to create an interface for each parameter that the
 * user can access directly on the web.
 * Each control then can convert the input of the user in the right JSON value of the parameter.
 * 
 * The user can also group set of parameters in different tabs/sections.
 * 
 * The supported elements are:
 * 
 * Render As Strings: flag to decide if the input params are represented as strings or with the native value.
 * Tab: section that groups controls. It has a collection of controls.
 * Textbox: text box input
 * Dropdown: generic combo box
 * Select Area: bounding box input as map
 * Number Slider: numeric input, represented as a slider
 * Date: date input represented as a calendar
 * Bool: boolean value, represented as a switch
 * Product Combo Box: special combo box that lists all the products in a workspace
 * Search EO Image: mini-search, not used at the moment
 * Hidden Field: field not shown in the interface but that can be converted in a parameter
 * 
 * @author p.campanella
 *
 */
public class ProcessorUI {
	
	/**
	 * Id of the processor
	 */
	private String processorId;
	
	/**
	 * jSON of the interface
	 */
	private String ui;
	
	/**
	 * Get processor UI JSON
	 * @return
	 */
	public String getUi() {
		return ui;
	}
	
	/**
	 * Set processor UI JSON
	 * @param ui
	 */
	public void setUi(String ui) {
		this.ui = ui;
	}
	
	public String getProcessorId() {
		return processorId;
	}

	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}
	

}
