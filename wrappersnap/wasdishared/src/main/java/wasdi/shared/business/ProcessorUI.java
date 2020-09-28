package wasdi.shared.business;

/**
 * Entity that represents the UI of a processor
 * @author p.campanella
 *
 */
public class ProcessorUI {
	private String processorId;
	
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
