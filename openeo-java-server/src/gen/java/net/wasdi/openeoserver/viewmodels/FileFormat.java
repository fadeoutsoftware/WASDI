/*
 * openEO API
 * The openEO API specification for interoperable cloud-based processing of large Earth observation datasets.  # API Principles  ## Language  In the specification the key words “MUST”, “MUST NOT”, “REQUIRED”, “SHALL”, “SHALL NOT”, “SHOULD”, “SHOULD NOT”, “RECOMMENDED”, “MAY”, and “OPTIONAL” in this document are to be interpreted as described in [RFC 2119](https://www.rfc-editor.org/rfc/rfc2119.html) and [RFC 8174](https://www.rfc-editor.org/rfc/rfc8174.html).  ## Casing  Unless otherwise stated the API works **case sensitive**.  All names SHOULD be written in snake case, i.e. words are separated with one underscore character (`_`) and no spaces, with all letters lower-cased. Example: `hello_world`. This applies particularly to endpoints and JSON property names. HTTP header fields follow their respective casing conventions, e.g. `Content-Type` or `OpenEO-Costs`, despite being case-insensitive according to [RFC 7230](https://www.rfc-editor.org/rfc/rfc7230.html#section-3.2).  ## HTTP / REST  This uses [HTTP REST](https://en.wikipedia.org/wiki/Representational_state_transfer) [Level 2](https://martinfowler.com/articles/richardsonMaturityModel.html#level2) for communication between client and back-end server.  Public APIs MUST be available via HTTPS only.   Endpoints are made use meaningful HTTP verbs (e.g. GET, POST, PUT, PATCH, DELETE) whenever technically possible. If there is a need to transfer big chunks of data for a GET requests to the back-end, POST requests MAY be used as a replacement as they support to send data via request body. Unless otherwise stated, PATCH requests are only defined to work on direct (first-level) children of the full JSON object. Therefore, changing a property on a deeper level of the full JSON object always requires to send the whole JSON object defined by the first-level property.  Naming of endpoints follow the REST principles. Therefore, endpoints are centered around resources. Resource identifiers MUST be named with a noun in plural form except for single actions that can not be modelled with the regular HTTP verbs. Single actions MUST be single endpoints with a single HTTP verb (POST is RECOMMENDED) and no other endpoints beneath it.  ## JSON  The API uses JSON for request and response bodies whenever feasible. Services use JSON as the default encoding. Other encodings can be requested using [Content Negotiation](https://www.w3.org/Protocols/rfc2616/rfc2616-sec12.html). Clients and servers MUST NOT rely on the order in which properties appears in JSON. Collections usually don't include nested JSON objects if those information can be requested from the individual resources.  ## Web Linking  The API is designed in a way that to most entities (e.g. collections and processes) a set of links can be added. These can be alternate representations, e.g. data discovery via OGC WCS or OGC CSW, references to a license, references to actual raw data for downloading, detailed information about pre-processing and more. Clients should allow users to follow the links.  Whenever links are utilized in the API, the description explains which relation (`rel` property) types are commonly used. A [list of standardized link relations types is provided by IANA](https://www.iana.org/assignments/link-relations/link-relations.xhtml) and the API tries to align whenever feasible.  Some very common relation types - usually not mentioned explicitly in the description of `links` fields - are:  1. `self`: which allows link to the location that the resource can be (permanently) found online.This is particularly useful when the data is data is made available offline, so that the downstream user knows where the data has come from.  2. `alternate`: An alternative representation of the resource, may it be another metadata standard the data is available in or simply a human-readable version in HTML or PDF.  3. `about`: A resource that is related or further explains the resource, e.g. a user guide.  ## Error Handling  The success of requests MUST be indicated using [HTTP status codes](https://www.rfc-editor.org/rfc/rfc7231.html#section-6) according to [RFC 7231](https://www.rfc-editor.org/rfc/rfc7231.html).  If the API responds with a status code between 100 and 399 the back-end indicates that the request has been handled successfully.  In general an error is communicated with a status code between 400 and 599. Client errors are defined as a client passing invalid data to the service and the service *correctly* rejecting that data. Examples include invalid credentials, incorrect parameters, unknown versions, or similar. These are generally \"4xx\" HTTP error codes and are the result of a client passing incorrect or invalid data. Client errors do *not* contribute to overall API availability.   Server errors are defined as the server failing to correctly return in response to a valid client request. These are generally \"5xx\" HTTP error codes. Server errors *do* contribute to the overall API availability. Calls that fail due to rate limiting or quota failures MUST NOT count as server errors.   ### JSON error object  A JSON error object SHOULD be sent with all responses that have a status code between 400 and 599.  ``` json {   \"id\": \"936DA01F-9ABD-4D9D-80C7-02AF85C822A8\",   \"code\": \"SampleError\",   \"message\": \"A sample error message.\",   \"url\": \"https://example.openeo.org/docs/errors/SampleError\" } ```  Sending `code` and `message` is REQUIRED.   * A back-end MAY add a free-form `id` (unique identifier) to the error response to be able to log and track errors with further non-disclosable details. * The `code` is either one of the [standardized textual openEO error codes](errors.json) or a proprietary error code. * The `message` explains the reason the server is rejecting the request. For \"4xx\" error codes the message explains how the client needs to modify the request.    By default the message MUST be sent in English language. Content Negotiation is used to localize the error messages: If an `Accept-Language` header is sent by the client and a translation is available, the message should be translated accordingly and the `Content-Language` header must be present in the response. See \"[How to localize your API](http://apiux.com/2013/04/25/how-to-localize-your-api/)\" for more information. * `url` is an OPTIONAL attribute and contains a link to a resource that is explaining the error and potential solutions in-depth.  ### Standardized status codes  The openEO API usually uses the following HTTP status codes for successful requests:   - **200 OK**:   Indicates a successful request **with** a response body being sent. - **201 Created**   Indicates a successful request that successfully created a new resource. Sends a `Location` header to the newly created resource **without** a response body. - **202 Accepted**   Indicates a successful request that successfully queued the creation of a new resource, but it has not been created yet. The response is sent **without** a response body. - **204 No Content**:   Indicates a successful request **without** a response body being sent.  The openEO API has some commonly used HTTP status codes for failed requests:   - **400 Bad Request**:   The back-end responds with this error code whenever the error has its origin on client side and no other HTTP status code in the 400 range is suitable.  - **401 Unauthorized**:   The client did not provide any authentication details for a resource requiring authentication or the provided authentication details are not correct.  - **403 Forbidden**:   The client did provided correct authentication details, but the privileges/permissions of the provided credentials do not allow to request the resource.  - **404 Not Found**:   The resource specified by the path does not exist, i.e. one of the resources belonging to the specified identifiers are not available at the back-end.   *Note:* Unsupported endpoints MUST use HTTP status code 501.  - **500 Internal Server Error**:   The error has its origin on server side and no other status code in the 500 range is suitable.   If a HTTP status code in the 400 range is returned, the client SHOULD NOT repeat the request without modifications. For HTTP status code in the 500 range, the client MAY repeat the same request later.  All HTTP status codes defined in RFC 7231 in the 400 and 500 ranges can be used as openEO error code in addition to the most used status codes mentioned here. Responding with openEO error codes 400 and 500 SHOULD be avoided in favor of any more specific standardized or proprietary openEO error code.  ## Temporal data  Date, time, intervals and durations are formatted based on ISO 8601 or its profile [RFC 3339](https://www.rfc-editor.org/rfc/rfc3339.html) whenever there is an appropriate encoding available in the standard. All temporal data are specified based on the Gregorian calendar.  # Authentication  The openEO API offers two forms of authentication by default: * OpenID Connect (recommended) at `GET /credentials/oidc` * Basic at `GET /credentials/basic`    After authentication with any of the methods listed above, the tokens obtained during the authentication workflows can be sent to protected endpoints in subsequent requests.  Further authentication methods MAY be added by back-ends.  <SecurityDefinitions />  # Cross-Origin Resource Sharing (CORS)  > Cross-origin resource sharing (CORS) is a mechanism that allows restricted resources [...] on a web page to be requested from another domain outside the domain from which the first resource was served. [...] > CORS defines a way in which a browser and server can interact to determine whether or not it is safe to allow the cross-origin request. It allows for more freedom and functionality than purely same-origin requests, but is more secure than simply allowing all cross-origin requests.  Source: [https://en.wikipedia.org/wiki/Cross-origin_resource_sharing](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing)  openEO-based back-ends are usually hosted on a different domain / host than the client that is requesting data from the back-end. Therefore most requests to the back-end are blocked by all modern browsers. This leads to the problem that the JavaScript library and any browser-based application can't access back-ends. Therefore, all back-end providers SHOULD support CORS to enable browser-based applications to access back-ends. [CORS is a recommendation of the W3C organization](https://www.w3.org/TR/cors/). The following chapters will explain how back-end providers can implement CORS support.  **Tip**: Most servers can send the required headers and the responses to the OPTIONS requests automatically for all endpoints. Otherwise you may also use a proxy server to add the headers and OPTIONS responses.  ## CORS headers  The following headers MUST be included with every response:  | Name                             | Description                                                  | Example | | -------------------------------- | ------------------------------------------------------------ | ------- | | Access-Control-Allow-Origin      | Allowed origin for the request, including protocol, host and port or `*` for all origins. It is RECOMMENDED to return the value `*` to allow requests from browser-based implementations such as the Web Editor. | `*` | | Access-Control-Expose-Headers    | Some endpoints require to send additional HTTP response headers such as `OpenEO-Identifier` and `Location`. To make these headers available to browser-based clients, they MUST be white-listed with this CORS header. The following HTTP headers are white-listed by browsers and MUST NOT be included: `Cache-Control`, `Content-Language`, `Content-Length`, `Content-Type`, `Expires`, `Last-Modified` and `Pragma`. At least the following headers MUST be listed in this version of the openEO API: `Link`, `Location`, `OpenEO-Costs` and `OpenEO-Identifier`. | `Link, Location, OpenEO-Costs, OpenEO-Identifier` |    ### Example request and response  Request:  ```http POST /api/v1/jobs HTTP/1.1 Host: openeo.cloudprovider.com Origin: https://client.org:8080 Authorization: Bearer basic//ZXhhbXBsZTpleGFtcGxl ```  Response:  ```http HTTP/1.1 201 Created Access-Control-Allow-Origin: * Access-Control-Expose-Headers: Location, OpenEO-Identifier, OpenEO-Costs, Link Content-Type: application/json Location: https://openeo.cloudprovider.com/api/v1/jobs/abc123 OpenEO-Identifier: abc123 ```  ## OPTIONS method  All endpoints must respond to the `OPTIONS` HTTP method. This is a response for the preflight requests made by web browsers before sending the actual request (e.g. `POST /jobs`). It needs to respond with a status code of `204` and no response body. **In addition** to the HTTP headers shown in the table above, the following HTTP headers MUST be included with every response to an `OPTIONS` request:  | Name                             | Description                                                  | Example | | -------------------------------- | ------------------------------------------------------------ | ------- | | Access-Control-Allow-Headers     | Comma-separated list of HTTP headers allowed to be sent with the actual (non-preflight) request. MUST contain at least `Authorization` if any kind of authorization is implemented by the back-end. | `Authorization, Content-Type` | | Access-Control-Allow-Methods     | Comma-separated list of HTTP methods allowed to be requested. Back-ends MUST list all implemented HTTP methods for the endpoint. | `OPTIONS, GET, POST, PATCH, PUT, DELETE` | | Content-Type                     | SHOULD return the content type delivered by the request that the permission is requested for. | `application/json` |  ### Example request and response  Request:  ```http OPTIONS /api/v1/jobs HTTP/1.1 Host: openeo.cloudprovider.com Origin: https://client.org:8080 Access-Control-Request-Method: POST  Access-Control-Request-Headers: Authorization, Content-Type ```  Note that the `Access-Control-Request-*` headers are automatically attached to the requests by the browsers.  Response:  ```http HTTP/1.1 204 No Content Access-Control-Allow-Origin: * Access-Control-Allow-Methods: OPTIONS, GET, POST, PATCH, PUT, DELETE Access-Control-Allow-Headers: Authorization, Content-Type Access-Control-Expose-Headers: Location, OpenEO-Identifier, OpenEO-Costs, Link Content-Type: application/json ```  # Processes  A **process** is an operation that performs a specific task on a set of parameters and returns a result. An example is computing a statistical operation, such as mean or median, on selected EO data. A process is similar to a function or method in programming languages. In openEO, processes are used to build a chain of processes ([process graph](#section/Processes/Process-Graphs)), which can be applied to EO data to derive your own findings from the data.  A **pre-defined process** is a process provided by the *back-end*. There is a set of predefined processes by openEO to improve interoperability between back-ends. Back-ends SHOULD follow these specifications whenever possible. Not all processes need to be implemented by all back-ends. See the **[process reference](https://processes.openeo.org)** for pre-defined processes.  A **user-defined process** is a process defined by the *user*. It can directly be part of another process graph or be stored as custom process on a back-end. Internally it is a *process graph* with optional additional metadata.  A **process graph** chains specific process calls from the set of pre-defined and user-defined processes together. A process graph itself can be stored as a (user-defined) process again. Similarly to scripts in the context of programming, process graphs organize and automate the execution of one or more processes that could alternatively be executed individually. In a process graph, processes need to be specific, i.e. concrete values or \"placeholders\" for input parameters need to be specified. These values can be scalars, arrays, objects, references to parameters or previous computations or other process graphs.  ## Defining Processes  Back-ends and users MAY define new proprietary processes for their domain.   **Back-end providers** MUST follow the schema for predefined processes as in [`GET /processes`](#operation/list-processes) to define new processes. This includes:  * Choosing a intuitive process id, consisting of only letters (a-z), numbers and underscores. It MUST be unique across the pre-defined processes. * Defining the parameters and their exact (JSON) schemes. * Specifying the return value of a process also with a (JSON) schema. * Providing examples or compliance tests. * Trying to make the process universally usable so that other back-end providers or openEO can adopt it.  **Users** MUST follow the schema for user-defined processes as in [`GET /process_graphs/{process_graph_id}`](#operation/describe-custom-process) to define new processes. This includes:  * Choosing a intuitive name as process id, consisting of only letters (a-z), numbers and underscores. It MUST be unique across the user-defined processes. * Defining the algorithm as a process graph. * Optionally, specifying the additional metadata for processes.  If new process are potentially useful for other back-ends the openEO consortium is happily accepting [pull requests](https://github.com/Open-EO/openeo-processes/pulls) to include them in the list of pre-defined processes.  ### Schemas  Each process parameter and the return values of a process define a schema that the value MUST comply to. The schemas are based on [JSON Schema draft-07](http://json-schema.org/).  Three custom keywords have been defined: * `subtype` for more fine-grained data-types than JSON Schema supports. * `parameters` to specify the parameters of a process graph (object with subtype `process-graph`). * `returns` to describe the return value of a process graph (object with subtype `process-graph`).  ### Subtypes  JSON Schema allows to specify only a small set of native data types (string, boolean, number, integer, array, object, null). To support more fine grained data types, a custom [JSON Schema keyword](https://json-schema.org/draft-07/json-schema-core.html#rfc.section.6.4) has been defined: `subtype`. It works similarly as the JSON Schema keyword [`format`](https://json-schema.org/draft-07/json-schema-validation.html#rfc.section.7) and standardizes a number of openEO related data types that extend the native data types, for example: `bounding-box` (object with at least `west`, `south`, `east` and `north` properties), `date-time` (string representation of date and time following RFC 3339), `raster-cube` (the type of data cubes), etc. The subtypes should be re-used in process schema definitions whenever suitable.  If a general data type such as `string` or `number` is used in a schema, all subtypes with the same parent data type can be passed, too. Clients should offer make passing subtypes as easy as passing a general data type. For example, a parameter accepting strings must also allow passing a string with subtype `date` and thus clients should encourage this by also providing a date-picker.  A list of predefined subtypes is available as JSON Schema in [openeo-processes](https://github.com/Open-EO/openeo-processes).  ## Process Graphs  As defined above, a **process graph** is a chain of processes with explicit values for their parameters. Technically, a process graph is defined to be a graph of connected processes with exactly one node returning the final result:  ``` <ProcessGraph> := {   \"<ProcessNodeIdentifier>\": <ProcessNode>,   ... } ```  `<ProcessNodeIdentifier>` is a unique key within the process graph that is used to reference (the return value of) this process in arguments of other processes. The identifier is unique only strictly within itself, excluding any parent and child process graphs. Process node identifiers are also strictly scoped and can not be referenced from child or parent process graphs. Circular references are not allowed.  Note: We provide a non-binding [JSON Schema for basic process graph validation](assets/pg-schema.json).  ### Processes (Process Nodes)  A single node in a process graph (i.e. a specific instance of a process) is defined as follows:  ``` <ProcessNode> := {   \"process_id\": <string>,   \"namespace\": <string> / null,   \"description\": <string>,   \"arguments\": <Arguments>,   \"result\": true / false } ``` A process node MUST always contain key-value-pairs named `process_id` and `arguments`. It MAY contain a `description`.  One of the nodes in a map of processes (the final one) MUST have the `result` flag set to `true`, all the other nodes can omit it as the default value is `false`. Having such a node is important as multiple end nodes are possible, but in most use cases it is important to exactly specify the return value to be used by other processes. Each child process graph must also specify a result node similar to the \"main\" process graph.  `process_id` MUST be a valid process ID in the `namespace` given. Clients SHOULD warn the user if a user-defined process is added with the same identifier as one of the pre-defined process.  ### Arguments  A process can have an arbitrary number of arguments. Their name and value are specified  in the process specification as an object of key-value pairs:  ``` <Arguments> := {   \"<ParameterName>\": <string|number|boolean|null|array|object|ResultReference|UserDefinedProcess|ParameterReference> } ```  **Notes:** - The specified data types are the native data types supported by JSON, except for `ResultReference`, `UserDefinedProcess` and `ParameterReference`. - Objects are not allowed to have keys with the following reserved names:      * `from_node`, except for objects of type `ResultReference`     * `process_graph`, except for objects of type `UserDefinedProcess`     * `from_parameter`, except for objects of type `ParameterReference`  - Arrays and objects can also contain a `ResultReference`, a `UserDefinedProcess` or a `ParameterReference`. So back-ends must *fully* traverse the process graphs, including all children.  ### Accessing results of other process nodes  A value of type `<ResultReference>` is an object with a key `from_node` and a `<ProcessNodeIdentifier>` as corresponding value:  ``` <ResultReference> := {   \"from_node\": \"<ProcessNodeIdentifier>\" } ```  This tells the back-end that the process expects the result (i.e. the return value) from another process node to be passed as argument. The `<ProcessNodeIdentifier>` is strictly scoped and can only reference nodes from within the same process graph, not child or parent process graphs.  ### User-defined process  A user-defined process in a process graph is a child process graph, to be evaluated as part of another process.  **Example**: You want to calculate the absolute value of each pixel in a data cube. This can be achieved in openEO by executing the `apply` process and pass it a user-defined process as the \"operator\" to apply to each pixel. In this simple example, the \"child\" process graph defining the user-defined process consists of a single process `absolute`, but it can be arbitrary complex in general.  A `<UserDefinedProcess>` argument MUST at least consist of an object with a key `process_graph`. Optionally, it can also be described with the same additional properties available for pre-defined processes such as an id, parameters, return values etc. When embedded in a process graph, these additional properties of a user-defined process are usually not used, except for validation purposes.  ``` <UserDefinedProcess> := {   \"process_graph\": <ProcessGraph>,   ... } ```  ### Accessing process parameters  A \"parent\" process that works with a user-defined process can make so called *process graph parameters* available to the \"child\" logic. Processes in the \"child\" process graph can access these parameters by passing a `ParameterReference` object as argument. It is an object with key `from_parameter` specifying the name of the process graph parameter:  ``` <ParameterReference> := {   \"from_parameter\": \"<ParameterReferenceName>\" } ```  The parameter names made available for `<ParameterReferenceName>` are defined and passed to the process graph by one of the parent entities. The parent could be a process (such as `apply` or `reduce_dimension`) or something else that executes a process graph (a secondary web service for example). If the parent is a process, the parameter are defined in the [`parameters` property](#section/Processes/Defining-Processes) of the corresponding JSON Schema.  In case of the example given above, the parameter `process` in the process [`apply`](https://processes.openeo.org/#apply) defines two process graph parameters: `x` (the value of each pixel that will be processed) and `context` (additional data passed through from the user). The process `absolute` expects an argument with the same name `x`. The process graph for the example would look as follows:  ``` {   \"process_id\": \"apply\",   \"arguments\": {     \"data\": {\"from_node\": \"loadcollection1\"}     \"process\": {       \"process_graph\": {         \"abs1\": {           \"process_id\": \"absolute\",           \"arguments\": {             \"x\": {\"from_parameter\": \"x\"}           },           \"result\": true         }       }     }   } } ```  `loadcollection1` would be a result from another process, which is not part of this example.  **Important:** `<ParameterReferenceName>` is less strictly scoped than `<ProcessNodeIdentifier>`. `<ParameterReferenceName>` can be any parameter from the process graph or any of its parents.  The value for the parameter MUST be resolved as follows: 1. In general the most specific parameter value is used. This means the parameter value is resolved starting from the current scope and then checking each parent for a suitable parameter value until a parameter values is found or the \"root\" process graph has been reached. 2. In case a parameter value is not available, the most unspecific default value from the process graph parameter definitions are used. For example, if default values are available for the root process graph and all children, the default value from the root process graph is used. 3. If no default values are available either, the error `ProcessParameterMissing` must be thrown.  ### Full example for an EVI computation  Deriving minimum EVI (Enhanced Vegetation Index) measurements over pixel time series of Sentinel 2 imagery. The main process graph in blue, child process graphs in yellow:  ![Graph with processing instructions](assets/pg-evi-example.png)  The process graph for the algorithm: [pg-evi-example.json](assets/pg-evi-example.json)  ## Data Processing  Processes can run in three different ways:  1. Results can be pre-computed by creating a ***batch job***. They are submitted to the back-end's processing system, but will remain inactive until explicitly put into the processing queue. They will run only once and store results after execution. Results can be downloaded. Batch jobs are typically time consuming and user interaction is not possible although log files are generated for them. This is the only mode that allows to get an estimate about time, volume and costs beforehand.  2. A more dynamic way of processing and accessing data is to create a **secondary web service**. They allow web-based access using different protocols such as [OGC WMS](http://www.opengeospatial.org/standards/wms), [OGC WCS](http://www.opengeospatial.org/standards/wcs), [OGC API - Features](https://www.ogc.org/standards/ogcapi-features) or [XYZ tiles](https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames). Some protocols such as the OGC WMS or XYZ tiles allow users to change the viewing extent or level of detail (zoom level). Therefore, computations often run *on demand* so that the requested data is calculated during the request. Back-ends should make sure to cache processed data to avoid additional/high costs and reduce waiting times for the user.  3. Processes can also be executed **on-demand** (i.e. synchronously). Results are delivered with the request itself and no job is created. Only lightweight computations, for example previews, should be executed using this approach as timeouts are to be expected for [long-polling HTTP requests](https://www.pubnub.com/blog/2014-12-01-http-long-polling/).  ### Validation  Process graph validation is a quite complex task. There's a [JSON schema](assets/pg-schema.json) for basic process graph validation. It checks the general structure of a process graph, but only checking against the schema is not fully validating a process graph. Note that this JSON Schema is probably good enough for a first version, but should be revised and improved for production. There are further steps to do:  1. Validate whether there's exactly one `result: true` per process graph. 2. Check whether the process names that are referenced in the field `process_id` are actually available in the corresponding `namespace`. 3. Validate all arguments for each process against the JSON schemas that are specified in the corresponding process specifications. 4. Check whether the values specified for `from_node` have a corresponding node in the same process graph. 5. Validate whether the return value and the arguments requesting a return value with `from_node` are compatible. 7. Check the content of arrays and objects. These could include parameter and result references (`from_node`, `from_parameter` etc.).   ### Execution  To process the process graph on the back-end you need to go through all nodes/processes in the list and set for each node to which node it passes data and from which it expects data. In another iteration the back-end can find all start nodes for processing by checking for zero dependencies.  You can now start and execute the start nodes (in parallel, if possible). Results can be passed to the nodes that were identified beforehand. For each node that depends on multiple inputs you need to check whether all dependencies have already finished and only execute once the last dependency is ready.  Please be aware that the result node (`result` set to `true`) is not necessarily the last node that is executed. The author of the process graph may choose to set a non-end node to the result node!
 *
 * The version of the OpenAPI document: 1.1.0
 * Contact: openeo.psc@uni-muenster.de
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package net.wasdi.openeoserver.viewmodels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Describes a specific file format.
 */
