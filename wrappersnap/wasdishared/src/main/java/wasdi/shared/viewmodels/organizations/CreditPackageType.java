package wasdi.shared.viewmodels.organizations;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CreditPackageType {
	
	Fifty("Fifty", "Fifty credits", "Fifty credits"),
	OneHndred("OneHundred", "One hundred credits", "One hundred credits");
	
	private String typeId;
	private String typeName;
	private String typeDescription;

	private static final Map<String, CreditPackageType> ENUM_MAP;
	
	private CreditPackageType(String sId, String sName, String sDescription) {
		this.typeId = sId;
		this.typeName = sName;
		this.typeDescription = sDescription;
	}

	static {
		ENUM_MAP = Arrays.stream(CreditPackageType.values())
				.collect(Collectors.toMap(CreditPackageType::name, Function.identity()));
	}

	public static CreditPackageType get(String name) {
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

	public static Map<String, CreditPackageType> getEnumMap() {
		return ENUM_MAP;
	}

}
