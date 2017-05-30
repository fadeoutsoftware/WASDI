package wasdi.shared.parameters;

import org.esa.s1tbx.io.orbits.sentinel1.SentinelPODOrbitFile;

/**
 * Created by s.adamo on 16/03/2017.
 */
public class ApplyOrbitSetting implements ISetting{

    private String[] sourceBandNames;

    /*
    @Parameter(valueSet = {SentinelPODOrbitFile.PRECISE + " (Auto Download)", SentinelPODOrbitFile.RESTITUTED + " (Auto Download)",
            DorisOrbitFile.DORIS_POR + " (ENVISAT)", DorisOrbitFile.DORIS_VOR + " (ENVISAT)" + " (Auto Download)",
            DelftOrbitFile.DELFT_PRECISE + " (ENVISAT, ERS1&2)" + " (Auto Download)",
            PrareOrbitFile.PRARE_PRECISE + " (ERS1&2)" + " (Auto Download)",
            K5OrbitFile.PRECISE },
            defaultValue = SentinelPODOrbitFile.PRECISE + " (Auto Download)", label = "Orbit State Vectors")
            */
    private String orbitType;

    //@Parameter(label = "Polynomial Degree", defaultValue = "3")
    private int polyDegree;

    //@Parameter(label = "Do not fail if new orbit file is not found", defaultValue = "false")
    private Boolean continueOnFail;

    public ApplyOrbitSetting(){
        setOrbitType(SentinelPODOrbitFile.PRECISE + " (Auto Download)");
        setPolyDegree(3);
        setContinueOnFail(false);
    }

    public String getOrbitType() {
        return orbitType;
    }

    public void setOrbitType(String orbitType) {
        this.orbitType = orbitType;
    }

    public int getPolyDegree() {
        return polyDegree;
    }

    public void setPolyDegree(int polyDegree) {
        this.polyDegree = polyDegree;
    }

    public Boolean getContinueOnFail() {
        return continueOnFail;
    }

    public void setContinueOnFail(Boolean continueOnFail) {
        this.continueOnFail = continueOnFail;
    }

    public String[] getSourceBandNames() {
        return sourceBandNames;
    }

    public void setSourceBandNames(String[] sourceBandNames) {
        this.sourceBandNames = sourceBandNames;
    }
}
