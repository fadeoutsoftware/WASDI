STAC and OGC API Features
=========================

WASDI supports the `SpatioTemporal Asset Catalog (STAC) standard <https://stacspec.org/>`_ and the `OGC API - Features standard <https://ogcapi.ogc.org/features/>`_. The ``StacResource`` in the ``wasdiwebserver`` project exposes WASDI workspaces as STAC collections and their files as STAC items.

The STAC service `landing page <https://www.wasdi.net/wasdiwebserver/rest/stac/>`_ is available online. A dedicated `OpenAPI description <https://www.wasdi.net/wasdiwebserver/rest/stac/openapi.json>`_ is provided to fulfil the requirement for concatenated links.

Exposed Endpoints
-----------------

* ``GET /stac``: STAC catalog landing page.
* ``GET /stac/conformance``: implemented STAC and OGC API Features conformance classes.
* ``GET /stac/collections``: list accessible WASDI workspaces as STAC collections.
* ``GET /stac/collections/{collectionId}``: retrieve a workspace as a STAC collection.
* ``GET /stac/collections/{collectionId}/items``: retrieve workspace files as a GeoJSON FeatureCollection of STAC items.
* ``GET /stac/collections/{collectionId}/items/{fileId}``: retrieve one workspace file as a STAC item.