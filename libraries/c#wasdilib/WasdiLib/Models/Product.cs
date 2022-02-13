namespace WasdiLib.Models
{
    public class Product
    {
        public NodeGroup BandsGroups { get; set; }
        public string Bbox { get; set; }
        public string FileName { get; set; }
        public Metadata Metadata { get; set; }
        public bool MetadataFileCreated { get; set; }
        public string MetadataFileReference { get; set; }
        public string Name { get; set; }
        public string ProductFriendlyName { get; set; }
        public string Style { get; set; }
    }

    public class Metadata
    {
        public string Name { get; set; }
        public List<Metadata> Elements { get; set; }
        public List<Attribute> Attributes { get; set; }
    }

    public class Attribute
    {
        public int DataType { get; set; }
        public long NumElems { get; set; }
        public string Description { get; set; }
        public string Data { get; set; }
    }

    public class NodeGroup
    {
        public List<Band> Bands { get; set; }
        public string NodeName { get; set; }
    }

    public class Band
    {
        public string GeoserverBoundingBox { get; set; }
        public string GeoserverUrl { get; set; }
        public int Height { get; set; } = 0;
        public string LayerId { get; set; }
        public string Name { get; set; }
        public bool Published { get; set; } = false;
        public int Width { get; set; } = 0;
    }

}
