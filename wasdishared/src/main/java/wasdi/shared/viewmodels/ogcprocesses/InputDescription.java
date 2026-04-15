package wasdi.shared.viewmodels.ogcprocesses;

public class InputDescription extends DescriptionType {
	private Integer minOccurs = 1;
	private Object maxOccurs = null;
	private Schema schema = null;
	
	public Integer getMinOccurs() {
		return minOccurs;
	}
	public void setMinOccurs(Integer minOccurs) {
		this.minOccurs = minOccurs;
	}
	public Object getMaxOccurs() {
		return maxOccurs;
	}
	public void setMaxOccurs(Object maxOccurs) {
		this.maxOccurs = maxOccurs;
	}
	public Schema getSchema() {
		return schema;
	}
	public void setSchema(Schema schema) {
		this.schema = schema;
	}	

}
