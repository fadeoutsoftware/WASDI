package it.fadeout.business;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.nfs.orbits.CoverageTool.CoverageRequest;
import org.nfs.orbits.CoverageTool.InterestArea;
import org.nfs.orbits.CoverageTool.Polygon;
import org.nfs.orbits.CoverageTool.apoint;
import org.nfs.orbits.sat.CoverageSwathResult;
import org.nfs.orbits.sat.ISatellite;
import org.nfs.orbits.sat.SatSensor;
import org.nfs.orbits.sat.Satellite;
import org.nfs.orbits.sat.SensorMode;
import org.nfs.orbits.sat.SwathArea;
import org.nfs.orbits.sat.test.TestSat;

import satLib.astro.time.Time;

public class InstanceFinder {

	/**
	 * List of Orbit's CosmoSkyMed satellites references
	 */
	public static final String[] s_sOrbitSats = new String[] {
			"/org/nfs/orbits/sat/resource/cosmosky1.xml",
			"/org/nfs/orbits/sat/resource/cosmosky2.xml",
			"/org/nfs/orbits/sat/resource/cosmosky3.xml",
			"/org/nfs/orbits/sat/resource/cosmosky4.xml",
            "/org/nfs/orbits/sat/resource/geoeye.xml",
            "/org/nfs/orbits/sat/resource/ikonos2.xml",
            "/org/nfs/orbits/sat/resource/quickbird2.xml",
            "/org/nfs/orbits/sat/resource/spot4.xml",
            "/org/nfs/orbits/sat/resource/spot5.xml",
            "/org/nfs/orbits/sat/resource/worldview2.xml"
	};

	static ArrayList<ISatellite> m_aoSatellites = null;		
	/**
	 * Constant to convert from wgs84 to radiant.
	 */
	static final double s_dConversionFactor = Math.PI / 180.0;

	/**
	 * Finds eo data instances by mean of Orbit library.
	 * 
	 * @param sArea
	 *            A string defining the area of interest.
	 * @param dtDate
	 *            Instances date.
	 * @param sMissionName
	 *            Name of satellite mission instances should made by. At the
	 *            moment this field is ignored, looking all cosmo skymed satellites.
	 * @return
	 */
	public static ArrayList<CoverageSwathResult> OLDfindSwats(String sArea, Date dtDate, String sMissionName) {
		
		// inizializzo i satelliti
		ArrayList<ISatellite> aoSatellites = new ArrayList<ISatellite>();
		
		

		// use all cosmo skymed satellites
		for (int i = 0; i < s_sOrbitSats.length; i++) {
			InputStream oInputStream = TestSat.class.getResourceAsStream(s_sOrbitSats[i]);
			
			Satellite oSatellite;
			try {
				oSatellite = new Satellite(oInputStream);
			} catch (Throwable oEx) {
				oEx.printStackTrace();
				System.out.println("InstanceFinder::findSwats: unable to instantiate satellite " + s_sOrbitSats[i] + " - " + oEx);
				return null;
			}
			
			// di ogni satellite devo specificare quali sensori attivare e quali
			// angoli considerare
			// (di Default nessun sensore è abilitato)

			// ottengo l'elenco dei sensori disponibili sul satellite
			ArrayList<SatSensor> oSatSensors = oSatellite.getSensors();
			
			for (SatSensor oSensor : oSatSensors) {
				
				System.out.println("SENSORE ORBIT: " + oSensor.getSName());
				// activate all sensors
				oSensor.setEnabled(true);
				// ottengo l'elenco di tutti i fasci (angoli) di
				// acquisizione disponibili per questo sensore
				ArrayList<SensorMode> oSensorModes = oSensor.getSensorModes();
				// per questo sensore attivo tutti i possibili fasci
				for (SensorMode oSensorMode : oSensorModes) {
					System.out.println("\tMODE: " + oSensorMode.getName());
					oSensorMode.setEnabled(true);
				}
			}
			
			// add the current satellite to the find list
			aoSatellites.add(oSatellite);
		}

		// preparo l'area di interesse
		InterestArea oAreaOfInterest = new InterestArea("required area");

		Polygon oPoligon = new Polygon();
		String sCleanedArea = sArea.replaceAll("[POLYGN()]", "");
		String[] asAreaPoints = sCleanedArea.split(",");
		int iPointsCount = asAreaPoints.length;
		apoint[] aoPoints = new apoint[iPointsCount];
		
		// process each polygon point
		if (asAreaPoints != null) {
			for (int iCount = 0; iCount < iPointsCount; iCount++) {
				String[] asPoint = asAreaPoints[iCount].split(" ");
				double dX;
				try {
					dX = Double.valueOf(asPoint[0]);
				} catch (Exception oEx) {
					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto x dell'area ");
					dX = 0;
				}
				double dY;
				try {
					dY = Double.valueOf(asPoint[1]);
				} catch (Exception oEx) {
					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto y dell'area ");
					dY = 0;
				}
				aoPoints[iCount] = new apoint(dX * s_dConversionFactor, dY * s_dConversionFactor, 0);

			}
		}
		
		double k = Math.PI / 180.0d;
		// setto i punti dell'area di interesse
		oPoligon.setVertex(aoPoints);
		oAreaOfInterest.setArea(oPoligon);

		String sStartDate = "" + (dtDate.getYear() + 1900);
		sStartDate += "" + ((dtDate.getMonth() + 1) < 10 ? "0" + (dtDate.getMonth() + 1) : (dtDate.getMonth() + 1));
		sStartDate += "" + (dtDate.getDate() < 10 ? "0" + dtDate.getDate() : dtDate.getDate());

		String sEndDate = "" + (dtDate.getYear() + 1900);
		sEndDate += "" + ((dtDate.getMonth() + 1) < 10 ? "0" + (dtDate.getMonth() + 1) : (dtDate.getMonth() + 1));
		sEndDate += "" + ((dtDate.getDate() + 1) < 10 ? "0" + (dtDate.getDate() + 1) : (dtDate.getDate() + 1));
		
		// scelgo il periodo di osservazione
		// DAL 31/07/2013 15:00:00
		Time datetime_start = new Time(sStartDate + "000000");

		// AL 01/08/2013 17:00:00
		Time datetime_end = new Time(sEndDate + "000000");

		// preparo la richiesta di copertura
		CoverageRequest coverageRequest = new CoverageRequest();

		// aggiungo l'area di interesse (posso aggiungerne anche più di una)
		coverageRequest.addInterestArea(oAreaOfInterest);

		// imposto la lista di satelliti da utilizzare per la ricerca
		coverageRequest.setISatellite(aoSatellites);

		// imposto le date di inizio e fine osservazione
		coverageRequest.setFirstDate(datetime_start);
		coverageRequest.setSecondDate(datetime_end);

		// Eseguo la ricerca
		// se a solveRequest passo false ottengo soltanto la potenziale
		// copertura, i
		// fasci non vengono considerati.
		// se passo true per ogni potenziale copertura viene calcolata anche la
		// copertura
		// dei fasci attivati precedentemente
		ArrayList<CoverageSwathResult> oResults = coverageRequest.solveRequest(true);
		
		// ris contiente l'elenco di tutte le potenziali coperture

		return oResults;
	}

