# WASDI Advanced Python Tutorial

This processor searches for Sentinel-2 images and extract an RGB GeoTIFF from it.

## Parameters

Parameters are in this form:

```json
{
  "BBOX": "45.9,8.5,45.7,8.7",
  "MAXCLOUD": "50",
  "DATE": "2020-10-25",
  "SEARCHDAYS": "20",
  "PROVIDER": "AUTO"
}
```

where:
- BBOX is the bounding box represented as a string with the format: "LATN,LONW,LATS,LONE"
- MAXCLOUD is an integer representing the maximum cloud coverage (percent)
- DATE is a date for the search
- SEARCHDAYS is the maximum number of days to search in the past
So the search will be performed on the BBOX and in the period [DATE - SEARCHDAYS, DATE], and for images with at most MAXCLOUD% cloud coverage
- PROVIDER is the data provider, use "AUTO"