package wasdi.shared.viewmodels.products;

/**
 * Created by s.adamo on 18/05/2016.
 */
public class AttributeViewModel {

    private int dataType;
    private long numElems;
    //private String name;
    private String description;
    private String data;
    
    public int getDataType() {
        return dataType;
    }
    public void setDataType(int dataType) {
        this.dataType = dataType;
    }
    public long getNumElems() { 
        return numElems;
    }
    public void setNumElems(long numElems) {
        this.numElems = numElems;
    }
//    public String getName() {
//        return name;
//    }
//    public void setName(String name) {
//        this.name = name;
//    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
}
