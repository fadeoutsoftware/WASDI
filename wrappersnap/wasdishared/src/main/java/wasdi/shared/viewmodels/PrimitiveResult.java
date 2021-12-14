package wasdi.shared.viewmodels;

/**
 * Generic Result with primitive value. 
 * It can have different values:	
 *		.Integer
 *		.Bool
 *		.String
 *		.Double
 * 
 * Each API can valorize one of more of these values
 *
 * Created by p.campanella on 14/10/2016.
 */
public class PrimitiveResult {

    private Integer IntValue;
    private String StringValue;
    private Double DoubleValue;
    private Boolean BoolValue;
    
    //singleton pattern
    private static PrimitiveResult s_oInvalid;
    
    static {
    	s_oInvalid = new PrimitiveResult();
    	s_oInvalid.IntValue = null;
    	s_oInvalid.StringValue = null;
    	s_oInvalid.DoubleValue = null;
    	s_oInvalid.BoolValue = false;
    }
    
    public PrimitiveResult(PrimitiveResult oPres) {
    	this.IntValue = oPres.IntValue;
    	this.StringValue = oPres.StringValue;
    	this.DoubleValue = oPres.DoubleValue;
    	this.BoolValue = oPres.BoolValue;
    }
    
    public static PrimitiveResult getInvalidInstance() {
    	return new PrimitiveResult(getInvalid());
    }
    
    public PrimitiveResult() {
	}

	public static PrimitiveResult getInvalid() {
    	return s_oInvalid;
    }

    public Integer getIntValue() {
        return IntValue;
    }

    public void setIntValue(Integer intValue) {
        IntValue = intValue;
    }

    public String getStringValue() {
        return StringValue;
    }

    public void setStringValue(String stringValue) {
        StringValue = stringValue;
    }

    public Double getDoubleValue() {
        return DoubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        DoubleValue = doubleValue;
    }

    public Boolean getBoolValue() {
        return BoolValue;
    }

    public void setBoolValue(Boolean boolValue) {
        BoolValue = boolValue;
    }
}
