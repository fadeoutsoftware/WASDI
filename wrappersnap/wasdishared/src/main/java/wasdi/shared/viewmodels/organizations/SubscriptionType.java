package wasdi.shared.viewmodels.organizations;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum SubscriptionType {

	Free("Free", "Free", "Free"),
	OneDayStandard("OneDayStandard", "One Day Standard", "One Day Standard"),
	OneWeekStandard("OneWeekStandard", "One Week Standard", "One Week Standard"),
	OneMonthStandard("OneMonthStandard", "One Month Standard", "One Month Standard"),
	OneYearStandard("OneYearStandard", "One Year Standard", "One Year Standard"),
	OneMonthProfessional("OneMonthProfessional", "One Month Professional", "One Month Professional"),
	OneYearProfessional("OneYearProfessional", "One Year Professional", "One Year Professional");

	private String typeId;
	private String typeName;
	private String typeDescription;

	private static final Map<String, SubscriptionType> ENUM_MAP;
	
	private SubscriptionType(String sId, String sName, String sDescription) {
		this.typeId = sId;
		this.typeName = sName;
		this.typeDescription = sDescription;
	}

	static {
		ENUM_MAP = Arrays.stream(SubscriptionType.values())
				.collect(Collectors.toMap(SubscriptionType::name, Function.identity()));
	}

	public static SubscriptionType get(String name) {
		return ENUM_MAP.get(name);
	}

	public String getTypeId() {
		return typeId;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getTypeDescription() {
		return typeDescription;
	}

	public void setTypeDescription(String typeDescription) {
		this.typeDescription = typeDescription;
	}

	public static Map<String, SubscriptionType> getEnumMap() {
		return ENUM_MAP;
	}

}
