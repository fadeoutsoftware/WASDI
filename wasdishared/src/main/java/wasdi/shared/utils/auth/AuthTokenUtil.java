package wasdi.shared.utils.auth;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Utility class for authentication token handling.
 * Manages both legacy WASDI session tokens and JWT tokens.
 * 
 * Token Formats:
 * - Legacy (x-session-token header): direct session ID
 * - Legacy (Authorization header): "Bearer wasdi-<session-id>"
 * - JWT (Authorization header): "Bearer <jwt-token>"
 * 
 * @author p.campanella
 *
 */
public class AuthTokenUtil {
	
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String WASDI_TOKEN_PREFIX = "wasdi-";
	
	/**
	 * Extract token from Authorization header.
	 * Handles "Bearer <token>" format.
	 * 
	 * @param sAuthHeader Authorization header value
	 * @return extracted token or empty string if invalid format
	 */
	public static String extractTokenFromAuthHeader(String sAuthHeader) {
		if (Utils.isNullOrEmpty(sAuthHeader)) {
			return "";
		}
		
		try {
			if (sAuthHeader.startsWith(BEARER_PREFIX)) {
				return sAuthHeader.substring(BEARER_PREFIX.length());
			}
		} catch (Exception oEx) {
			WasdiLog.warnLog("AuthTokenUtil.extractTokenFromAuthHeader: error extracting token: " + oEx);
		}
		
		return "";
	}
	
	/**
	 * Check if token is a legacy WASDI token.
	 * Legacy tokens are prefixed with "wasdi-" when in Authorization header.
	 * 
	 * @param sToken Token to check
	 * @return true if this is a legacy WASDI token
	 */
	public static boolean isLegacyWasdiToken(String sToken) {
		if (Utils.isNullOrEmpty(sToken)) {
			return false;
		}
		return sToken.startsWith(WASDI_TOKEN_PREFIX);
	}
	
	/**
	 * Extract the actual session ID from a legacy WASDI token.
	 * Removes the "wasdi-" prefix.
	 * 
	 * @param sToken Legacy token with "wasdi-" prefix
	 * @return session ID without prefix
	 */
	public static String extractLegacySessionId(String sToken) {
		if (Utils.isNullOrEmpty(sToken) || !isLegacyWasdiToken(sToken)) {
			return sToken;
		}
		
		try {
			return sToken.substring(WASDI_TOKEN_PREFIX.length());
		} catch (Exception oEx) {
			WasdiLog.warnLog("AuthTokenUtil.extractLegacySessionId: error extracting session ID: " + oEx);
			return sToken;
		}
	}
	
	/**
	 * Wrap a legacy session ID with the WASDI token prefix.
	 * Used when returning tokens to clients using Authorization header.
	 * 
	 * @param sSessionId Session ID from database
	 * @return token formatted as "wasdi-<session-id>"
	 */
	public static String wrapLegacySessionId(String sSessionId) {
		if (Utils.isNullOrEmpty(sSessionId)) {
			return "";
		}
		return WASDI_TOKEN_PREFIX + sSessionId;
	}
	
	/**
	 * Check if token looks like a JWT (not a legacy token).
	 * JWT tokens are longer and don't have the "wasdi-" prefix.
	 * This is a simple heuristic check.
	 * 
	 * @param sToken Token to check
	 * @return true if token appears to be JWT format
	 */
	public static boolean appearsToBeJWT(String sToken) {
		if (Utils.isNullOrEmpty(sToken)) {
			return false;
		}
		
		// JWT tokens have 3 parts separated by dots: header.payload.signature
		int iDotCount = 0;
		for (char c : sToken.toCharArray()) {
			if (c == '.') {
				iDotCount++;
			}
		}
		
		return iDotCount == 2 && !isLegacyWasdiToken(sToken);
	}
}
