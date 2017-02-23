package search;

/**
 * Created by s.adamo on 21/02/2017.
 */
public class SentinelInfo {

    private String downloadLink;

    private String fileName;

    private String sceneCenterLat;

    private String sceneCenterLon;

    private String orbit;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSceneCenterLat() {
        return sceneCenterLat;
    }

    public void setSceneCenterLat(String sceneCenterLat) {
        this.sceneCenterLat = sceneCenterLat;
    }

    public String getSceneCenterLon() {
        return sceneCenterLon;
    }

    public void setSceneCenterLon(String sceneCenterLon) {
        this.sceneCenterLon = sceneCenterLon;
    }

    public String getOrbit() {
        return orbit;
    }

    public void setOrbit(String orbit) {
        this.orbit = orbit;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }
}
