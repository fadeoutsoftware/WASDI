/**
 * Wasdi class expose methods and utilities to interact with WASDI services, using
 * Javascript/Typescript as specification and programming language. The package available
 * through npm repository, can be imported.
 * Object definitions are also provided.
 * Check README and LICENSE for further details
 */
export class Wasdi {
  _m_sUser: string;
  _m_sPassword: string;

  _m_sActiveWorkspace: string;
  _m_sWorkspaceOwner: string;
  _m_sWorkspaceBaseUrl: string;

  _m_sParametersFilePath: string;
  _m_sSessionId: string;
  _m_bValidSession: boolean;
  _m_sBasePath: string;

  _m_bDownloadActive: boolean;
  _m_bUploadActive: boolean;
  _m_bVerbose: boolean;
  _m_aoParamsDictionary = {};

  _m_sMyProcId: string;
  _m_sBaseUrl = "https://www.wasdi.net/wasdiwebserver/rest";
  _m_bIsOnServer: boolean;
  _m_iRequestsTimeout = 2 * 60;
  // handled through field instead of local variables
  _m_sWorkspaceName = "";
  _m_sWorkspaceId = "";

  _m_aoRunningProcessId: string[];

  _m_asRunningProcessorIds: string[];

  constructor() {
    this._m_sUser = undefined;
    this._m_sPassword = undefined;

    this._m_sActiveWorkspace = undefined;
    this._m_sWorkspaceOwner = "";
    this._m_sWorkspaceBaseUrl = "";

    this._m_sParametersFilePath = undefined;
    this._m_sSessionId = "";
    this._m_bValidSession = false;
    this._m_sBasePath = undefined;

    this._m_bDownloadActive = true;
    this._m_bUploadActive = true;
    this._m_bVerbose = true;
    this._m_aoParamsDictionary = {};

    this._m_sMyProcId = "";
    this._m_sBaseUrl = "https://www.wasdi.net/wasdiwebserver/rest";
    this._m_bIsOnServer = false;
    this._m_iRequestsTimeout = 2 * 60;

    this._m_sWorkspaceName = "";
    this._m_sWorkspaceId = "";
  }

  /**
   * Print status utility, prints the information about the current session with WASDI
   */
  printStatus() {
    console.log("");
    console.log("[INFO] jswasdilib.printStatus: user: " + this.User);
    console.log("[INFO] jswasdilib.printStatus: password: ***********");
    console.log("[INFO] jswasdilib.printStatus: session id: " + this.SessionId);
    console.log(
        "[INFO] jswasdilib.printStatus: active workspace: " + this.ActiveWorkspace
    );
    console.log(
        "[INFO] jswasdilib.printStatus: workspace owner: " + this.WorkspaceOwner
    );
    console.log(
        "[INFO] jswasdilib.printStatus: parameters file path: " +
        this.ParametersFilePath
    );
    console.log("[INFO] jswasdilib.printStatus: base path: " + this.BasePath);
    console.log(
        "[INFO] jswasdilib.printStatus: download active: " + this.DownloadActive
    );
    console.log(
        "[INFO] jswasdilib.printStatus: upload active: " + this.UploadActive
    );
    console.log("[INFO] jswasdilib.printStatus: verbose: " + this.Verbose);
    console.log(
        "[INFO] jswasdilib.printStatus: param dict: " + this.ParamsDictionary
    );
    console.log("[INFO] jswasdilib.printStatus: proc id: " + this.MyProcId);
    console.log("[INFO] jswasdilib.printStatus: base url: " + this.BaseUrl);
    console.log(
        "[INFO] jswasdilib.printStatus: is on server: " + this.IsOnServer
    );
    console.log(
        "[INFO] jswasdilib.printStatus: workspace base url: " +
        this.WorkspaceBaseUrl
    );
    this._m_bValidSession = this._m_sSessionId != undefined;
    if (this.ValidSession) {
      console.log("[INFO] jswasdilib.printStatus: session is valid :-)");
    } else {
      console.log(
          "[ERROR] jswasdilib.printStatus: session is not valid :-(" +
          "  ******************************************************************************"
      );
    }
  }

