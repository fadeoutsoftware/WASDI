namespace WasdiLib.Models
{
    internal class MosaicSetting
    {
        public double PixelSizeX { get; set; } = -1.0;
        public double PixelSizeY { get; set; } = -1.0;
        public int NoDataValue { get; set; }
        public int InputIgnoreValue { get; set; }
        public string OutputFormat { get; set; } = "GeoTIFF";
        public List<string> Sources { get; set; } = new List<string>();
    }
}
