package wasdi.shared.viewmodels;


import java.util.ArrayList;
import java.util.List;

/**
 * Represents the complete request payload for the map printing service.
 * All related data models (Center, WmsLayer, Wkt) are defined as public static
 * nested classes within this file to allow a single public class per file.
 */
public class PrinterViewModel {

    private String baseMap;
    private int zoomLevel;
    private Center center;
    private String format; // "pdf" or "png"
    private List<WmsLayer> wmsLayers;
    private List<Wkt> wkts;

    public PrinterViewModel() {
        // Default constructor for JSON deserialization
        // Initialize lists to prevent NullPointerExceptions when adding elements
        this.wmsLayers = new ArrayList<>();
        this.wkts = new ArrayList<>();
    }

    // Getters and Setters for PrinterViewModel
    public String getBaseMap() {
        return baseMap;
    }

    public void setBaseMap(String baseMap) {
        this.baseMap = baseMap;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public Center getCenter() {
        return center;
    }

    public void setCenter(Center center) {
        this.center = center;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<WmsLayer> getWmsLayers() {
        return wmsLayers;
    }

    public void setWmsLayers(List<WmsLayer> wmsLayers) {
        this.wmsLayers = wmsLayers;
    }

    public List<Wkt> getWkts() {
        return wkts;
    }

    public void setWkts(List<Wkt> wkts) {
        this.wkts = wkts;
    }

    /**
     * Represents the center coordinates of the map.
     * Defined as a public static nested class.
     */
    public static class Center { // Changed to public static
        private double lat;
        private double lng;

        public Center() {
            // Default constructor for JSON deserialization
        }

        // Public getters and setters for lat
        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        // Public getters and setters for lng
        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }
    }

    /**
     * Represents a WMS layer to be included in the print.
     * Defined as a public static nested class.
     */
    public static class WmsLayer { // Changed to public static
        private String name;
        private String layerId;
        private String wmsUrl;

        public WmsLayer() {
            // Default constructor for JSON deserialization
        }

        // Public getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLayerId() {
            return layerId;
        }

        public void setLayerId(String layerId) {
            this.layerId = layerId;
        }

        public String getWmsUrl() {
            return wmsUrl;
        }

        public void setWmsUrl(String wmsUrl) {
            this.wmsUrl = wmsUrl;
        }
    }

    /**
     * Represents a Well-Known Text (WKT) geometry to be included in the print.
     * Defined as a public static nested class.
     */
    public static class Wkt { // Changed to public static
        private String name;
        private String geom;

        public Wkt() {
            // Default constructor for JSON deserialization
        }

        // Public getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGeom() {
            return geom;
        }

        public void setGeom(String geom) {
            this.geom = geom;
        }
    }
}
