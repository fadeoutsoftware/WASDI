# WASDI

**Web Advanced Space Developer Interface**

[![Documentation Status](https://readthedocs.org/projects/wasdi/badge/?version=latest)](https://wasdi.readthedocs.io/en/latest/?badge=latest)

> Full documentation: https://wasdi.readthedocs.io/en/latest/

---

## What is WASDI?

WASDI is an **Earth Observation (EO) Platform** designed to let scientists concentrate on science — not on IT infrastructure. It provides a cloud environment where EO processing algorithms can be developed locally and then deployed as scalable cloud services with minimal effort.

- Getting started tutorial: [WASDI Tutorial](https://wasdi.readthedocs.io/en/latest/GettingStarted/WasdiTutorial.html)
- User manual: [Signing Up and Signing In](https://wasdi.readthedocs.io/en/latest/UserManual/SigningUpAndSigningIn.html)

---

## WASDI Libraries

WASDI provides client libraries for the most common scientific and development languages:

| Language | Library |
|----------|---------|
| Python   | `wasdi` |
| JavaScript | `wasdi-js` |
| TypeScript | `wasdi-ts` |
| Java | `wasdi-java` |
| C# | `wasdi-csharp` |
| Octave | `wasdi-octave` |

### The key concept: `getPath()`

The only adaptation needed to make existing code run on WASDI is to **never use hard-coded file paths**. Instead, always resolve paths through the library:

```python
# Python example
import wasdi
file_path = wasdi.getPath("myfile.tif")
```

This single change is all that is required. The code will continue to work on your local machine exactly as before — and once dragged and dropped into WASDI, it automatically becomes a **scalable cloud service**.

### Advanced library features

Beyond path resolution, the WASDI libraries offer a rich set of features:

- **Search EO and non-EO images** — query satellite data catalogues
- **Ingest images into the workspace** — download and stage data for processing
- **Run another application** — chain WASDI apps together via `wasdi.executeProcessor()`
- **Run a SNAP workflow** — execute ESA SNAP graph processing tool chains
- **Mosaic / subset images** — spatial operations on raster data

---

## WASDI Applications

Every WASDI application, regardless of the language it is written in:

- **Takes a JSON file as input** — parameters are passed as a structured JSON object
- **Produces output files** in the workspace — typically geospatial files such as GeoTIFF, GeoPackage, or Shapefile
- **Can optionally store a JSON payload** in the database — a structured result object accessible after execution

All apps, in all supported languages, can interoperate with each other using:

```python
wasdi.executeProcessor("another-app-id", oParameters)
```

This makes it straightforward to build complex multi-step pipelines by composing independent applications.
