/**
 * Created by a.corrado on 18/01/2017.
 */

'use strict';
angular.module('wasdi.ProcessesLaunchedService', ['wasdi.ProcessesLaunchedService']).
service('ProcessesLaunchedService', ['ConstantsService','$rootScope','$http', function (oConstantsService,$rootScope,$http) {
    this.m_aoProcessesRunning = [];
    this.m_aoProcessesStopped = [];

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
        console.log("Last loadProcessesFromServer():"+utilsGetTimeStamp());
        this.m_oHttp.get(this.APIURL + '/process/lastbyws?sWorkspaceId='+sWorkSpaceId).success(function (data, status)
            {
                if(!utilsIsObjectNullOrUndefined(data))
                {
                    /*
                    // Full Process Log for extreme debug
                    console.log("ProcessLaunchedService.loadProcessesFromServer PROCESSES LOG:")
                    data.forEach(function (i) {
                        console.log(i.operationType + ": " + i.status + " " + i.progressPerc + "%")
                    });
                    */

                    oService.m_aoProcessesRunning = data;
                    oService.updateProcessesBar();
                }
            }).error(function (data,status)
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN LOAD PROCESSES");
            });
    }

    this.loadAllProcessesFromServer = function(sWorkSpaceId)
    {
        var oService = this;
        console.log("Last loadProcessesFromServer():"+utilsGetTimeStamp());
        this.m_oHttp.get(this.APIURL + '/process/byws?sWorkspaceId='+sWorkSpaceId).success(function (data, status)
            {
                if(!utilsIsObjectNullOrUndefined(data))
                {
                    /*
                    // Full Process Log for extreme debug
                    console.log("ProcessLaunchedService.loadProcessesFromServer PROCESSES LOG:")
                    data.forEach(function (i) {
                        console.log(i.operationType + ": " + i.status + " " + i.progressPerc + "%")
                    });
                    */

                    oService.m_aoProcessesRunning = data;
                    oService.updateProcessesBar();
                }
            }).error(function (data,status)
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN LOAD PROCESSES");
            });
    }

    this.getAllProcessesFromServer = function(sWorkSpaceId)
    {
        return this.m_oHttp.get(this.APIURL + '/process/byws?sWorkspaceId='+sWorkSpaceId);
    }

    this.removeProcessInServer = function(sPidInput,sWorkSpaceId,oProcess)
    {
        if(utilsIsObjectNullOrUndefined(sPidInput)===true) return false;

        var oService = this;
        this.m_oHttp.get(this.APIURL + '/process/delete?sProcessObjId=' + sPidInput).success(function (data, status)
            {
                if(utilsIsObjectNullOrUndefined(sWorkSpaceId) === false )
                {
                    oProcess.status = "stopped";
                    oService.m_aoProcessesStopped.push(oProcess);
                    oService.loadProcessesFromServer(sWorkSpaceId);
                }
            }).error(function (data,status)
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR WHILE KILLING THE PROCESS");
            });
        return true;
    }

    this.getProcesses = function()
    {
        return this.m_aoProcessesRunning;
    }


    this.updateProcessesBar = function()
    {
        //send a message to RootController for update the bar of processes
        $rootScope.$broadcast('m_aoProcessesRunning:updated',true);
    }


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
        if(utilsIsString(sProductId) == false) return false;

        if(utilsIsStrNullOrEmpty(sProductId) == true) return false;

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

                        if(utilsIsSubstring(sProductId,oId) == true) return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    this.checkIfFileIsDownloading = function(oLayer,sTypeOfProcess)
    {
        if(utilsIsObjectNullOrUndefined(oLayer)=== true) return false;
        var sProcessName = oLayer.title;
        var sLink = oLayer.link;
        if(utilsIsStrNullOrEmpty(sProcessName)) return false;
        if(utilsIsStrNullOrEmpty(sTypeOfProcess)) return false;

        var sProcess = {"productName":sProcessName,"operationType":sTypeOfProcess, "link":sLink};

        if(utilsIsObjectNullOrUndefined(sProcess)) return false;

        var aoProcesses = this.getProcesses();
        if(utilsIsObjectNullOrUndefined(aoProcesses)) return false;

        var iNumberOfProcesses = aoProcesses.length;

        for(var iIndex = 0; iIndex < iNumberOfProcesses; iIndex++ )
        {
            /*check if the processes are equals*/
            //aoProcesses[iIndex].productName == sProcess.productName
            if( (utilsIsSubstring(aoProcesses[iIndex].productName, sProcess.productName)===true || utilsIsSubstring(aoProcesses[iIndex].productName, sProcess.link)===true)
                && aoProcesses[iIndex].operationType == sProcess.operationType && aoProcesses[iIndex].status=== "RUNNING")
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

}]);
