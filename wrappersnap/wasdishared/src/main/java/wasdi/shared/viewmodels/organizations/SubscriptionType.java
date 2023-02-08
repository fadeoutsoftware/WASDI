package wasdi.shared.viewmodels.organizations;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubscriptionType {

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

	static {
		ENUM_MAP = Arrays.stream(SubscriptionType.values())
				.collect(Collectors.toMap(SubscriptionType::name, Function.identity()));
	}

	public static SubscriptionType get(String name) {
		return ENUM_MAP.get(name);
	}

}