	/**
	 * 
	 * @param sArea
	 * @param dtDate
	 * @param sSensorResolution
	 * @param sSensorType
	 * @return
	 */
	public static ArrayList<CoverageSwathResult> findSwats(String sArea, Date dtDate, String sSensorResolution, String sSensorType) {
		
		// inizializzo i satelliti
		if (m_aoSatellites == null) {			
			
			System.out.println("findSwats: CREO I SATELLITI");
			
			m_aoSatellites = new ArrayList<ISatellite>();
			
			// use all cosmo skymed satellites
			for (int i = 0; i < s_sOrbitSats.length; i++) {
				
				System.out.println("findSwats: cerco satellite: " + s_sOrbitSats[i]);
				InputStream oInputStream = TestSat.class.getResourceAsStream(s_sOrbitSats[i]);
				
				Satellite oSatellite;
				try {
					oSatellite = new Satellite(oInputStream);
					System.out.println("costruito");
				} catch (Throwable oEx) {
					oEx.printStackTrace();
					System.out.println("InstanceFinder::findSwats: unable to instantiate satellite " + s_sOrbitSats[i] + " - " + oEx);
					return null;
				}

				// add the current satellite to the find list
				m_aoSatellites.add(oSatellite);
			}
		
		}
		
		if (m_aoSatellites != null) {
			System.out.println("findSwats: Satelliti Disponibili " + m_aoSatellites.size());
		}
		else {
			System.out.println("findSwats: m_aoSatellites NULL ");
		}
		
		ArrayList<ISatellite> aoSatellites = new ArrayList<ISatellite>();
		
		for (ISatellite oSatellite : m_aoSatellites) {
			
			if (oSatellite.getType().toString().toUpperCase().equals(sSensorType.toUpperCase()) == false) continue;
			
			// di ogni satellite devo specificare quali sensori attivare e quali
			// angoli considerare
			// (di Default nessun sensore è abilitato)

			// ottengo l'elenco dei sensori disponibili sul satellite
			ArrayList<SatSensor> oSatSensors = oSatellite.getSensors();
			
			for (SatSensor oSensor : oSatSensors) {
				
				boolean bEnabled = false;
				if (oSensor.getResolution().toString().toUpperCase().substring(0, 1).equals(sSensorResolution.toUpperCase().substring(0, 1) )==true) {
					bEnabled = true;
				}
				
				//System.out.println("SENSORE ORBIT: " + oSensor.getSName());
				
				// activate all sensors
				oSensor.setEnabled(bEnabled);
				// ottengo l'elenco di tutti i fasci (angoli) di
				// acquisizione disponibili per questo sensore
				ArrayList<SensorMode> oSensorModes = oSensor.getSensorModes();
				// per questo sensore attivo tutti i possibili fasci
				for (SensorMode oSensorMode : oSensorModes) {
					//System.out.println("\tMODE: " + oSensorMode.getName());
					oSensorMode.setEnabled(bEnabled);
				}
			}
			
			aoSatellites.add(oSatellite);	
		}

		// preparo l'area di interesse
		InterestArea oAreaOfInterest = new InterestArea("required area");

		Polygon oPoligon = new Polygon();
		String sCleanedArea = sArea.replaceAll("[POLYGN()]", "");
		String[] asAreaPoints = sCleanedArea.split(",");
		int iPointsCount = asAreaPoints.length;
		apoint[] aoPoints = new apoint[iPointsCount];
		
		// process each polygon point
		if (asAreaPoints != null) {
			for (int iCount = 0; iCount < iPointsCount; iCount++) {
				String[] asPoint = asAreaPoints[iCount].split(" ");
				double dX;
				try {
					dX = Double.valueOf(asPoint[0]);
				} catch (Exception oEx) {
					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto x dell'area ");
					dX = 0;
				}
				double dY;
				try {
					dY = Double.valueOf(asPoint[1]);
				} catch (Exception oEx) {
					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto y dell'area ");
					dY = 0;
				}
				aoPoints[iCount] = new apoint(dX * s_dConversionFactor, dY * s_dConversionFactor, 0);

			}
		}
		
		double k = Math.PI / 180.0d;
		// setto i punti dell'area di interesse
		oPoligon.setVertex(aoPoints);
		oAreaOfInterest.setArea(oPoligon);
		
		SimpleDateFormat oFormat = new SimpleDateFormat("yyyyMMdd");
		String sDate = oFormat.format(dtDate);
		sDate += "062100";
		
		// scelgo il periodo di osservazione
		Time oDateTimeStart = new Time(sDate);
		Time oDateTimeEnd = new Time(sDate);
		
		// Set starting and ending time according the time of the request
		Calendar oCalendar = GregorianCalendar.getInstance();
		
		oCalendar.setTime(dtDate);
		if (oCalendar.get(Calendar.HOUR_OF_DAY)<=12 && oCalendar.get(Calendar.MINUTE)<30) {
			// Ok for tomorrow
			oDateTimeStart.add(Time.HOUR,24);
			oDateTimeEnd.add(Time.HOUR,48);
		}
		else {
			// Impossible, go to the day after
			oDateTimeStart.add(Time.HOUR,48);
			oDateTimeEnd.add(Time.HOUR,72);			
		}
		
		// preparo la richiesta di copertura
		CoverageRequest coverageRequest = new CoverageRequest();

		// aggiungo l'area di interesse (posso aggiungerne anche più di una)
		coverageRequest.addInterestArea(oAreaOfInterest);

		// imposto la lista di satelliti da utilizzare per la ricerca
		coverageRequest.setISatellite(aoSatellites);

		// imposto le date di inizio e fine osservazione
		coverageRequest.setFirstDate(oDateTimeStart);
		coverageRequest.setSecondDate(oDateTimeEnd);

		
		System.out.println("findSwats CHIAMO SOLVE REQUEST");
		// Eseguo la ricerca
		// se a solveRequest passo false ottengo soltanto la potenziale
		// copertura, i fasci non vengono considerati.
		// se passo true per ogni potenziale copertura viene calcolata anche la copertura dei fasci attivati precedentemente
		ArrayList<CoverageSwathResult> oResults = coverageRequest.solveRequest(true);
		
		System.out.println("findSwats TORNO");
		// ris contiente l'elenco di tutte le potenziali coperture

		return oResults;
	}
	
