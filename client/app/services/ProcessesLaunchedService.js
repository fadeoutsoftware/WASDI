/**
 * Created by a.corrado on 18/01/2017.
 */

'use strict';
angular.module('wasdi.ProcessesLaunchedService', ['wasdi.ProcessesLaunchedService']).
service('ProcessesLaunchedService', ['ConstantsService','$rootScope','$http', function (oConstantsService,$rootScope,$http) {
    this.m_aoProcessesRunning = [];
    this.m_aoProcessesStopped = []
    /*
    * this.m_aoProcessesRunning  is a list of object
    * {processName: , nodeId:,typeOfProcess: }
    * */
    this.COOKIE_EXPIRE_TIME_DAYS = 1;//days
    this.m_oConstantsService = oConstantsService;
    this.APIURL =  this.m_oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    /* ATTENTION!!! THE ORDER OF TYPE PROCESS IS IMPORTANT !! */
    this.TYPE_OF_PROCESS=["DOWNLOAD","PUBLISHBAND","PUBLISH","UPDATEPROCESSES"];
    /*TODO ADD ID USER FOR COOKIE*/

    this.loadProcessesFromServer = function(sWorkSpaceId)
    {
        var oService = this;
        this.m_oHttp.get(this.APIURL + '/process/byws?sWorkspaceId='+sWorkSpaceId)// /ws/processbyws = /process/byws
            .success(function (data, status)
            {
                if(!utilsIsObjectNullOrUndefined(data))
                {
                    console.log("PROCESSES:")
                    data.forEach(function (i) {
                        console.log(i.operationType + ": " + i.status + " " + i.progressPerc + "%")
                    })


                    oService.m_aoProcessesRunning = data;
                    oService.updateProcessesBar();//Aggiungere update della lista dei processi
                }
            })
            .error(function (data,status)
            {
                utilsVexDialogAlertTop("Error in load Processes");
            });
    }

    this.removeProcessInServer = function(sPidInput,sWorkSpaceId,oProcess)
    {
        if(utilsIsObjectNullOrUndefined(sPidInput)===true)
            return false;

        var oService = this;
        this.m_oHttp.get(this.APIURL + '/process/delete?sProcessObjId=' + sPidInput)// /ws/processbyws = /process/byws
            .success(function (data, status)
            {

                if(utilsIsObjectNullOrUndefined(sWorkSpaceId) === false )
                {
                    oProcess.status = "stopped";
                    oService.m_aoProcessesStopped.push(oProcess);
                    oService.loadProcessesFromServer(sWorkSpaceId);

                }

                // oController.loadProcessesFromServer()
            })
            .error(function (data,status)
            {
                utilsVexDialogAlertTop("Error while kill the process ");
            });
        return true;
    }
    /****************************************************/

    ///*LOCAL STORAGE METHODS */
    //this.getProcessesRunningListByLocalStorage = function(sWorkSpaceId,sUserId)
    //{
    //    if(utilsIsStrNullOrEmpty(sWorkSpaceId))
    //        return null;
    //    if(utilsIsStrNullOrEmpty(sUserId))
    //        return null;
    //    return JSON.parse(this.m_oConstantsService.getItemInLocalStorage("m_aoProcessesRunning"+sWorkSpaceId+sUserId));
    //}
    //
    //this.setProcessesRunningListByLocalStorage  = function(oValue,sWorkSpaceId,sUserId)
    //{
    //    if(utilsIsObjectNullOrUndefined(oValue))
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sWorkSpaceId))
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sUserId))
    //        return false;
    //    var sValue = JSON.stringify(oValue);
    //    this.m_oConstantsService.setItemLocalStorage("m_aoProcessesRunning"+sWorkSpaceId+sUserId,sValue);
    //
    //    return true;
    //
    //}
    ///****************************************************/
    //
    //
    //this.loadProcessesByLocalStorage = function(sWorkSpaceId,sUserId)
    //{
    //    if(utilsIsStrNullOrEmpty(sWorkSpaceId))
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sUserId))
    //        return false;
    //
    //    this.m_aoProcessesRunning =  this.getProcessesRunningListByLocalStorage (sWorkSpaceId,sUserId);
    //    if(utilsIsObjectNullOrUndefined(this.m_aoProcessesRunning))
    //        this.m_aoProcessesRunning = [];
    //    return true;
    //}
    //
    //
    ////GET Processes by local storage
    this.getProcesses = function()
    {
        return this.m_aoProcessesRunning;
    }
    //
    //this.addProcessesByLocalStorage = function(sProcessName,iIdBandNodeInTree,sTypeOfProcess,sWorkSpaceId,sUserId)
    //{
    //    if(utilsIsStrNullOrEmpty(sProcessName))
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sTypeOfProcess))
    //        return false;
    //
    //    var sProcess = {"processName":sProcessName,"nodeId":iIdBandNodeInTree,"typeOfProcess":sTypeOfProcess};
    //
    //    if(utilsIsObjectNullOrUndefined(sProcess))
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sWorkSpaceId))
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sUserId))
    //        return false;
    //    var asProcess = this.getProcessesByLocalStorage(sWorkSpaceId,sUserId);
    //    if(utilsIsObjectNullOrUndefined(asProcess))
    //        return false;
    //    asProcess.push(sProcess);
    //    this.setProcessesRunningListByLocalStorage(asProcess,sWorkSpaceId,sUserId);
    //    this.loadProcessesByLocalStorage(sWorkSpaceId,sUserId);
    //    this.updateProcessesBar();
    //    return true;
    //}
    //
    this.updateProcessesBar = function()
    {
        //send a message to RootController for update the bar of processes
        $rootScope.$broadcast('m_aoProcessesRunning:updated',true);
    }
    //
    //this.isEmptyProcessesRunningList = function()
    //{
    //    if(utilsIsObjectNullOrUndefined( this.m_aoProcessesRunning) || this.m_aoProcessesRunning.length == 0)
    //        return true;
    //    return false;
    //}
    //
    //
    //
    //this.indexProcess = function(sProcess)
    //{
    //    if(utilsIsObjectNullOrUndefined(sProcess))
    //        return -1;
    //
    //    var iNumberOfProcesses = this.m_aoProcessesRunning.length;
    //    for(var iIndex=0; iIndex < iNumberOfProcesses; iIndex++)
    //    {
    //        if(this.m_aoProcessesRunning[iIndex] == sProcess)
    //            return iIndex
    //
    //    }
    //
    //    return -1;
    //}
    //
    //
    //this.indexProcessFindByProperty = function(oProperty,oValue)
    //{
    //    if(utilsIsObjectNullOrUndefined(oProperty) ||utilsIsObjectNullOrUndefined(oProperty))
    //        return -1;
    //
    //    var iNumberOfProcesses = this.m_aoProcessesRunning.length;
    //    for(var iIndex=0; iIndex < iNumberOfProcesses; iIndex++)
    //    {
    //        if(this.m_aoProcessesRunning[iIndex][oProperty] == oValue)
    //            return iIndex
    //
    //    }
    //    return -1;
    //}
    //
    //this.indexProcessFindByPropertySubstringVersion = function(oProperty,oValue)
    //{
    //    if(utilsIsObjectNullOrUndefined(oProperty) ||utilsIsObjectNullOrUndefined(oProperty))
    //        return -1;
    //
    //    var iNumberOfProcesses = this.m_aoProcessesRunning.length;
    //    for(var iIndex=0; iIndex < iNumberOfProcesses; iIndex++)
    //    {
    //        if(utilsIsSubstring(oValue,this.m_aoProcessesRunning[iIndex][oProperty]) == true)
    //            return iIndex
    //
    //    }
    //    return -1;
    //}
    //
    //this.removeProcess = function(sProcess,sWorkSpaceId,sUserId)
    //{
    //    if(utilsIsObjectNullOrUndefined(sProcess))
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sWorkSpaceId))
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sUserId))
    //        return false;
    //
    //    var iIndexProcess =  this.indexProcess(sProcess);
    //
    //    this.m_aoProcessesRunning.splice(iIndexProcess,1);
    //    this.setProcessesRunningListByLocalStorage(this.m_aoProcessesRunning,sWorkSpaceId,sUserId);
    //}
    //
    //this.removeProcessByProperty = function(oProperty,oValue,sWorkSpaceId,sUserId)
    //{
    //    if(utilsIsObjectNullOrUndefined(oProperty) )
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sWorkSpaceId))
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sUserId))
    //        return false;
    //
    //    var iIndexProcess =  this.indexProcessFindByProperty(oProperty,oValue);
    //    if(iIndexProcess == -1)
    //        return false;
    //    this.m_aoProcessesRunning.splice(iIndexProcess,1);
    //    this.setProcessesRunningListByLocalStorage(this.m_aoProcessesRunning,sWorkSpaceId,sUserId);
    //    return true;
    //}
    //
    //this.removeProcessByPropertySubstringVersion = function(oProperty,oValue,sWorkSpaceId,sUserId)
    //{
    //    if(utilsIsObjectNullOrUndefined(oProperty) )
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sWorkSpaceId))
    //        return false;
    //    if(utilsIsStrNullOrEmpty(sUserId))
    //        return false;
    //    var iIndexProcess =  this.indexProcessFindByPropertySubstringVersion(oProperty,oValue);
    //    if(iIndexProcess == -1)
    //        return false;
    //    this.m_aoProcessesRunning.splice(iIndexProcess,1);
    //    this.setProcessesRunningListByLocalStorage(this.m_aoProcessesRunning,sWorkSpaceId,sUserId);
    //    this.updateProcessesBar();
    //    return true;
    //}
    //
    this.thereAreSomePublishBandProcess = function()
    {
        if(! (utilsIsObjectNullOrUndefined(this.m_aoProcessesRunning) || this.m_aoProcessesRunning.length == 0))
        {
            for(var iIndex = 0; iIndex < this.m_aoProcessesRunning.length; iIndex++)
            {
                if(this.m_aoProcessesRunning[iIndex].operationType == this.getTypeOfProcessPublishingBand())
                    return true;
            }
            return false;
        }
        return false;
    }

    this.thereIsPublishBandProcessOfTheProduct = function(sProductId)
    {
        if(utilsIsString(sProductId) == false)
            return false;
        if(utilsIsStrNullOrEmpty(sProductId) == true)
            return false;

        if(! (utilsIsObjectNullOrUndefined(this.m_aoProcessesRunning) || this.m_aoProcessesRunning.length == 0))
        {
            for(var iIndex = 0; iIndex < this.m_aoProcessesRunning.length; iIndex++)
            {
                if(this.m_aoProcessesRunning[iIndex].operationType != this.TYPE_OF_PROCESS[1] )//publish band
                {
                    var asProductNameSplit = this.m_aoProcessesRunning[iIndex].productName.split(".");// remove .zip .rar ....
                    if(utilsIsObjectNullOrUndefined (asProductNameSplit) == false && asProductNameSplit.length != 0 )
                    {
                        var sProductName = asProductNameSplit[0];
                        var oId = sProductName;
                        if(utilsIsSubstring(sProductId,oId) == true)
                            return true;
                    }
                }
                //if(this.m_aoProcessesRunning[iIndex].operationType == this.getTypeOfProcessPublishingBand())
                //    return true;
            }
            return false;
        }
        return false;
    }

    this.checkIfFileIsDownloading = function(sProcessName,sTypeOfProcess)
    {
        if(utilsIsStrNullOrEmpty(sProcessName))
            return false;
        if(utilsIsStrNullOrEmpty(sTypeOfProcess))
            return false;

        var sProcess = {"productName":sProcessName,"operationType":sTypeOfProcess};

        if(utilsIsObjectNullOrUndefined(sProcess))
            return false;

        var aoProcesses = this.getProcesses();
        if(utilsIsObjectNullOrUndefined(aoProcesses)) return false;

        var iNumberOfProcesses = aoProcesses.length;

        for(var iIndex = 0; iIndex < iNumberOfProcesses; iIndex++ )
        {
            /*check if the processes are equals*/
            if(aoProcesses[iIndex].productName == sProcess.productName && aoProcesses[iIndex].operationType == sProcess.operationType)
            {
                return true;
            }
        }
        return false;

    }

    this.getTypeOfProcessProductDownload = function()
    {
        return this.TYPE_OF_PROCESS[0];
    }

    this.getTypeOfProcessPublishingBand = function()
    {
        return this.TYPE_OF_PROCESS[1];
    }

    this.checkIfProcessWasStopped = function(oProcessInput)
    {
        if(!this.m_aoProcessesStopped)
            return false;
        var iLengthProcessesStopped = this.m_aoProcessesStopped.length;
        for(var iIndexProcess = 0; iIndexProcess < iLengthProcessesStopped;  iIndexProcess++)
        {
            if( (this.m_aoProcessesStopped[iIndexProcess].processObjId === oProcessInput.processObjId) && (this.m_aoProcessesStopped[iIndexProcess].operationType === oProcessInput.operationType)
                &&(this.m_aoProcessesStopped[iIndexProcess].operationDate === oProcessInput.operationDate) && (this.m_aoProcessesStopped[iIndexProcess].pid === oProcessInput.pid)
                &&(this.m_aoProcessesStopped[iIndexProcess].product_name === oProcessInput.product_name) )
            {
                return true;
            }

        }
        return false;
    }
    //this.addProcessToList= function (sProcessName,sTypeOfProcess,sTimeStamp)
    //{
    //    if(utilsIsObjectNullOrUndefined(sProcessName))
    //        return false;
    //    if(utilsIsObjectNullOrUndefined(sTypeOfProcess))
    //        return false;
    //    if(utilsIsObjectNullOrUndefined(sTimeStamp))
    //        return false;
    //    var oProcess = {"processName":sProcessName,"timeStamp":iIdBandNodeInTree,"typeOfProcess":sTypeOfProcess};
    //    this.m_aoProcessesRunning.push(oProcess);
    //}
}]);
