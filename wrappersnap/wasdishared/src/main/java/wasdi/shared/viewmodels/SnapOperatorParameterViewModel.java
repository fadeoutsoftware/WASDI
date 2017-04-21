package wasdi.shared.viewmodels;

/**
 * Created by doy on 20/04/2017.
 */
public class SnapOperatorParameterViewModel {

    private String field;
    
    private String alias = "";

    private String itemAlias = "";

    private String defaultValue = "";

    private String label = "";

    private String unit = "";

    private String description = "";

    private String[] valueSet = {};

    private String interval = "";

    private String condition = "";

    private String pattern = "";

    private String format = "";

    private boolean notNull = false;

    private boolean notEmpty = false;

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getItemAlias() {
		return itemAlias;
	}

	public void setItemAlias(String itemAlias) {
		this.itemAlias = itemAlias;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String[] getValueSet() {
		return valueSet;
	}

	public void setValueSet(String[] valueSet) {
		this.valueSet = valueSet;
	}

	public String getInterval() {
		return interval;
	}

	public void setInterval(String interval) {
		this.interval = interval;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public boolean isNotNull() {
		return notNull;
	}

	public void setNotNull(boolean notNull) {
		this.notNull = notNull;
	}

	public boolean isNotEmpty() {
		return notEmpty;
	}

	public void setNotEmpty(boolean notEmpty) {
		this.notEmpty = notEmpty;
	}    
    
}
