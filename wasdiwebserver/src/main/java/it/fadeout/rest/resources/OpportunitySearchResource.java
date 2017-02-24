package it.fadeout.rest.resources;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.nfs.orbits.CoverageTool.Polygon;
import org.nfs.orbits.CoverageTool.apoint;
import org.nfs.orbits.sat.CoverageSwathResult;
import org.nfs.orbits.sat.SwathArea;

import it.fadeout.Wasdi;
import it.fadeout.business.InstanceFinder;
import it.fadeout.viewmodels.CoverageSwathResultViewModel;
import it.fadeout.viewmodels.OrbitFilterViewModel;
import it.fadeout.viewmodels.OrbitSearchViewModel;
import wasdi.shared.business.User;
import wasdi.shared.utils.Utils;

@Path("/searchorbit")
public class OpportunitySearchResource {


	@POST
	@Path("/search")
	@Produces({"application/xml", "application/json", "text/html"})
	@Consumes(MediaType.APPLICATION_JSON)
	public ArrayList<CoverageSwathResultViewModel> Search(@HeaderParam("x-session-token") String sSessionId, OrbitSearchViewModel OrbitSearch)
	{

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ArrayList<CoverageSwathResultViewModel> aoCoverageSwathResultViewModels = new ArrayList<CoverageSwathResultViewModel>();

		try
		{
			if (oUser == null) {
				return aoCoverageSwathResultViewModels;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoCoverageSwathResultViewModels;
			}	

			if (OrbitSearch == null) return null;

			//set nfs properties download
			String userHome = System.getProperty( "user.home");
			String Nfs = System.getProperty( "nfs.data.download" );
			if (Nfs == null)
				System.setProperty( "nfs.data.download", userHome + "/nfs/download");

			System.out.println("init wasdi: nfs dir " + System.getProperty( "nfs.data.download" ));

			Date dtDate = new Date(); 
			String sArea = OrbitSearch.getPolygon();
			int iIdCoverageCounter = 1;

			// Foreach filter combination found
			List<OrbitFilterViewModel> oOrbitFilters = OrbitSearch.getOrbitFilters();
			for (OrbitFilterViewModel oOrbitFilter : oOrbitFilters) {

				// Find the opportunities
				ArrayList<CoverageSwathResult> aoCoverageSwathResult = new ArrayList<>();
				try {
					aoCoverageSwathResult = InstanceFinder.findSwatsByFilters(sArea, OrbitSearch.getAcquisitionStartTime(), OrbitSearch.getAcquisitionEndTime(), OrbitSearch.getSatelliteNames(), oOrbitFilter.getSensorResolution(),oOrbitFilter.getSensorType());
				} catch (ParseException e) {
					// TODO Auto-generated catch block
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
						iIdCoverageCounter ++;
						aoCoverageSwathResultViewModels.add(oCoverageSwathResultViewModel);
					}
				}

			}
			
			return aoCoverageSwathResultViewModels;
		}
		catch(Exception oEx){
			System.out.println("OpportunitySearchResource.Search: Error searching opportunity " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return aoCoverageSwathResultViewModels;
	}


	private ArrayList<CoverageSwathResultViewModel>  getSwatViewModelFromResult(CoverageSwathResult oSwath) {
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

			if (oSwath.getCoveredArea() != null) oVM.CoveredAreaName = oSwath.getCoveredArea().getName();

			if (oSwath.getSensor() != null) {
				if (oSwath.getSensor().getLooking()!=null) oVM.SensorLookDirection = oSwath.getSensor().getLooking().toString();
			}

			if (oSwath.getTimeStart() != null) {
				GregorianCalendar oCalendar =  oSwath.getTimeStart().getCurrentGregorianCalendar();							
				//oVM.AcquisitionStartTime = oCalendar.getTime();
				oVM.AcquisitionStartTime = new Date(oCalendar.getTimeInMillis());
			}
			if (oSwath.getTimeEnd() != null) {
				GregorianCalendar oCalendar =  oSwath.getTimeEnd().getCurrentGregorianCalendar();
				//oVM.AcquisitionEndTime = oCalendar.getTime();
				oVM.AcquisitionEndTime = new Date(oCalendar.getTimeInMillis());
			}

			oVM.AcquisitionDuration = oSwath.getDuration();

			if (oSwath.getFootprint() != null) {
				Polygon oPolygon = oSwath.getFootprint();
				apoint [] aoPoints = oPolygon.getVertex();

				if (aoPoints != null) {

					oVM.SwathFootPrint = "POLYGON((";

					for (int iPoints = 0; iPoints<aoPoints.length; iPoints++) {
						apoint oPoint = aoPoints[iPoints];
						oVM.SwathFootPrint+= "" + (oPoint.x*180.0/Math.PI);
						oVM.SwathFootPrint+= " ";
						oVM.SwathFootPrint+= "" + (oPoint.y*180.0/Math.PI);
						oVM.SwathFootPrint+=",";
					}

					oVM.SwathFootPrint = oVM.SwathFootPrint.substring(0,oVM.SwathFootPrint.length()-2);

					oVM.SwathFootPrint += "))";
				}
			}


			List<SwathArea> aoAreas = oSwath.getChilds();

			for (SwathArea oArea : aoAreas) {

				CoverageSwathResultViewModel oSwathResult = new CoverageSwathResultViewModel(oVM);

				if (oArea.getMode() != null) {
					oSwathResult.SensorMode = oArea.getMode().getName();
					if (oArea.getMode().getViewAngle() != null) oSwathResult.Angle = oArea.getMode().getViewAngle().toString();
				}

				if (oArea.getswathSize()!= null) {
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
					apoint [] aoPoints = oPolygon.getVertex();

					if (aoPoints != null) {

						oSwathResult.FrameFootPrint = "POLYGON((";

						for (int iPoints = 0; iPoints<aoPoints.length; iPoints++) {
							apoint oPoint = aoPoints[iPoints];
							oSwathResult.FrameFootPrint+= "" + (oPoint.x*180.0/Math.PI);
							oSwathResult.FrameFootPrint+= " ";
							oSwathResult.FrameFootPrint+= "" + (oPoint.y*180.0/Math.PI);
							oSwathResult.FrameFootPrint+=",";
						}

						oSwathResult.FrameFootPrint = oSwathResult.FrameFootPrint.substring(0,oSwathResult.FrameFootPrint.length()-2);

						oSwathResult.FrameFootPrint += "))";
					}
				}


				aoResults.add(oSwathResult);
			}

		}
		return aoResults;
	}
}