	public static void test() {
		// inizializzo i satelliti
		ArrayList<ISatellite> satelliti = new ArrayList<ISatellite>();

		for (int i = 0; i < s_sOrbitSats.length; i++) {
			InputStream strm = TestSat.class
					.getResourceAsStream(s_sOrbitSats[i]);
			Satellite sat;
			try {
				sat = new Satellite(strm);
			} catch (Throwable oEx) {
				// TODO Auto-generated catch block
				oEx.printStackTrace();
				return;
			}
			// visualizzo le info del satellite
			sat.printInfo();
			// di ogni satellite devo specificare quali sensori attivare e quali
			// angoli considerare
			// (di Default nessun sensore è abilitato)

			// ottengo l'elenco dei sensori disponibili sul satellite
			ArrayList<SatSensor> sns = sat.getSensors();
			for (SatSensor sensor : sns) {
				// attivo solo acquisizioni StripMap - right
				if (sensor.getSName().startsWith("StripMap (HIMAGE) - Right")) {
					sensor.setEnabled(true);

					// ottengo l'elenco di tutti i fasci (angoli) di
					// acquisizione disponibili per questo sensore
					ArrayList<SensorMode> snsmode = sensor.getSensorModes();
					// per questo sensore attivo tutti i possibili fasci
					for (SensorMode itm : snsmode)
						itm.setEnabled(true);
				}
			}
			satelliti.add(sat);
		}

		// preparo l'area di interesse
		InterestArea iarea = new InterestArea("area test");
		Polygon pol = new Polygon();
		double k = Math.PI / 180.0d;
		// setto i punti dell'area di interesse
		pol.setVertex(new apoint[] {
				// i punti devono essere convertiti da wgs84 in radianti
				new apoint(8.63 * k, 44.51 * k, 0),
				new apoint(9.10 * k, 44.46 * k, 0),
				new apoint(9.05 * k, 44.29 * k, 0),
				new apoint(8.66 * k, 44.32 * k, 0), });
		iarea.setArea(pol);

		// scelgo il periodo di osservazione
		// DAL 31/07/2013 15:00:00
		Time datetime_start = new Time("20130731150000");

		// AL 01/08/2013 17:00:00
		Time datetime_end = new Time("20130801170000");

		// preparo la richiesta di copertura
		CoverageRequest coverageRequest = new CoverageRequest();

		// aggiungo l'area di interesse (posso aggiungerne anche più di una)
		coverageRequest.addInterestArea(iarea);

		// imposto la lista di satelliti da utilizzare per la ricerca
		coverageRequest.setISatellite(satelliti);

		// imposto le date di inizio e fine osservazione
		coverageRequest.setFirstDate(datetime_start);
		coverageRequest.setSecondDate(datetime_end);

		// Eseguo la ricerca
		// se a solveRequest passo false ottengo soltanto la potenziale
		// copertura, i
		// fasci non vengono considerati.
		// se passo true per ogni potenziale copertura viene calcolata anche la
		// copertura
		// dei fasci attivati precedentemente
		ArrayList<CoverageSwathResult> ris = coverageRequest.solveRequest(true);
		// ris contiente l'elenco di tutte le potenziali coperture

		// visualizzo i risultati
		for (CoverageSwathResult cov : ris) {
			// visualizzo tutti i dettagli..
			System.out.println("ID swath: " + cov.getSwathName());
			System.out.println("satellite: " + cov.getSat().getName());
			System.out.println("Sensore utilizzato: "
					+ cov.getSensor().getSName());
			// fascio del sensore utilizzato (angoli)
			System.out.println("Sensore Mode: " + cov.getMode());
			// se sono state specificate più aree di interesse è utilse sapere
			// questo swath che area copre
			System.out.println("area di interesse coperta: "
					+ cov.getCoveredArea().getName());
			System.out.println("% copertura " + cov.getCoverage() * 100);
			System.out.println("Inizio acquisizione: "
					+ cov.getTimeStart().getDateTimeStr());
			System.out.println("Fine acquisizione: "
					+ cov.getTimeEnd().getDateTimeStr());
			System.out.println("Durata " + cov.getDuration());
			System.out.println("larghezza copertura "
					+ cov.getswathSize().getWidth());
			System.out.println("lunghezza copertura "
					+ cov.getswathSize().getLength());
			// visualizzo le coordinate della copertura

			apoint[] vrtx = cov.getFootprint().getVertex();
			for (apoint pnt : vrtx)
				// converto i punti da radianti a lon lat (wgs84)
				System.out.println("lon: " + pnt.x / k + " lat: " + pnt.y / k);
			// se a solveRequest ho passato true avro' anche le aree di
			// copertura di
			// ogni singolo fascio
			ArrayList<SwathArea> chld = cov.getChilds();
			for (SwathArea itm : chld) {
				// posso visualizzare le stesse info di sopra poichè
				// SwathArea è la superclasse di CoverageSwathResult

				// Con printDetail visualizzo le stesse info
				itm.printDetail();
			}

		}
	}
	
}
