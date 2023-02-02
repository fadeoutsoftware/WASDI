package it.fadeout.business;

/**
 * Enum with the name of the different allowed Image Collections in WASDI
 * 
 * @author p.campanella
 *
 */
public enum ImagesCollections {	
	PROCESSORS("processors"),
	USERS("users"),
	ORGANIZATIONS("organizations");
	
	private String m_sFolder;
	
	ImagesCollections(String sFolder) {
		this.m_sFolder=sFolder;
	}
	public String getFolder() {
		return m_sFolder;
	}
	
}
