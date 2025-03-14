package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.nfs.orbits.CoverageTool.Polygon;
import org.nfs.orbits.CoverageTool.apoint;
import org.nfs.orbits.sat.CoverageSwathResult;
import org.nfs.orbits.sat.SatFactory;
import org.nfs.orbits.sat.SatSensor;
import org.nfs.orbits.sat.Satellite;
import org.nfs.orbits.sat.SensorMode;
import org.nfs.orbits.sat.SwathArea;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.ColorMode;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.StyleState;
import it.fadeout.Wasdi;
import it.fadeout.business.InstanceFinder;
import satLib.astro.time.Time;
import wasdi.shared.business.users.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.plan.CoverageSwathResultViewModel;
import wasdi.shared.viewmodels.plan.OpportunitiesSearchViewModel;
import wasdi.shared.viewmodels.plan.SatelliteOrbitResultViewModel;
import wasdi.shared.viewmodels.plan.SatelliteResourceViewModel;
import wasdi.shared.viewmodels.plan.SensorModeViewModel;
import wasdi.shared.viewmodels.plan.SensorViewModel;

/**
 * Opportunity Search Resource.
 * Hosts API for:
 * 	.search new acquisition possibilities
 * 	.Download the relative kml file
 *  .Get the actual position of satellites
 * 
 * @author p.campanella
 *
 */
@Path("/searchorbit")
public class OpportunitySearchResource {
	
	
	/**
	 * Search new acquisition possibilities
	 * @param sSessionId User session 
	 * @param OpportunitiesSearch Input filters view model
	 * @return List of Coverage Swath Result View Models, each representing a possible acquisition
	 */
	@POST
	@Path("/search")
	@Produces({ "application/xml", "application/json", "text/html" })
	@Consumes(MediaType.APPLICATION_JSON)
	public ArrayList<CoverageSwathResultViewModel> search(@HeaderParam("x-session-token") String sSessionId,
			OpportunitiesSearchViewModel OpportunitiesSearch) {
		WasdiLog.debugLog("OpportunitySearchResource.Search");

		ArrayList<CoverageSwathResultViewModel> aoCoverageSwathResultViewModels = new ArrayList<CoverageSwathResultViewModel>();

		try {

			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				WasdiLog.warnLog("OpportunitySearchResource.search: invalid session");
				return aoCoverageSwathResultViewModels;
			}
			
			// set nfs properties download
			String userHome = System.getProperty("user.home");
			String Nfs = System.getProperty("nfs.data.download");
			if (Nfs == null)
				System.setProperty("nfs.data.download", userHome + "/nfs/download");

			WasdiLog.debugLog("nfs dir " + System.getProperty("nfs.data.download"));

			int iIdCoverageCounter = 1;

			ArrayList<CoverageSwathResult> aoCoverageSwathResult = new ArrayList<>();
			aoCoverageSwathResult = InstanceFinder.findSwatsByFilters(OpportunitiesSearch);

			if (aoCoverageSwathResult == null)
				return null;

			// For each Swath Result
			for (CoverageSwathResult oSwatResul : aoCoverageSwathResult) {
				// Get View Model and Childs
				ArrayList<CoverageSwathResultViewModel> aoModels = getSwatViewModelFromResult(oSwatResul);
				for (CoverageSwathResultViewModel oCoverageSwathResultViewModel : aoModels) {
					oCoverageSwathResultViewModel.IdCoverageSwathResultViewModel = iIdCoverageCounter;
					iIdCoverageCounter++;
					aoCoverageSwathResultViewModels.add(oCoverageSwathResultViewModel);
				}

				CoverageSwathResultViewModel oSwathResultViewModel = getCoverageSwathResultViewModelFromCoverageSwathResult(oSwatResul);
				oSwathResultViewModel.FrameFootPrint = "";
				oSwathResultViewModel.IdCoverageSwathResultViewModel = iIdCoverageCounter;
				iIdCoverageCounter++;

				aoCoverageSwathResultViewModels.add(oSwathResultViewModel);
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("OpportunitySearchResource.search: Error searching opportunity " + oEx);
		}

		return aoCoverageSwathResultViewModels;
	}