@ApiModel(description = "Describes a specific file format.")
@JsonPropertyOrder({
  FileFormat.JSON_PROPERTY_TITLE,
  FileFormat.JSON_PROPERTY_DESCRIPTION,
  FileFormat.JSON_PROPERTY_GIS_DATA_TYPES,
  FileFormat.JSON_PROPERTY_DEPRECATED,
  FileFormat.JSON_PROPERTY_EXPERIMENTAL,
  FileFormat.JSON_PROPERTY_PARAMETERS,
  FileFormat.JSON_PROPERTY_LINKS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2023-03-29T12:14:04.450152500+02:00[Europe/Rome]")
public class FileFormat   {
  public static final String JSON_PROPERTY_TITLE = "title";
  @JsonProperty(JSON_PROPERTY_TITLE)
  private String title;

  public static final String JSON_PROPERTY_DESCRIPTION = "description";
  @JsonProperty(JSON_PROPERTY_DESCRIPTION)
  private String description;

  /**
   * Gets or Sets gisDataTypes
   */
  public enum GisDataTypesEnum {
    RASTER("raster"),
    
    VECTOR("vector"),
    
    TABLE("table"),
    
    OTHER("other");

    private String value;

    GisDataTypesEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static GisDataTypesEnum fromValue(String value) {
      for (GisDataTypesEnum b : GisDataTypesEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_GIS_DATA_TYPES = "gis_data_types";
  @JsonProperty(JSON_PROPERTY_GIS_DATA_TYPES)
  private List<GisDataTypesEnum> gisDataTypes = new ArrayList<>();

  public static final String JSON_PROPERTY_DEPRECATED = "deprecated";
  @JsonProperty(JSON_PROPERTY_DEPRECATED)
  private Boolean deprecated = false;

  public static final String JSON_PROPERTY_EXPERIMENTAL = "experimental";
  @JsonProperty(JSON_PROPERTY_EXPERIMENTAL)
  private Boolean experimental = false;

  public static final String JSON_PROPERTY_PARAMETERS = "parameters";
  @JsonProperty(JSON_PROPERTY_PARAMETERS)
  private Map<String, ResourceParameter> parameters = new HashMap<>();

  public static final String JSON_PROPERTY_LINKS = "links";
  @JsonProperty(JSON_PROPERTY_LINKS)
  private List<Link> links = null;

  public FileFormat title(String title) {
    this.title = title;
    return this;
  }

  /**
   * A human-readable short title to be displayed to users **in addition** to the names specified in the keys. This property is only for better user experience so that users can understand the names better. Example titles could be &#x60;GeoTiff&#x60; for the key &#x60;GTiff&#x60; (for file formats) or &#x60;OGC Web Map Service&#x60; for the key &#x60;WMS&#x60; (for service types). The title MUST NOT be used in communication (e.g. in process graphs), although clients MAY translate the titles into the corresponding names.
   * @return title
   **/
  @JsonProperty(value = "title")
  @ApiModelProperty(value = "A human-readable short title to be displayed to users **in addition** to the names specified in the keys. This property is only for better user experience so that users can understand the names better. Example titles could be `GeoTiff` for the key `GTiff` (for file formats) or `OGC Web Map Service` for the key `WMS` (for service types). The title MUST NOT be used in communication (e.g. in process graphs), although clients MAY translate the titles into the corresponding names.")
  
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public FileFormat description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Detailed description to explain the entity.  [CommonMark 0.29](http://commonmark.org/) syntax MAY be used for rich text representation.
   * @return description
   **/
  @JsonProperty(value = "description")
  @ApiModelProperty(value = "Detailed description to explain the entity.  [CommonMark 0.29](http://commonmark.org/) syntax MAY be used for rich text representation.")
  
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public FileFormat gisDataTypes(List<GisDataTypesEnum> gisDataTypes) {
    this.gisDataTypes = gisDataTypes;
    return this;
  }

  public FileFormat addGisDataTypesItem(GisDataTypesEnum gisDataTypesItem) {
    this.gisDataTypes.add(gisDataTypesItem);
    return this;
  }

  /**
   * Specifies the supported GIS spatial data types for this format. It is RECOMMENDED to specify at least one of the data types, which will likely become a requirement in a future API version.
   * @return gisDataTypes
   **/
  @JsonProperty(value = "gis_data_types")
  @ApiModelProperty(required = true, value = "Specifies the supported GIS spatial data types for this format. It is RECOMMENDED to specify at least one of the data types, which will likely become a requirement in a future API version.")
  @NotNull 
  public List<GisDataTypesEnum> getGisDataTypes() {
    return gisDataTypes;
  }

  public void setGisDataTypes(List<GisDataTypesEnum> gisDataTypes) {
    this.gisDataTypes = gisDataTypes;
  }

  public FileFormat deprecated(Boolean deprecated) {
    this.deprecated = deprecated;
    return this;
  }

  /**
   * Declares that the specified entity is deprecated with the potential to be removed in any of the next versions. It should be transitioned out of usage as soon as possible and users should refrain from using it in new implementations.
   * @return deprecated
   **/
  @JsonProperty(value = "deprecated")
  @ApiModelProperty(value = "Declares that the specified entity is deprecated with the potential to be removed in any of the next versions. It should be transitioned out of usage as soon as possible and users should refrain from using it in new implementations.")
  
  public Boolean getDeprecated() {
    return deprecated;
  }

  public void setDeprecated(Boolean deprecated) {
    this.deprecated = deprecated;
  }

  public FileFormat experimental(Boolean experimental) {
    this.experimental = experimental;
    return this;
  }

  /**
   * Declares that the specified entity is experimental, which means that it is likely to change or may produce unpredictable behaviour. Users should refrain from using it in production, but still feel encouraged to try it out and give feedback.
   * @return experimental
   **/
  @JsonProperty(value = "experimental")
  @ApiModelProperty(value = "Declares that the specified entity is experimental, which means that it is likely to change or may produce unpredictable behaviour. Users should refrain from using it in production, but still feel encouraged to try it out and give feedback.")
  
  public Boolean getExperimental() {
    return experimental;
  }

  public void setExperimental(Boolean experimental) {
    this.experimental = experimental;
  }

  public FileFormat parameters(Map<String, ResourceParameter> parameters) {
    this.parameters = parameters;
    return this;
  }

  public FileFormat putParametersItem(String key, ResourceParameter parametersItem) {
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * Specifies the supported parameters for this file format.
   * @return parameters
   **/
  @JsonProperty(value = "parameters")
  @ApiModelProperty(required = true, value = "Specifies the supported parameters for this file format.")
  @NotNull @Valid 
  public Map<String, ResourceParameter> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, ResourceParameter> parameters) {
    this.parameters = parameters;
  }

  public FileFormat links(List<Link> links) {
    this.links = links;
    return this;
  }

  public FileFormat addLinksItem(Link linksItem) {
    if (this.links == null) {
      this.links = new ArrayList<>();
    }
    this.links.add(linksItem);
    return this;
  }

  /**
   * Links related to this file format, e.g. external documentation.  For relation types see the lists of [common relation types in openEO](#section/API-Principles/Web-Linking).
   * @return links
   **/
  @JsonProperty(value = "links")
  @ApiModelProperty(value = "Links related to this file format, e.g. external documentation.  For relation types see the lists of [common relation types in openEO](#section/API-Principles/Web-Linking).")
  @Valid 
  public List<Link> getLinks() {
    return links;
  }

  public void setLinks(List<Link> links) {
    this.links = links;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileFormat fileFormat = (FileFormat) o;
    return Objects.equals(this.title, fileFormat.title) &&
        Objects.equals(this.description, fileFormat.description) &&
        Objects.equals(this.gisDataTypes, fileFormat.gisDataTypes) &&
        Objects.equals(this.deprecated, fileFormat.deprecated) &&
        Objects.equals(this.experimental, fileFormat.experimental) &&
        Objects.equals(this.parameters, fileFormat.parameters) &&
        Objects.equals(this.links, fileFormat.links);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, description, gisDataTypes, deprecated, experimental, parameters, links);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FileFormat {\n");
    
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    gisDataTypes: ").append(toIndentedString(gisDataTypes)).append("\n");
    sb.append("    deprecated: ").append(toIndentedString(deprecated)).append("\n");
    sb.append("    experimental: ").append(toIndentedString(experimental)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("    links: ").append(toIndentedString(links)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