  /**
   * Test method to check wasdi instance, with a tiny bit of developer's traditions.
   * Used across this library to check the connection state.
   * @param  noOutput if true, the method doesn't output the response on the console
   */
  helloWasdiWorld(noOutput: boolean): string {
    var xhr = new XMLHttpRequest();
    let response = undefined;
    xhr.addEventListener("readystatechange", function () {
      if (this.readyState === 4) {
        response = this.responseText;
        if (!noOutput) console.log(response);
      }
    });

    xhr.open("GET", this._m_sBaseUrl + "/wasdi/hello", false);

    xhr.send();
    return response;
  }

  /**
   * Util methods to check initialization of the current base URL.
   * Can be used to verify the WASDI service on the selected node.
   * @returns {boolean} true, if the connection is ok, false instead
   */
  checkBaseUrl() {
    let response = this.helloWasdiWorld(true);
    if (response.includes("Hello Wasdi")) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Api call for the login to WASDI services. Valid credential must be available.
   * @param sUserName The username, corresponding to the e-mail used during registration
   * @param sPassword The selected password
   */
  login() {
    var oCredentials = JSON.stringify({
      userId: this.User,
      userPassword: this.Password,
    });

    var xhr = new XMLHttpRequest();
    //xhr.withCredentials = true;

    xhr.addEventListener("readystatechange", function () {
      if (this.readyState === 4) {
      }
    });

    xhr.open("POST", this._m_sBaseUrl + "/auth/login/", false);
    xhr.setRequestHeader("Content-Type", "application/json");

    xhr.send(oCredentials);
    // local scope
    let response = JSON.parse(xhr.response);

    if ("sessionId" in response && response.sessionId != undefined) {
      this._m_sSessionId = response.sessionId; // init the current session id
      return "[jswasdilib]login : User authenticated ";
    }

    return "[jswasdilib]login : Login failed, please check configuration";
  }

  /**
   * Loads configuration and parameters. If no filename is specified, it
   * loads the file config.json and parameters.json from the same level directory of the calling
   * URL
   * @param configFile a JSON containing all the required information to login to WASDI, please check repository for example
   * @param parametersFile a JSON containing the parameters that can be used in case of a launch of an appliciation
   */
  loadConfig(configFile: string, parametersFile: string) {
    let sUrl = configFile;
    if (configFile == undefined) {
      // default value
      sUrl = "./config.json";
    }
    var request = new XMLHttpRequest();
    request.open("GET", sUrl, false); // `false` makes the request synchronous
    request.send(null);

    if (request.status === 200) {
      let jsondata = JSON.parse(request.responseText);
      this._m_sUser = jsondata.USER ? jsondata.USER : this._m_sUser;
      this._m_sPassword = jsondata.PASSWORD
          ? jsondata.PASSWORD
          : this._m_sPassword;
      this._m_sWorkspaceName = jsondata.WORKSPACE
          ? jsondata.WORKSPACE
          : this._m_sWorkspaceName;
      this._m_sWorkspaceId = jsondata.WORKSPACEID
          ? jsondata.WORKSPACEID
          : this._m_sWorkspaceId;
      this._m_sBasePath = jsondata.BASEPATH
          ? jsondata.BASEPATH
          : this._m_sBasePath;
      this._m_sParametersFilePath = jsondata.PARAMETERSFILEPATH
          ? jsondata.PARAMETERSFILEPATH
          : this._m_sParametersFilePath;
      this._m_bDownloadActive = jsondata.DOWNLOADACTIVE
          ? jsondata.DOWNLOADACTIVE
          : this._m_bDownloadActive;
      this._m_bUploadActive = jsondata.UPLOADACTIVE
          ? jsondata.UPLOADACTIVE
          : this._m_bUploadActive;
      this._m_bVerbose = jsondata.VERBOSE ? jsondata.VERBOSE : this._m_bVerbose;
      this._m_sBaseUrl = jsondata.BASEURL ? jsondata.BASEURL : this._m_sBaseUrl;
      this._m_iRequestsTimeout = jsondata.REQUESTTIMEOUT
          ? jsondata.REQUESTTIMEOUT
          : this._m_iRequestsTimeout;
    }

    if (!this.checkBaseUrl()) {
      console.log(
          "[jswasdilib] Error in base Url - Can't contact wasdi instance, please check configuration "
      );
    }

    if (this._m_sUser != undefined && this._m_sPassword != undefined) {
      this.loadParameters(parametersFile);
    }
  }

  /**
   * Loads a json containing the parameters which are then imported in a dedicated field.
   * If filename is not specified the methods search for "parameters.json", as default
   * @param filename the file name of the JSON containing the parameters
   */
  loadParameters(filename: string) {
    let sUrl = filename;
    if (filename == undefined) {
      // default value
      sUrl = "./parameters.json";
    }
    var request = new XMLHttpRequest();
    request.open("GET", sUrl, false); // `false` makes the request synchronous
    request.send(null);

    if (request.status === 200) {
      let jsondata = JSON.parse(request.responseText);
      this._m_aoParamsDictionary = jsondata;
    }
  }

  /**
   * Asyncronous call to load and initialize base parameters of the library
   * @param filename
   * @returns {Promise<void>}
   */
  async asyncLoadConfig(filename: string) {
    let initPromiseChain = fetch(filename)
        .then((response) => {
          return response.json();
        })
        .then((jsondata) => {
          // async, so must init here

          this._m_sUser = jsondata.USER;
          this._m_sPassword = jsondata.PASSWORD;
          this._m_sWorkspaceName = jsondata.WORKSPACE;
          this._m_sWorkspaceId = jsondata.WORKSPACEID;
          this._m_sBasePath = jsondata.BASEPATH;
          this._m_sParametersFilePath = jsondata.PARAMETERSFILEPATH;
          this._m_bDownloadActive = jsondata.DOWNLOADACTIVE;
          this._m_bUploadActive = jsondata.UPLOADACTIVE;
          this._m_bVerbose = jsondata.VERBOSE;

          this._m_iRequestsTimeout = jsondata.REQUESTTIMEOUT;
          // suppose that, at least, user and password are set
          let bIsValid =
              this._m_sUser != undefined && this._m_sPassword != undefined;
          if (!bIsValid) {
            console.log(
                "[ERROR] jswasdilib._loadConfig: something went wrong" +
                "  ******************************************************************************"
            );
          }
          return bIsValid;
        })
        .then((isValid) => {
          if (isValid && this._m_sParametersFilePath != undefined) {
            fetch(this._m_sParametersFilePath)
                .then((response) => {
                  return response.json();
                })
                .then((jsondata) => {
                  this._m_aoParamsDictionary = jsondata;
                });
          }
        });
    let result = await initPromiseChain;
    return result;
  }

  /**
   * Retrieve the information of a single Workspace, by using its ID
   * @param workspaceID a String containing the workspace ID
   */
  getWorkspaceById(workspaceID: string) {
    var xhr = new XMLHttpRequest();

    xhr.addEventListener("readystatechange", function () {
      if (this.readyState === 4) {
        console.log(this.responseText);
      }
    });

    xhr.open(
        "GET",
        this._m_sBaseUrl + "/ws/getws?workspace=" + workspaceID,
        false
    );
    xhr.setRequestHeader("Accept", "application/json, text/plain, */*");
    xhr.setRequestHeader("x-session-token", this._m_sSessionId);

    xhr.send();
    return xhr.response;
  }

  // @ts-ignore
  /**
   * Private util method to return parsed object from string. Due to requirements, uses the async http call
   * @url String containing the url of the required resource
   * @param params String containing the parameters, must include the correct syntax like '?=[...]'
   * @returns {number|string|{}|any}
   */
  private getObject(url: string, params: string) {
    var xhr = new XMLHttpRequest();

    xhr.addEventListener("readystatechange", function () {
      if (this.readyState === 4) {
      }
    });

    xhr.open("GET", url + params, false);
    xhr.setRequestHeader("Accept", "application/json, text/plain, */*");
    xhr.setRequestHeader("x-session-token", this._m_sSessionId);

    xhr.send();
    return JSON.parse(xhr.response);
  }

  /**
   * Private util method to return parsed object from string. Due to requirements, uses the async http call
   * @url String containing the url of the required resource
   * @param params String containing the parameters, must include the correct syntax like '?=[...]'
   * @returns {number|string|{}|any}
   */
  private postObject(url: string, params: string, data: string) {
    var xhr = new XMLHttpRequest();

    xhr.addEventListener("readystatechange", function () {
      if (this.readyState === 4) {
        //        console.log(this.responseText);
      }
    });

    xhr.open("POST", url + params, false);
    xhr.setRequestHeader("Accept", "application/json, text/plain, */*");
    xhr.setRequestHeader("x-session-token", this._m_sSessionId);

    xhr.send(data);
    return JSON.parse(xhr.response);
  }

  /**
   * Retrieves the list of Workspace of the current logged user.
   */
  workspaceList() {
    var workspaceList = this.getObject(this._m_sBaseUrl + "/ws/byuser", "");
    console.log("[INFO] jswasdilib.workspaceList: Available workspaces : ");
    workspaceList.forEach((a: { workspaceName: string; workspaceId: string }) =>
        console.log(a.workspaceName + " - Id" + a.workspaceId)
    );
    return workspaceList;
  }

  /**
   * Retrieve a list of workspace of the current logged user.
   */
  productListByActiveWs() {
    let productList = this.getObject(
        this._m_sBaseUrl + "/product/byws",
        "?workspace=" + this.ActiveWorkspace
    );
    console.log(
        "[INFO] jswasdilib.productListByActiveWs: Products in the active workspace "
    );
    productList.forEach((a: { name: string }) => console.log(a.name));
    return productList;
  }

  /**
   * Opens a workspace and set it as active workspace. The active workspace is the one used
   * for the following operations, like launch a processor or execute a workflow.
   * @param workspaceID The id of the selected workspace
   * @returns {{workspaceId}|number|string|{}|*}
   */
  openWorkspaceById(workspaceID: string) {
    let ws = this.getObject(
        this._m_sBaseUrl + "/ws/getws",
        "?workspace=" + workspaceID
    ); // retrieves workspace viewmodel
    if (ws.workspaceId) {
      this._m_sActiveWorkspace = ws.workspaceId;
      this._m_sWorkspaceName = ws.name;
      this._m_sWorkspaceBaseUrl = ws.apiUrl;
      console.log(
          "[INFO] jswasdilib.openWorkspace: Opened Workspace " +
          this._m_sWorkspaceName
      );
      return ws;
    } else {
      console.log("[ERROR] Could not find Workspace, please check parameters");
      return;
    }
  }

  /**
   * Open a workspace and set it as active workspace, by using its Id
   * @param workspaceID The id of the selected workspace
   * @returns {{workspaceId}|number|string|{}|*}
   */
  openWorkspace(workspaceName: string) {
    let wsList = this.workspaceList();
    if (wsList) {
      let ws = {
        workspaceId: "",
        workspaceName: "",
      };
      wsList.forEach((a: { workspaceName: string; workspaceId: string }) => {
        if (a.workspaceName == workspaceName) {
          ws = a;
        }
      });
      if (ws.workspaceId) {
        console.log(
            "[INFO] jswasdilib.openWorkspaceByName: Opened Workspace " +
            ws.workspaceName
        );
        return this.openWorkspaceById(ws.workspaceId);
      } else {
        console.log(
            '[INFO] jswasdilib.openWorkspaceByName: Could not find workspace "' +
            workspaceName +
            '"'
        );
      }
    } else {
      console.log(
          "[ERROR] jswasdilib.openWorkspaceByName: please check parameters"
      );
      return;
    }
  }

  /**
   * Launch a process in the current workspace.
   * Check getDeployed method to obtain a list of the available processors
   * @param appname a String containing the name of the selected application
   * @param jsonParameters a JSON containing the parameters for the application, please check the app on WASDI
   * for a specific reference
   */
  launchProcessor(appname: string, jsonParameters: string) {
    if (this._m_sActiveWorkspace) {
      let response = this.postObject(
          this._m_sWorkspaceBaseUrl + "/processors/run",
          "?name=" + appname + "&workspace=" + this._m_sActiveWorkspace,
          jsonParameters
      );
      if (response.processingIdentifier) {
        this._m_aoRunningProcessId.push(response.processingIdentifier);
      }
      return response;
    } else {
      console.log(
          "[INFO] jswasdilib.LaunchProcessor: no workspace opened, please use wasdi.openWorkspace se the active workspace or wasdi.createWorkspace for a new one"
      );
    }
  }

  /**
   * Retrieves a list of applications available on the WASDI marketplace.
   * The response is an array of strings that can be used to launch the particular application
   */
  getDeployed() {
    return this.getObject(this._m_sBaseUrl, "/processors/getdeployed");
  }

  /**
   * Set the payload of a process, identified by its processId
   * @param sProcessId the processId to add the payload
   * @param data JSON string containing the payload
   */
  setProcessPayload(sProcessId: string, data: string) {
    return this.getObject(
        this._m_sWorkspaceBaseUrl + "/process/setpayload",
        "?procws=" + sProcessId + "&payload=" + encodeURIComponent(data)
    );
  }

  /**
   * Create a new workspace for the active user.
   * @param wsName The workspace name, if the name is already used WADI will append a further numeric identifier
   * (like the OS for new folders)
   */
  createWorkspace(wsName: string) {
    let ws = this.getObject(this._m_sBaseUrl + "/ws/create", "?name=" + wsName);
    if (ws.stringValue) {
      console.log(
          "[INFO] jswasdilib.CreateWorkspace: New workspace " +
          wsName +
          " created"
      );
      console.log(
          "[INFO] jswasdilib.CreateWorkspace: Workspace Id " + ws.stringValue
      );
      this._m_sActiveWorkspace = ws.stringValue;
      console.log("[INFO] jswasdilib.CreateWorkspace: Active workspace set");
    }
    return;
  }

  /**
   * Retrieves the process status of a process, identified by its processId.
   * The response contains, among other data, the status and the progress percentage
   * @param processId the string containing the processId selected
   * @return the process Object
   */
  getProcessStatus(processId: string) {
    let procWs = this.getObject(
        this._m_sWorkspaceBaseUrl + "/process/byid",
        "?procws=" + processId
    );
    if (procWs.status) {
      console.log(
          "[INFO] jswasdilib.getProcessStatus: Process status " +
          procWs.status +
          " Percentage " +
          procWs.progressPerc +
          " %"
      );
      return procWs;
    }
  }

  /**
   * Prints details about the status if the processes launched during the last session
   */
  printProcesses() {
    this._m_aoRunningProcessId.forEach((a) => this.getProcessStatus(a));
  }

  /**
   * Retrieves a list of products from the current active workspace
   */
  getProductsActiveWorkspace() {
    return this.getObject(
        this._m_sWorkspaceBaseUrl + "/product/byws",
        "?workspace=" + this._m_sActiveWorkspace
    );
  }

  /**
   * Publish a band of the particular product selected. To obtain a list of available bands, a function
   * that retrieves the product details can be used.
   * @param fileName The product name in the current workspace
   * @param bandName The band that needs to be published
   */
  publishBand(fileName: string, bandName: string) {
    // search filename in the current workspace
    let file = this.getProductsActiveWorkspace().find(
        (a: { fileName: string }) => a.fileName == fileName
    );
    if (file && file.bandsGroups) {
      let band = file.bandsGroups.bands.find(
          (b: { name: string }) => b.name == bandName
      );
      if (band) {
        console.log("[INFO] jswasdilib.publishBand: Band found, begin publish");
        let published = this.getObject(
            this._m_sWorkspaceBaseUrl + "/filebuffer/publishband",
            "?fileUrl=" +
            fileName +
            "&workspace=" +
            this._m_sActiveWorkspace +
            "&band=" +
            bandName
        );
        return published.payload;
      } else {
        console.log("[INFO] jswasdilib.publishBand: Band not found found");
      }
    } else {
      console.log(
          "[INFO] jswasdilib.publishBand: File " +
          fileName +
          " not found in the current workspace"
      );
    }
  }

  get User() {
    return this._m_sUser;
  }

  get Password() {
    return this._m_sPassword;
  }

  get ActiveWorkspace() {
    return this._m_sActiveWorkspace;
  }

  get WorkspaceOwner() {
    return this._m_sWorkspaceOwner;
  }

  get WorkspaceBaseUrl() {
    return this._m_sWorkspaceBaseUrl;
  }

  get ParametersFilePath() {
    return this._m_sParametersFilePath;
  }

  get SessionId() {
    return this._m_sSessionId;
  }

  get ValidSession() {
    return this._m_bValidSession;
  }

  get BasePath() {
    return this._m_sBasePath;
  }

  get DownloadActive() {
    return this._m_bDownloadActive;
  }

  get UploadActive() {
    return this._m_bUploadActive;
  }

  get Verbose() {
    return this._m_bVerbose;
  }

  get ParamsDictionary() {
    return this._m_aoParamsDictionary;
  }

  get MyProcId() {
    return this._m_sMyProcId;
  }

  get BaseUrl() {
    return this._m_sBaseUrl;
  }

  get IsOnServer() {
    return this._m_bIsOnServer;
  }

  get RequestsTimeout() {
    return this._m_iRequestsTimeout;
  }

  get WorkspaceId() {
    return this._m_sWorkspaceId;
  }

  get WorkspaceName() {
    return this._m_sWorkspaceName;
  }
}
