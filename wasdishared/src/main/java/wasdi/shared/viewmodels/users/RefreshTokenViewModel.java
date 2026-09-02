package wasdi.shared.viewmodels.users;

/**
 * Refresh-token request payload.
 */
public class RefreshTokenViewModel {

	private String refreshToken;

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}