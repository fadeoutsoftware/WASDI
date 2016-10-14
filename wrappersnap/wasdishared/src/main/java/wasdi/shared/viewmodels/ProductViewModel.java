package wasdi.shared.viewmodels;

/**
 * Created by s.adamo on 18/05/2016.
 */
public class ProductViewModel {

    private MetadataViewModel metadata;

    private NodeGroupViewModel bandsGroups;


    public MetadataViewModel getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataViewModel metadata) {
        this.metadata = metadata;
    }


    public NodeGroupViewModel getBandsGroups() {
        return bandsGroups;
    }

    public void setBandsGroups(NodeGroupViewModel bandsGroups) {
        this.bandsGroups = bandsGroups;
    }
}
