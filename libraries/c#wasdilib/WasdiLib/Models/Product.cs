namespace WasdiLib.Models
{
    public class Product
    {
        public BandsGroups BandsGroups { get; set; }
        public string Bbox { get; set; }
        public string FileName { get; set; }
        public object Metadata { get; set; }
        public bool MetadataFileCreated { get; set; }
        public object MetadataFileReference { get; set; }
        public string Name { get; set; }
        public object ProductFriendlyName { get; set; }
        public object Style { get; set; }
    }

    public class BandsGroups
    {
        public List<Band> Bands { get; set; }
        public string NodeName { get; set; }
    }

    public class Band
    {
        public object GeoserverBoundingBox { get; set; }
        public string GeoserverUrl { get; set; }
        public int Height { get; set; }
        public object LayerId { get; set; }
        public string Name { get; set; }
        public bool Published { get; set; }
        public int Width { get; set; }
    }

}
