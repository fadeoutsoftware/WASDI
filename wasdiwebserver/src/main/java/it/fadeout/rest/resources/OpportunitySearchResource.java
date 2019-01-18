package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nfs.orbits.CoverageTool.Polygon;
import org.nfs.orbits.CoverageTool.apoint;
import org.nfs.orbits.sat.CoverageSwathResult;
import org.nfs.orbits.sat.SatFactory;
import org.nfs.orbits.sat.SatSensor;
import org.nfs.orbits.sat.Satellite;
import org.nfs.orbits.sat.SwathArea;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import it.fadeout.Wasdi;
import it.fadeout.business.InstanceFinder;
import it.fadeout.viewmodels.CoverageSwathResultViewModel;
import it.fadeout.viewmodels.OrbitFilterViewModel;
import it.fadeout.viewmodels.OrbitSearchViewModel;
import it.fadeout.viewmodels.SatelliteOrbitResultViewModel;
import it.fadeout.viewmodels.SatelliteResourceViewModel;
import satLib.astro.time.Time;
import wasdi.shared.business.User;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.Utils;

@Path("/searchorbit")
public class OpportunitySearchResource {
	@Context
	ServletConfig m_oServletConfig;
	
	CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();

	@POST
	@Path("/search")
	@Produces({ "application/xml", "application/json", "text/html" })
	@Consumes(MediaType.APPLICATION_JSON)
	public ArrayList<CoverageSwathResultViewModel> Search(@HeaderParam("x-session-token") String sSessionId,
			OrbitSearchViewModel OrbitSearch) {
		Wasdi.DebugLog("OpportunitySearchResource.Search");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ArrayList<CoverageSwathResultViewModel> aoCoverageSwathResultViewModels = new ArrayList<CoverageSwathResultViewModel>();

		try {
			if (oUser == null) {
				return aoCoverageSwathResultViewModels;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoCoverageSwathResultViewModels;
			}

			if (OrbitSearch == null)
				return null;

			// set nfs properties download
			String userHome = System.getProperty("user.home");
			String Nfs = System.getProperty("nfs.data.download");
			if (Nfs == null)
				System.setProperty("nfs.data.download", userHome + "/nfs/download");

			System.out.println("nfs dir " + System.getProperty("nfs.data.download"));

			Date dtDate = new Date();
			String sArea = OrbitSearch.getPolygon();
			int iIdCoverageCounter = 1;

			// Foreach filter combination found
			List<OrbitFilterViewModel> oOrbitFilters = OrbitSearch.getOrbitFilters();
			for (OrbitFilterViewModel oOrbitFilter : oOrbitFilters) {

				// Find the opportunities
				ArrayList<CoverageSwathResult> aoCoverageSwathResult = new ArrayList<>();
				try {
					aoCoverageSwathResult = InstanceFinder.findSwatsByFilters(sArea,
							OrbitSearch.getAcquisitionStartTime(), OrbitSearch.getAcquisitionEndTime(),
							OrbitSearch.getSatelliteNames(), oOrbitFilter.getSensorResolution(),
							oOrbitFilter.getSensorType(),OrbitSearch.getLookingType(),OrbitSearch.getViewAngle(),OrbitSearch.getSwathSize());
				} 
				catch (ParseException e) {
					e.printStackTrace();
				}

				if (aoCoverageSwathResult == null)
					return null;

				// For each Swat Result
				for (CoverageSwathResult oSwatResul : aoCoverageSwathResult) {
					// Get View Model and Childs
					ArrayList<CoverageSwathResultViewModel> aoModels = getSwatViewModelFromResult(oSwatResul);
					for (CoverageSwathResultViewModel oCoverageSwathResultViewModel : aoModels) {
						oCoverageSwathResultViewModel.IdCoverageSwathResultViewModel = iIdCoverageCounter;
						iIdCoverageCounter++;
						aoCoverageSwathResultViewModels.add(oCoverageSwathResultViewModel);
					}
				}

			}

			return aoCoverageSwathResultViewModels;
		} catch (Exception oEx) {
			System.out.println("OpportunitySearchResource.Search: Error searching opportunity " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return aoCoverageSwathResultViewModels;
	}

	private ArrayList<CoverageSwathResultViewModel> getSwatViewModelFromResult(CoverageSwathResult oSwath) {
		ArrayList<CoverageSwathResultViewModel> aoResults = new ArrayList<CoverageSwathResultViewModel>();
		CoverageSwathResultViewModel oVM = new CoverageSwathResultViewModel();
		if (oSwath != null) {

			oVM.SwathName = oSwath.getSwathName();
			oVM.IsAscending = oSwath.isAscending();
			if (oSwath.getSat() != null) {
				oVM.SatelliteName = oSwath.getSat().getName();
			}

			if (oSwath.getSat() != null) {
				oVM.SensorName = oSwath.getSensor().getSName();

				if (oSwath.getSat().getType() != null)
					oVM.SensorType = oSwath.getSat().getType().name();

			}

			if (oSwath.getCoveredArea() != null)
				oVM.CoveredAreaName = oSwath.getCoveredArea().getName();

			if (oSwath.getSensor() != null) {
				if (oSwath.getSensor().getLooking() != null)
					oVM.SensorLookDirection = oSwath.getSensor().getLooking().toString();
			}

			if (oSwath.getTimeStart() != null) {
				GregorianCalendar oCalendar = oSwath.getTimeStart().getCurrentGregorianCalendar();
				// oVM.AcquisitionStartTime = oCalendar.getTime();
				oVM.AcquisitionStartTime = new Date(oCalendar.getTimeInMillis());
			}
			if (oSwath.getTimeEnd() != null) {
				GregorianCalendar oCalendar = oSwath.getTimeEnd().getCurrentGregorianCalendar();
				// oVM.AcquisitionEndTime = oCalendar.getTime();
				oVM.AcquisitionEndTime = new Date(oCalendar.getTimeInMillis());
			}

			oVM.AcquisitionDuration = oSwath.getDuration();

			if (oSwath.getFootprint() != null) {
				Polygon oPolygon = oSwath.getFootprint();
				apoint[] aoPoints = oPolygon.getVertex();

				if (aoPoints != null) {

					oVM.SwathFootPrint = "POLYGON((";

					for (int iPoints = 0; iPoints < aoPoints.length; iPoints++) {
						apoint oPoint = aoPoints[iPoints];
						oVM.SwathFootPrint += "" + (oPoint.x * 180.0 / Math.PI);
						oVM.SwathFootPrint += " ";
						oVM.SwathFootPrint += "" + (oPoint.y * 180.0 / Math.PI);
						oVM.SwathFootPrint += ",";
					}

					oVM.SwathFootPrint = oVM.SwathFootPrint.substring(0, oVM.SwathFootPrint.length() - 2);

					oVM.SwathFootPrint += "))";
				}
			}

			List<SwathArea> aoAreas = oSwath.getChilds();

			for (SwathArea oArea : aoAreas) {

				CoverageSwathResultViewModel oSwathResult = new CoverageSwathResultViewModel(oVM);

				if (oArea.getMode() != null) {
					oSwathResult.SensorMode = oArea.getMode().getName();
					if (oArea.getMode().getViewAngle() != null)
						oSwathResult.Angle = oArea.getMode().getViewAngle().toString();
				}

				if (oArea.getswathSize() != null) {
					oSwathResult.CoverageLength = oArea.getswathSize().getLength();
					oSwathResult.CoverageWidth = oArea.getswathSize().getWidth();
				}

				oSwathResult.Coverage = oArea.getCoverage() * 100;

				if (oArea.getswathSize() != null) {
					oSwathResult.CoverageWidth = oArea.getswathSize().getWidth();
					oSwathResult.CoverageLength = oArea.getswathSize().getLength();
				}

				if (oArea.getFootprint() != null) {
					Polygon oPolygon = oArea.getFootprint();
					apoint[] aoPoints = oPolygon.getVertex();

					if (aoPoints != null) {

						oSwathResult.FrameFootPrint = "POLYGON((";

						for (int iPoints = 0; iPoints < aoPoints.length; iPoints++) {
							apoint oPoint = aoPoints[iPoints];
							oSwathResult.FrameFootPrint += "" + (oPoint.x * 180.0 / Math.PI);
							oSwathResult.FrameFootPrint += " ";
							oSwathResult.FrameFootPrint += "" + (oPoint.y * 180.0 / Math.PI);
							oSwathResult.FrameFootPrint += ",";
						}

						oSwathResult.FrameFootPrint = oSwathResult.FrameFootPrint.substring(0,
								oSwathResult.FrameFootPrint.length() - 2);

						oSwathResult.FrameFootPrint += "))";
					}
				}

				aoResults.add(oSwathResult);
			}

		}
		return aoResults;
	}

	@GET
	@Path("/track/{satellitename}")
	@Produces({ "application/xml", "application/json", "text/html" })
	@Consumes(MediaType.APPLICATION_JSON)
	public SatelliteOrbitResultViewModel getSatelliteTrack(@HeaderParam("x-session-token") String sSessionId,
			@PathParam("satellitename") String satname) {

		Wasdi.DebugLog("OpportunitySearchResource.GetSatelliteTrack");

		// set nfs properties download
		String userHome = System.getProperty("user.home");
		String Nfs = System.getProperty("nfs.data.download");
		if (Nfs == null) {
			System.setProperty("nfs.data.download", userHome + "/nfs/download");
			System.out.println("nfs dir " + System.getProperty("nfs.data.download"));
		}

		SatelliteOrbitResultViewModel ret = new SatelliteOrbitResultViewModel();
		String satres = InstanceFinder.s_sOrbitSatsMap.get(satname);
		try {

			Time tconv = new Time();
			tconv.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			Satellite sat = SatFactory.buildSat(satres);

			ret.code = satname;

			ret.satelliteName = sat.getDescription();
			sat.getSatController().moveToNow();
			ret.currentTime = tconv.convertJD2String(sat.getOrbitCore().getCurrentJulDate());
			ret.setCurrentPosition(sat.getOrbitCore().getLLA());

			sat.getOrbitCore().setShowGroundTrack(true);

			// lag
			double[] tm = sat.getOrbitCore().getTimeLag();
			for (int i = sat.getOrbitCore().getNumGroundTrackLagPts() - 1; i >= 0; i--)
				ret.addPosition(sat.getOrbitCore().getGroundTrackLlaLagPt(i), tconv.convertJD2String(tm[i]));

			// lead
			tm = sat.getOrbitCore().getTimeLead();
			for (int i = 0; i < sat.getOrbitCore().getNumGroundTrackLeadPts(); i++)
				ret.addPosition(sat.getOrbitCore().getGroundTrackLlaLeadPt(i), tconv.convertJD2String(tm[i]));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	@GET
	@Path("/getkmlsearchresults")
	@Produces({ "application/xml"})//, "application/json", "text/html" 
	//@Consumes(MediaType.APP)
	@Consumes(MediaType.APPLICATION_XML)
	public Kml getKmlSearchResults ()
	{
		final Kml kml = new Kml();
		kml.createAndSetPlacemark()
		   .withName("London, UK").withOpen(Boolean.TRUE)
		   .createAndSetPoint().addToCoordinates(-0.126236, 51.500152);
		
		/*try {
			kml.marshal(new File("C:\\temp\\wasdi\\HelloKml.kml"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}*/
		return kml;
	}
	@GET
	@Path("/updatetrack/{satellitesname}")
	@Produces({ "application/xml", "application/json", "text/html" })
	@Consumes(MediaType.APPLICATION_JSON)
	public ArrayList<SatelliteOrbitResultViewModel> getUpdatedSatelliteTrack(@HeaderParam("x-session-token") String sSessionId, @PathParam("satellitesname") String sSatName) {

		//Wasdi.DebugLog("OpportunitySearchResource.getUpdatedSatelliteTrack");

		// Check if we have codes
		if (Utils.isNullOrEmpty(sSatName)) return null;

		// Return array
		ArrayList<SatelliteOrbitResultViewModel> aoRet = new ArrayList<SatelliteOrbitResultViewModel>();

		// Clear the string
		if (sSatName.endsWith("-")) {
			sSatName = sSatName.substring(0, sSatName.length() - 1);
		}

		// Split the codes
		String[] asSatellites = sSatName.split("-");

		// Get "now" in the right format
		Time tconv = new Time();
		double k = 180.0 / Math.PI;
		tconv.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

		//For all the satellites
		for (int iSat = 0; iSat < asSatellites.length; iSat++) {

			String sSat = asSatellites[iSat];

			// Create the View Mode
			SatelliteOrbitResultViewModel oPositionViewModel = new SatelliteOrbitResultViewModel();
			
			String oSatelliteResource = InstanceFinder.s_sOrbitSatsMap.get(sSat);
			
			try {

				//Create the Satellite
				Satellite oSatellite = SatFactory.buildSat(oSatelliteResource);				
				oSatellite.getSatController().moveToNow();
				
				// Set Data to the view model
				oPositionViewModel.satelliteName = oSatellite.getDescription();
				oPositionViewModel.code = sSat;				
				oPositionViewModel.currentPosition = (oSatellite.getOrbitCore().getLatitude() * k) + ";" + (oSatellite.getOrbitCore().getLongitude() * k) + ";" + oSatellite.getOrbitCore().getAltitude();
				
				oSatellite.getOrbitCore().setShowGroundTrack(true);

			} 
			catch (Exception e) {
				Wasdi.DebugLog("OpportunitySearchResource.getUpdatedSatelliteTrack: Exception!!" + e.toString());
				e.printStackTrace();
				continue;
			}

			aoRet.add(oPositionViewModel);
		}

		return aoRet;
	}
	
	
	@GET
	@Path("/getsatellitesresource")
	@Produces({ "application/xml", "application/json", "text/html"}) 
	//@Consumes(MediaType.APP)
	@Consumes(MediaType.APPLICATION_JSON)
	public ArrayList <SatelliteResourceViewModel> getSatellitesResources (@HeaderParam("x-session-token") String sSessionId)
	{
//		if(! m_oCredentialPolicy.validSessionId(sSessionId)) {
//			//todo retur error
//			//Satellite oSatellite = new
//		}
		String[] asSatellites = null;
		
//		String satres = InstanceFinder.s_sOrbitSatsMap.get("COSMOSKY1");
		String sSatellites = m_oServletConfig.getInitParameter("LIST_OF_SATELLITES");
		if (sSatellites != null && sSatellites.length() > 0) 
		{
			asSatellites = sSatellites.split(",|;");
		}
		
		ArrayList <SatelliteResourceViewModel> aaoReturnValue = new ArrayList <SatelliteResourceViewModel>();
		for(Integer iIndexSarellite = 0; iIndexSarellite < asSatellites.length ; iIndexSarellite ++)
		{
			try {
				String satres = InstanceFinder.s_sOrbitSatsMap.get(asSatellites[iIndexSarellite]);
				Satellite oSatellite = SatFactory.buildSat(satres);
				ArrayList<SatSensor> aoSatelliteSensors = oSatellite.getSensors();
				
				SatelliteResourceViewModel oSatelliteResource = new SatelliteResourceViewModel();
				oSatelliteResource.setSatelliteName(asSatellites[iIndexSarellite]);
				oSatelliteResource.setSatelliteSensors(aoSatelliteSensors);
				aaoReturnValue.add(oSatelliteResource);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}			
		}
		
		return aaoReturnValue;

	}

}
