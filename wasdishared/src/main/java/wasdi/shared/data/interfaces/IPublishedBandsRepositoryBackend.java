package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.PublishedBand;

/**
 * Backend contract for published bands repository.
 */
public interface IPublishedBandsRepositoryBackend {

	boolean insertPublishedBand(PublishedBand oFile);

	PublishedBand getPublishedBand(String sProductName, String sBandName);

	List<PublishedBand> getPublishedBandsByProductName(String sProductName);

	List<PublishedBand> getList();

	int deleteByProductName(String sProductName);

	int deleteByProductNameLayerId(String sProductName, String sLayerId);
}
