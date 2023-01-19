package wasdi.shared.business;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;
import static wasdi.shared.business.UserApplicationPermission.NODE_READ;
import static wasdi.shared.business.UserApplicationPermission.NODE_WRITE;
import static wasdi.shared.business.UserApplicationPermission.ORGANIZATION_READ;
import static wasdi.shared.business.UserApplicationPermission.ORGANIZATION_WRITE;
import static wasdi.shared.business.UserApplicationPermission.PROCESSOR_PARAMETERS_TEMPLATE_READ;
import static wasdi.shared.business.UserApplicationPermission.PROCESSOR_PARAMETERS_TEMPLATE_WRITE;
import static wasdi.shared.business.UserApplicationPermission.STYLE_READ;
import static wasdi.shared.business.UserApplicationPermission.STYLE_WRITE;
import static wasdi.shared.business.UserApplicationPermission.SUBSCRIPTION_READ;
import static wasdi.shared.business.UserApplicationPermission.SUBSCRIPTION_WRITE;
import static wasdi.shared.business.UserApplicationPermission.USER_READ;
import static wasdi.shared.business.UserApplicationPermission.USER_WRITE;
import static wasdi.shared.business.UserApplicationPermission.WORKSPACE_READ;
import static wasdi.shared.business.UserApplicationPermission.WORKSPACE_WRITE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public enum UserApplicationRole {

	ADMIN(new HashSet<UserApplicationPermission>(
			Arrays.asList(ADMIN_DASHBOARD, NODE_READ, NODE_WRITE, STYLE_READ, STYLE_WRITE, USER_READ, USER_WRITE, WORKSPACE_READ,
					ORGANIZATION_READ, ORGANIZATION_WRITE, SUBSCRIPTION_READ, SUBSCRIPTION_WRITE,
					WORKSPACE_WRITE, PROCESSOR_PARAMETERS_TEMPLATE_READ, PROCESSOR_PARAMETERS_TEMPLATE_WRITE))),

	DEVELOPER(new HashSet<UserApplicationPermission>(Arrays.asList(ADMIN_DASHBOARD, NODE_READ, NODE_WRITE,
			ORGANIZATION_READ, ORGANIZATION_WRITE, SUBSCRIPTION_READ, SUBSCRIPTION_WRITE,
			WORKSPACE_READ, WORKSPACE_WRITE, PROCESSOR_PARAMETERS_TEMPLATE_READ, PROCESSOR_PARAMETERS_TEMPLATE_WRITE))),

	USER(new HashSet<UserApplicationPermission>(Arrays.asList(WORKSPACE_READ, WORKSPACE_WRITE,
			ORGANIZATION_READ, ORGANIZATION_WRITE, SUBSCRIPTION_READ, SUBSCRIPTION_WRITE)));

	private static final Map<String, UserApplicationRole> ENUM_MAP;

	private final Set<UserApplicationPermission> permissions;

	static {
		ENUM_MAP = Arrays.stream(UserApplicationRole.values())
				.collect(Collectors.toMap(UserApplicationRole::name, Function.identity()));
	}

	UserApplicationRole(Set<UserApplicationPermission> permissions) {
		this.permissions = permissions;
	}

	public Set<UserApplicationPermission> getPermissions() {
		return this.permissions;
	}

	public Set<String> getGrantedAuthorities() {
		Set<String> authorities = getPermissions().stream().map(UserApplicationPermission::getPermission)
				.collect(Collectors.toSet());

		authorities.add("ROLE_" + this.name());

		return authorities;
	}

	public static UserApplicationRole get(String name) {
		return ENUM_MAP.get(name.toUpperCase());
	}

	public static boolean userHasRightsToAccessApplicationResource(String sUserRole,
			UserApplicationPermission ePermission) {
		boolean bResult = false;

		if (Utils.isNullOrEmpty(sUserRole)) {
			WasdiLog.debugLog("UserApplicationRole.userHasRightsToAccessResource() | The user role (" + sUserRole
					+ "): is invalid");
		} else if (ePermission == null) {
			WasdiLog.debugLog("UserApplicationRole.userHasRightsToAccessResource() | The permission (" + ePermission
					+ "): is invalid");
		} else {
			UserApplicationRole oUserApplicationRole = UserApplicationRole.get(sUserRole);
			Set<String> asGrantedAuthorities = oUserApplicationRole.getGrantedAuthorities();

			if (asGrantedAuthorities != null && asGrantedAuthorities.contains(ePermission.getPermission())) {
				bResult = true;
			} else {
				WasdiLog.debugLog("UserApplicationRole.userHasRightsToAccessResource() | The user role " + sUserRole
						+ " does not have the rights to access the resource (" + ePermission.name() + ").");
			}
		}

		return bResult;
	}

}
