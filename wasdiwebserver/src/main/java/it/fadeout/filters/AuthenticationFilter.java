package it.fadeout.filters;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import it.fadeout.Wasdi;
import wasdi.shared.business.users.User;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.auth.AuthTokenUtil;
import wasdi.shared.utils.log.WasdiLog;

/**
 * JAX-RS ContainerRequestFilter for authentication.
 * 
 * Handles both legacy and modern authentication methods:
 * 1. x-session-token header (legacy, backward compatible)
 * 2. Authorization header with Bearer token:
 *    - "Bearer wasdi-<session-id>" for legacy tokens
 *    - "Bearer <jwt-token>" for JWT tokens (Phase 2)
 * 
 * The filter attempts to validate the token and inject the authenticated User
 * into the request context. If authentication fails, it sets null as the user,
 * which endpoints already validate for.
 * 
 * No exceptions are thrown - follows WASDI philosophy of graceful degradation.
 * 
 * @author p.campanella
 *
 */
@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
	
	private static final String CONTEXT_USER_KEY = "authenticated-user";
	private static final String HEADER_SESSION_TOKEN = "x-session-token";
	private static final String HEADER_AUTHORIZATION = "Authorization";
	
	@Override
	public void filter(ContainerRequestContext oRequestContext) throws IOException {
		
		User oUser = null;
		
		try {
			// Try to get token from headers
			String sToken = extractToken(oRequestContext);
			
			if (!Utils.isNullOrEmpty(sToken)) {
				// Validate token and get user
				oUser = validateAndGetUser(sToken);
			}
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("AuthenticationFilter.filter: error during authentication: " + oEx);
		}
		
		// Always set user in context (can be null - endpoints will handle)
		oRequestContext.setProperty(CONTEXT_USER_KEY, oUser);
	}
	
	/**
	 * Extract authentication token from request headers.
	 * Priority:
	 * 1. x-session-token header (legacy, direct)
	 * 2. Authorization header (Bearer token)
	 * 
	 * @param oRequestContext JAX-RS request context
	 * @return token string or empty if not found
	 */
	private String extractToken(ContainerRequestContext oRequestContext) {
		
		// First try legacy header
		String sToken = oRequestContext.getHeaderString(HEADER_SESSION_TOKEN);
		if (!Utils.isNullOrEmpty(sToken)) {
			WasdiLog.debugLog("AuthenticationFilter.extractToken: found x-session-token header");
			return sToken;
		}
		
		// Then try Authorization header
		String sAuthHeader = oRequestContext.getHeaderString(HEADER_AUTHORIZATION);
		if (!Utils.isNullOrEmpty(sAuthHeader)) {
			sToken = AuthTokenUtil.extractTokenFromAuthHeader(sAuthHeader);
			if (!Utils.isNullOrEmpty(sToken)) {
				WasdiLog.debugLog("AuthenticationFilter.extractToken: found Authorization header with token");
				return sToken;
			}
		}
		
		return "";
	}
	
	/**
	 * Validate token and retrieve associated User.
	 * Discriminates between legacy WASDI tokens and JWT tokens.
	 * 
	 * @param sToken Token to validate
	 * @return User object if valid, null otherwise
	 */
	private User validateAndGetUser(String sToken) {
		
		if (Utils.isNullOrEmpty(sToken)) {
			return null;
		}
		
		try {
			if (AuthTokenUtil.isLegacyWasdiToken(sToken)) {
				// Legacy token - extract session ID and check DB
				String sSessionId = AuthTokenUtil.extractLegacySessionId(sToken);
				return validateLegacyToken(sSessionId);
			} else if (AuthTokenUtil.appearsToBeJWT(sToken)) {
				// JWT token - validate with Keycloak (Phase 2)
				return validateJWTToken(sToken);
			} else {
				// Treat as direct session ID (x-session-token format)
				return validateLegacyToken(sToken);
			}
		} catch (Exception oEx) {
			WasdiLog.warnLog("AuthenticationFilter.validateAndGetUser: error validating token: " + oEx);
			return null;
		}
	}
	
	/**
	 * Validate a legacy WASDI session token.
	 * Checks the session database.
	 * 
	 * @param sSessionId Session ID
	 * @return User object if valid, null otherwise
	 */
	private User validateLegacyToken(String sSessionId) {
		try {
			if (!Utils.isNullOrEmpty(sSessionId)) {
				return Wasdi.getUserFromSession(sSessionId);
			}
		} catch (Exception oEx) {
			WasdiLog.warnLog("AuthenticationFilter.validateLegacyToken: error validating legacy token: " + oEx);
		}
		return null;
	}
	
	/**
	 * Validate a JWT token with Keycloak.
	 * Phase 2 implementation.
	 * 
	 * @param sJwtToken JWT token
	 * @return User object if valid, null otherwise
	 */
	private User validateJWTToken(String sJwtToken) {
		// TODO: Phase 2 - Implement JWT validation with Keycloak
		WasdiLog.debugLog("AuthenticationFilter.validateJWTToken: JWT validation not yet implemented (Phase 2)");
		return null;
	}
}
