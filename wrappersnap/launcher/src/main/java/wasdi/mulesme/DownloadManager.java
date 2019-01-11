package wasdi.mulesme;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import wasdi.ConfigReader;
import wasdi.filebuffer.ProviderAdapter;
import wasdi.filebuffer.ProviderAdapterSupplier;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.viewmodels.QueryResultViewModel;


public class DownloadManager {
	
//	public static Logger logger = Logger.getLogger(DownloadManager.class);

    private static String QUERY_TEMPLATE = "( footprint:\"intersects(__FOOTPRINT__)\" ) AND ( beginPosition:[__FROM__ TO __TO__] AND endPosition:[__FROM__ TO __TO__] ) AND (platformname:__PLATFORM__ AND producttype:__PRODUCTTYPE__)";
    private static SimpleDateFormat QUERY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static SimpleDateFormat ARGS_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");
    
    /**
     * footprints used to filter products
     */
	private ArrayList<String> footprints = new ArrayList<>();
	/**
	 * download destination folder
	 */
	private File destinationDir = null;
	
	private String platformName = "Sentinel-1";
	private String productType = "GRD";
    private String queryLimit = "2";
    private String querySortedBy = "ingestiondate";
    private String queryOrder = "desc";	
    private String providerName = "SENTINEL";
    private String providerUser = "";
    private String providerPassword = "";
    
    /**
     * constructor with the fottprints shape file and the download destination folder 
     * @param footprintShp
     * @param destinationDir
     * @throws Exception
     */
	public DownloadManager(File footprintShp, File destinationDir) throws Exception{
		
		this.destinationDir = destinationDir;
		
		ShapefileDataStore ds = new ShapefileDataStore(footprintShp.toURI().toURL());
		SimpleFeatureSource fs = ds.getFeatureSource();
		SimpleFeatureCollection fc = fs.getFeatures();
		FeatureIterator<SimpleFeature> fit = fc.features();		
		while (fit.hasNext()) {
			SimpleFeature f = fit.next();
			Object o = f.getDefaultGeometry();
			String wkt = null;
			if (o instanceof MultiPolygon) {
				MultiPolygon mp = (MultiPolygon) o;
				Polygon p = (Polygon)mp.getGeometryN(0);
				wkt = p.toText();				
			}
			if (o instanceof Polygon) {
				Polygon p = (Polygon) o;
				wkt = p.toText();
			}
//			System.out.println(wkt);				
			if (wkt!=null) footprints.add(wkt);
		}
		fit.close();
		ds.dispose();
	}
	
	/**
	 * search and download the products in the specified period
	 * @param from
	 * @param to
	 */
	public void download(Date from, Date to) {
		String query = QUERY_TEMPLATE
				.replaceAll("__FROM__", QUERY_DATE_FORMAT.format(from))
				.replaceAll("__TO__", QUERY_DATE_FORMAT.format(to))
				.replaceAll("__PLATFORM__", platformName)
				.replaceAll("__PRODUCTTYPE__", productType);
		//TODO read from config file
		String sDownloadProtocol = "";
		QueryExecutor executor = QueryExecutor.newInstance(providerName, providerUser, providerPassword, "0", queryLimit, querySortedBy, queryOrder, sDownloadProtocol);
		//replaced by the next one
		//DownloadFile oDownloadFile = DownloadFile.getDownloadFile("SENTINEL");
		ProviderAdapter oProviderAdapter = new ProviderAdapterSupplier().supplyProviderAdapter("SENTINEL");
		
		System.out.println("searching products between " + from + " and " + to + " for " + footprints.size() + " regions ");
		
		for (String footprint : footprints) {
			String footprintQuery = query.replaceAll("__FOOTPRINT__", footprint);
			
//			System.out.println("managing footprint " + footprint);
			
			try {
				ArrayList<QueryResultViewModel> results = executor.execute(footprintQuery);
				if (results == null) {
					System.out.println("\tno results found");
					continue;
				}
				for (QueryResultViewModel result : results) {
					
//					if (result.getTitle().contains("S1B")) continue;
					
					System.out.println("\tdownloading " + result.getSummary() + " --> " + result.getLink());
					//TODO implement subscriber interface
					//TODO subscribe
					oProviderAdapter.ExecuteDownloadFile(result.getLink(), providerUser, providerPassword, destinationDir.getAbsolutePath(), null);
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
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	public String getProductType() {
		return productType;
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}

	public String getQueryLimit() {
		return queryLimit;
	}

	public void setQueryLimit(String queryLimit) {
		this.queryLimit = queryLimit;
	}

	public String getQuerySortedBy() {
		return querySortedBy;
	}

	public void setQuerySortedBy(String querySortedBy) {
		this.querySortedBy = querySortedBy;
	}

	public String getQueryOrder() {
		return queryOrder;
	}

	public void setQueryOrder(String queryOrder) {
		this.queryOrder = queryOrder;
	}

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public String getProviderUser() {
		return providerUser;
	}

	public void setProviderUser(String providerUser) {
		this.providerUser = providerUser;
	}

	public String getProviderPassword() {
		return providerPassword;
	}

	public void setProviderPassword(String providerPassword) {
		this.providerPassword = providerPassword;
	}

	public static void main(String[] args) throws Exception {		
		Date from = ARGS_DATE_FORMAT.parse(args[2]);
		Date to = ARGS_DATE_FORMAT.parse(args[3]);
		
		DownloadManager manager = new DownloadManager(new File(args[0]), new File(args[1]));
		// 2018-09-07: SET USER AND PW from config.properties
		// TODO: Never Tested
		manager.setProviderUser(ConfigReader.getPropValue("DHUS_USER"));
		manager.setProviderUser(ConfigReader.getPropValue("DHUS_PASSWORD"));
		
		if (args.length > 4) manager.setQueryLimit(args[4]);
		manager.download(from, to);
	}
}