	/**
	 * Converts the Orbit Lib Ouptut in a WASDI View Model
	 * 
	 * @param oSwath CoverageSwathResult of the Orbit Lib
	 * @return CoverageSwathResultViewModel representig the oSwath entity
	 */
	private CoverageSwathResultViewModel getCoverageSwathResultViewModelFromCoverageSwathResult(
			CoverageSwathResult oSwath) {
		WasdiLog.debugLog("OpportunitySearchResource.getCoverageSwathResultViewModelFromCoverageSwathResult");

		CoverageSwathResultViewModel oVM = new CoverageSwathResultViewModel();
		try {
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
					oVM.AcquisitionStartTime = new Date(oCalendar.getTimeInMillis());
				}
				if (oSwath.getTimeEnd() != null) {
					GregorianCalendar oCalendar = oSwath.getTimeEnd().getCurrentGregorianCalendar();
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
			}

			// ADD CHILDS
			ArrayList<SwathArea> aoChilds = oSwath.getChilds();
			ArrayList<CoverageSwathResultViewModel> aoChildsViewModel = new ArrayList<CoverageSwathResultViewModel>();

			for (SwathArea oSwathArea : aoChilds) {
				CoverageSwathResultViewModel oChild;
				oChild = getCoverageSwathResultViewModelFromCoverageSwathResult(oSwathArea);
				aoChildsViewModel.add(oChild);

			}
			oVM.aoChilds = aoChildsViewModel;
		} catch (Exception oE) {
			WasdiLog.errorLog("OpportunitySearchResource.getCoverageSwathResultViewModelFromCoverageSwathResult: " + oE);
		}
		return oVM;
	}
	
	/**
	 * Convert the child swath area in a view model
	 * @param oSwath Swath Area to convert to CoverageSwathResultViewModel View Model
	 * @return CoverageSwathResultViewModel View Model
	 */
	private CoverageSwathResultViewModel getCoverageSwathResultViewModelFromCoverageSwathResult(SwathArea oSwath) {
		WasdiLog.debugLog("OpportunitySearchResource.getCoverageSwathResultViewModelFromCoverageSwathResult");
		CoverageSwathResultViewModel oVM = new CoverageSwathResultViewModel();
		try {
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
					oVM.AcquisitionStartTime = new Date(oCalendar.getTimeInMillis());
				}
				if (oSwath.getTimeEnd() != null) {
					GregorianCalendar oCalendar = oSwath.getTimeEnd().getCurrentGregorianCalendar();
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
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("OpportunitySearchResource.getCoverageSwathResultViewModelFromCoverageSwathResult: " + oE);
		}
		return oVM;
	}
	
	/**
	 * Converts the Coverage Swat Result of Orbit (NFS Plan Engine) to a WASDI Swat View Mode
	 * @param oSwath NFS result
	 * @return WASDI View Model
	 */
	private ArrayList<CoverageSwathResultViewModel> getSwatViewModelFromResult(CoverageSwathResult oSwath) {
		WasdiLog.debugLog("OpportunitySearchResource.getSwatViewModelFromResult");
		ArrayList<CoverageSwathResultViewModel> aoResults = new ArrayList<CoverageSwathResultViewModel>();
		try {
			if (oSwath == null)
				return aoResults;

			CoverageSwathResultViewModel oVM = getCoverageSwathResultViewModelFromCoverageSwathResult(oSwath);

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
		} catch (Exception oE) {
			WasdiLog.errorLog("OpportunitySearchResource.getSwatViewModelFromResult: " + oE);
		}
		return aoResults;
	}
	
	/**
	 * Get the track (points in time) of a sat
	 * @param sSessionId User Id
	 * @param sSatname Satellite Code
	 * @return Sat Orbit Result View Model
	 */
	@GET
	@Path("/track/{satellitename}")
	@Produces({ "application/xml", "application/json", "text/html" })
	@Consumes(MediaType.APPLICATION_JSON)
	public SatelliteOrbitResultViewModel getSatelliteTrack(@HeaderParam("x-session-token") String sSessionId, @PathParam("satellitename") String sSatname) {
		
		SatelliteOrbitResultViewModel oReturnViewModel = new SatelliteOrbitResultViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);
		if(null == oUser) {
			WasdiLog.warnLog("OpportunitySearchResource.getSatelliteTrack: invalid session");
			return oReturnViewModel;
		}

		
		try {
			// set nfs properties download
			String sUserHome = System.getProperty("user.home");
			String sNfs = System.getProperty("nfs.data.download");
			if (sNfs == null) {
				System.setProperty("nfs.data.download", sUserHome + "/nfs/download");
				WasdiLog.debugLog("nfs dir " + System.getProperty("nfs.data.download"));
			}

			String sSatres = InstanceFinder.getOrbitSatsMap().get(sSatname);

			try {

				Time oTimeConv = new Time();
				oTimeConv.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				Satellite oSat = SatFactory.buildSat(sSatres);

				if (oSat == null) return oReturnViewModel;

				oReturnViewModel.code = sSatname;

				oReturnViewModel.satelliteName = oSat.getDescription();
				oSat.getSatController().moveToNow();
				oReturnViewModel.currentTime = oTimeConv.convertJD2String(oSat.getOrbitCore().getCurrentJulDate());
				oReturnViewModel.setCurrentPosition(oSat.getOrbitCore().getLLA());

				oSat.getOrbitCore().setShowGroundTrack(true);

				// lag
				double[] tm = oSat.getOrbitCore().getTimeLag();
				for (int i = oSat.getOrbitCore().getNumGroundTrackLagPts() - 1; i >= 0; i--)
					oReturnViewModel.addPosition(oSat.getOrbitCore().getGroundTrackLlaLagPt(i), oTimeConv.convertJD2String(tm[i]));

				// lead
				tm = oSat.getOrbitCore().getTimeLead();
				for (int i = 0; i < oSat.getOrbitCore().getNumGroundTrackLeadPts(); i++)
					oReturnViewModel.addPosition(oSat.getOrbitCore().getGroundTrackLlaLeadPt(i), oTimeConv.convertJD2String(tm[i]));
			} catch (Exception oE) {
				WasdiLog.errorLog("OpportunitySearchResource.getSatelliteTrack( " + sSatname + " ): " + oE);
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("OpportunitySearchResource.getSatelliteTrack( " + sSatname + " ): " + oE);
		}
		return oReturnViewModel;
	}
	
	/**
	 * Returns a KML with the acquisition opportunity
	 * @param sSessionId User Session
	 * @param sText
	 * @param sFootPrint
	 * @return
	 */
	@GET
	@Path("/getkmlsearchresults")
	@Produces({ "application/xml" })
	@Consumes(MediaType.APPLICATION_XML)
	public Kml getKmlSearchResults(@HeaderParam("x-session-token") String sSessionId, @QueryParam("text") String sText,
			@QueryParam("footPrint") String sFootPrint) {
		WasdiLog.debugLog("OpportunitySearchResource.getKmlSearchResults( Text: " + sText + ", Footprint: " + sFootPrint + " )");
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("OpportunitySearchResource.getKmlSearchResults: invalid session");
			return null;
		}
		if (sFootPrint.isEmpty() || sText.isEmpty()) {
			return null;
		}
		Kml kml = null;
		try {
			String[] asPoints = Utils.convertPolygonToArray(sFootPrint);
			kml = KmlFactory.createKml();

			// get coordinates
			Boundary oOuterBoundaryIs = new Boundary();
			LinearRing oLinearRing = new LinearRing();
			List<Coordinate> aoCoordinates = new ArrayList<Coordinate>();
			for (String string : asPoints) {
				string = string.replaceAll(" ", ",");
				Coordinate oCoordinate = new Coordinate(string);

				aoCoordinates.add(oCoordinate);

			}

			oLinearRing.setCoordinates(aoCoordinates);

			oOuterBoundaryIs.setLinearRing(oLinearRing);

			// set placemark
			Placemark oPlacemark = kml.createAndSetPlacemark().withName(sText).withVisibility(true);
			// styleLine
			oPlacemark.createAndAddStyleMap().createAndAddPair().withKey(StyleState.NORMAL).createAndSetStyle()
			.createAndSetLineStyle().withColor("FF0000FF").withColorMode(ColorMode.NORMAL).withWidth(1);

			// set polystyle
			oPlacemark.createAndAddStyleMap().createAndAddPair().createAndSetStyle().createAndSetPolyStyle()
			.withColor("FF0000FF").withColorMode(ColorMode.NORMAL).withFill(true).withOutline(true);

			// set polygon
			oPlacemark.createAndSetPolygon().withAltitudeMode(AltitudeMode.CLAMP_TO_GROUND).withExtrude(false)
			.withOuterBoundaryIs(oOuterBoundaryIs);

			kml.setFeature(oPlacemark);
		} catch (Exception oE) {
			WasdiLog.errorLog("OpportunitySearchResource.getKmlSearchResults( Text: " + sText + ", Footprint: " + sFootPrint + " ): " + oE);
		}

		return kml;
	}
	
	/**
	 * Updates the sat track for all the satellites in a single call
	 * @param sSessionId User Session
	 * @param sSatName Satellite names separated by -
	 * @return List of Sat Orbit Result View Models, one for each valid satellite
	 */
	@GET
	@Path("/updatetrack/{satellitesname}")
	@Produces({ "application/xml", "application/json", "text/html" })
	@Consumes(MediaType.APPLICATION_JSON)
	public ArrayList<SatelliteOrbitResultViewModel> getUpdatedSatelliteTrack(
			@HeaderParam("x-session-token") String sSessionId, @PathParam("satellitesname") String sSatName) {


		User oUser = Wasdi.getUserFromSession(sSessionId);
		if(null==oUser) {
			WasdiLog.warnLog("OpportunitySearchResource.getUpdatedSatelliteTrack: invalid session");
			return null;
		}

		// Check if we have codes
		if (Utils.isNullOrEmpty(sSatName)) {
			WasdiLog.debugLog("OpportunitySearchResource.getUpdatedSatelliteTrack: invalid sat name");
			return null;
		}
			

		// Return array
		ArrayList<SatelliteOrbitResultViewModel> aoRet = new ArrayList<SatelliteOrbitResultViewModel>();

		try {

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

			// For all the satellites
			for (int iSat = 0; iSat < asSatellites.length; iSat++) {

				String sSat = asSatellites[iSat];

				// Create the View Mode
				SatelliteOrbitResultViewModel oPositionViewModel = new SatelliteOrbitResultViewModel();

				String oSatelliteResource = InstanceFinder.getOrbitSatsMap().get(sSat);

				try {

					// Create the Satellite
					Satellite oSatellite = SatFactory.buildSat(oSatelliteResource);
					oSatellite.getSatController().moveToNow();

					// Set Data to the view model
					oPositionViewModel.satelliteName = oSatellite.getDescription();
					oPositionViewModel.code = sSat;
					oPositionViewModel.currentPosition = (oSatellite.getOrbitCore().getLatitude() * k) + ";"
							+ (oSatellite.getOrbitCore().getLongitude() * k) + ";"
							+ oSatellite.getOrbitCore().getAltitude();

					oSatellite.getOrbitCore().setShowGroundTrack(true);

				} catch (Exception e) {
					WasdiLog.errorLog("OpportunitySearchResource.getUpdatedSatelliteTrack: " + e);
					continue;
				}

				aoRet.add(oPositionViewModel);
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("OpportunitySearchResource.getUpdatedSatelliteTrack error: " + oE);
		}

		return aoRet;
	}
	
	/**
	 * Get a list of supported satellites for planning
	 * @param sSessionId User Session
	 * @return List of supported Satellites
	 */
	@GET
	@Path("/getsatellitesresource")
	@Produces({ "application/xml", "application/json", "text/html" })
	// @Consumes(MediaType.APP)
	@Consumes(MediaType.APPLICATION_JSON)
	public ArrayList<SatelliteResourceViewModel> getSatellitesResources(@HeaderParam("x-session-token") String sSessionId) {
		
		WasdiLog.debugLog("OpportunitySearchResource.getSatellitesResources");

		ArrayList<SatelliteResourceViewModel> aaoReturnValue = new ArrayList<SatelliteResourceViewModel>();
		
		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);
			
			if(null==oUser) {
				WasdiLog.warnLog("OpportunitySearchResource.getSatellitesResources: invalid session");
				return aaoReturnValue;
			}

			String[] asSatellites = null;

			String sSatellites = WasdiConfig.Current.plan.listOfSatellites;
			
			if (sSatellites != null && sSatellites.length() > 0) {
				asSatellites = sSatellites.split(",|;");
			}
			
			if (asSatellites == null) {
				return aaoReturnValue;
			}

			for (Integer iIndexSatellite = 0; iIndexSatellite < asSatellites.length; iIndexSatellite++) {
				try {
					String sSatres = InstanceFinder.getOrbitSatsMap().get(asSatellites[iIndexSatellite]);
					Satellite oSatellite = SatFactory.buildSat(sSatres);
					
					if (oSatellite == null) {
						WasdiLog.warnLog("OpportunitySearchResource: impossible to build satellite: " + sSatres);
						continue;
					}
					
					ArrayList<SatSensor> aoSatelliteSensors = oSatellite.getSensors();
					
					// Convert the Sat Sensor List in the view model
					SatelliteResourceViewModel oSatelliteResource = new SatelliteResourceViewModel();
					oSatelliteResource.setSatelliteName(oSatellite.getName());
					
					ArrayList<SensorViewModel> aoSensorViewModels = new ArrayList<SensorViewModel>();
					
					for (SatSensor oSatSensor : aoSatelliteSensors) {
						SensorViewModel oSensorViewModel = new SensorViewModel();
						oSensorViewModel.setDescription(oSatSensor.getDescription());
						oSensorViewModel.setEnable(oSatSensor.isEnabled());
						
						for (SensorMode oMode : oSatSensor.getSensorModes()) {
							SensorModeViewModel oSensorModeViewModel = new SensorModeViewModel();
							
							oSensorModeViewModel.setEnable(oMode.isEnabled());
							oSensorModeViewModel.setName(oMode.getName());
							
							oSensorViewModel.getSensorModes().add(oSensorModeViewModel);
							
						}
						
						aoSensorViewModels.add(oSensorViewModel);
					}
					
					oSatelliteResource.setSatelliteSensors(aoSensorViewModels);
					aaoReturnValue.add(oSatelliteResource);
				} catch (Exception oE) {
					WasdiLog.errorLog("OpportunitySearchResource.getSatellitesResources Exception: ", oE);
					return aaoReturnValue;
				}
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("OpportunitySearchResource.getSatellitesResources: ", oE);
		}
		return aaoReturnValue;

	}

}
