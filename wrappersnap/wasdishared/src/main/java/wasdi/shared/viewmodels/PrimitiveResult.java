package wasdi.shared.viewmodels;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class PrimitiveResult {

    private Integer IntValue;
    private String StringValue;
    private Double DoubleValue;
    private Boolean BoolValue;

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
