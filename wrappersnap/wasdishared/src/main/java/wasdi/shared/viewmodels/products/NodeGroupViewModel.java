package wasdi.shared.viewmodels.products;

import java.util.List;

/**
 * Represents a node group of a product. 
 * In the client editor product tree, node groups are groups of bands.
 * 
 * Created by s.adamo on 23/05/2016.
 */
public class NodeGroupViewModel {


    private String nodeName;

    public NodeGroupViewModel() {

    }
    public NodeGroupViewModel(String sNodeName)
    {
        this.setNodeName(sNodeName);
    }

    private List<BandViewModel> bands;

    public List<BandViewModel> getBands() {
        return bands;
    }

    public void setBands(List<BandViewModel> bands) {
        this.bands = bands;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
