package wasdi.mulesme;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import wasdi.ConfigReader;
import wasdi.filebuffer.ProviderAdapter;
import wasdi.filebuffer.ProviderAdapterFactory;
import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.opensearch.QueryExecutorFactory;
import wasdi.shared.utils.AuthenticationCredentials;
import wasdi.shared.viewmodels.QueryResultViewModel;


public class DownloadManager {
	
//	public static Logger logger = Logger.getLogger(DownloadManager.class);

    private static String s_sQUERY_TEMPLATE = "( footprint:\"intersects(__FOOTPRINT__)\" ) AND ( beginPosition:[__FROM__ TO __TO__] AND endPosition:[__FROM__ TO __TO__] ) AND (platformname:__PLATFORM__ AND producttype:__PRODUCTTYPE__)";
    private static SimpleDateFormat s_oQUERY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static SimpleDateFormat s_oARGS_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");
    
    /**
     * footprints used to filter products
     */
	private ArrayList<String> m_asfootprints = new ArrayList<>();
	/**
	 * download destination folder
	 */
	private File m_odestinationDir = null;
	
	private String m_splatformName = "Sentinel-1";
	private String m_sproductType = "GRD";
    private String m_squeryLimit = "2";
    private String m_squerySortedBy = "ingestiondate";
    private String m_squeryOrder = "desc";	
    private String m_sproviderName = "SENTINEL";
    private String m_sproviderUser = "";
    private String m_sproviderPassword = "";
    
    /**
     * constructor with the footprints shape file and the download destination folder 
     * @param oFootprintShp
     * @param oDestinationDir
     * @throws Exception
     */
	public DownloadManager(File oFootprintShp, File oDestinationDir) throws Exception{
		
		this.m_odestinationDir = oDestinationDir;
		
		ShapefileDataStore oDataStore = new ShapefileDataStore(oFootprintShp.toURI().toURL());
		SimpleFeatureSource oFeatureSource = oDataStore.getFeatureSource();
		SimpleFeatureCollection oFeatureCollection = oFeatureSource.getFeatures();
		FeatureIterator<SimpleFeature> oFeatureIterator = oFeatureCollection.features();		
		while (oFeatureIterator.hasNext()) {
			SimpleFeature oFeature = oFeatureIterator.next();
			Object oDefaultGeom = oFeature.getDefaultGeometry();
			String oWkt = null;
			if (oDefaultGeom instanceof MultiPolygon) {
				MultiPolygon oMultiPolygon = (MultiPolygon) oDefaultGeom;
				Polygon oPolygon = (Polygon)oMultiPolygon.getGeometryN(0);
				oWkt = oPolygon.toText();				
			}
			if (oDefaultGeom instanceof Polygon) {
				Polygon oPolygon = (Polygon) oDefaultGeom;
				oWkt = oPolygon.toText();
			}
//			System.out.println(wkt);				
			if (oWkt!=null) m_asfootprints.add(oWkt);
		}
		oFeatureIterator.close();
		oDataStore.dispose();
	}
	
	/**
	 * search and download the products in the specified period
	 * @param from
	 * @param to
	 */
	public void download(Date from, Date to) {
		String query = s_sQUERY_TEMPLATE
				.replaceAll("__FROM__", s_oQUERY_DATE_FORMAT.format(from))
				.replaceAll("__TO__", s_oQUERY_DATE_FORMAT.format(to))
				.replaceAll("__PLATFORM__", m_splatformName)
				.replaceAll("__PRODUCTTYPE__", m_sproductType);
		//TODO read from config file
		String sDownloadProtocol = "";
		QueryExecutorFactory oFactory = new QueryExecutorFactory();
		AuthenticationCredentials oCredentials = new AuthenticationCredentials(m_sproviderUser, m_sproviderPassword);
		QueryExecutor oExecutor = oFactory.getExecutor(m_sproviderName, oCredentials,
				//"0", m_squeryLimit, m_squerySortedBy, m_squeryOrder,
				sDownloadProtocol, "true");
		
		//replaced by the next one
		//DownloadFile oDownloadFile = DownloadFile.getDownloadFile("SENTINEL");
		ProviderAdapter oProviderAdapter = new ProviderAdapterFactory().supplyProviderAdapter("SENTINEL");
		
		System.out.println("searching products between " + from + " and " + to + " for " + m_asfootprints.size() + " regions ");
		
		for (String sFootprint : m_asfootprints) {
			String sFootprintQuery = query.replaceAll("__FOOTPRINT__", sFootprint);
			
//			System.out.println("managing footprint " + footprint);
			
			try {
				PaginatedQuery oQuery = new PaginatedQuery(sFootprintQuery, "0", m_squeryLimit, m_squerySortedBy, m_squeryOrder ); 
				List<QueryResultViewModel> aoResults = oExecutor.executeAndRetrieve(oQuery);
				if (aoResults == null) {
					System.out.println("\tno results found");
					continue;
				}
				for (QueryResultViewModel oResult : aoResults) {
					
//					if (result.getTitle().contains("S1B")) continue;
					
					System.out.println("\tdownloading " + oResult.getSummary() + " --> " + oResult.getLink());
					//TODO implement subscriber interface
					//TODO subscribe
					oProviderAdapter.ExecuteDownloadFile(oResult.getLink(), m_sproviderUser, m_sproviderPassword, m_odestinationDir.getAbsolutePath(), null);
					//TODO unsubscribe
					
					System.out.println("\t\tdone");
				}					
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			System.out.println("end");
		}
	}
	
	public String getPlatformName() {
		return m_splatformName;
	}

	public void setPlatformName(String platformName) {
		this.m_splatformName = platformName;
	}

	public String getProductType() {
		return m_sproductType;
	}

	public void setProductType(String productType) {
		this.m_sproductType = productType;
	}

	public String getQueryLimit() {
		return m_squeryLimit;
	}

	public void setQueryLimit(String queryLimit) {
		this.m_squeryLimit = queryLimit;
	}

	public String getQuerySortedBy() {
		return m_squerySortedBy;
	}

	public void setQuerySortedBy(String querySortedBy) {
		this.m_squerySortedBy = querySortedBy;
	}

	public String getQueryOrder() {
		return m_squeryOrder;
	}

	public void setQueryOrder(String queryOrder) {
		this.m_squeryOrder = queryOrder;
	}

	public String getProviderName() {
		return m_sproviderName;
	}

	public void setProviderName(String providerName) {
		this.m_sproviderName = providerName;
	}

	public String getProviderUser() {
		return m_sproviderUser;
	}

	public void setProviderUser(String providerUser) {
		this.m_sproviderUser = providerUser;
	}

	public String getProviderPassword() {
		return m_sproviderPassword;
	}

	public void setProviderPassword(String providerPassword) {
		this.m_sproviderPassword = providerPassword;
	}

	public static void main(String[] args) throws Exception {		
		Date from = s_oARGS_DATE_FORMAT.parse(args[2]);
		Date to = s_oARGS_DATE_FORMAT.parse(args[3]);
		
		DownloadManager manager = new DownloadManager(new File(args[0]), new File(args[1]));
		// 2018-09-07: SET USER AND PW from config.properties
		// TODO: Never Tested
		manager.setProviderUser(ConfigReader.getPropValue("DHUS_USER"));
		manager.setProviderUser(ConfigReader.getPropValue("DHUS_PASSWORD"));
		
		if (args.length > 4) manager.setQueryLimit(args[4]);
		manager.download(from, to);
	}
}
