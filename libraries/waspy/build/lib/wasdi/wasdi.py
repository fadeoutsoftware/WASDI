'''
Waspy - WASDI Python Library

Created on 11 Jun 2018

@author: p.campanella - FadeOut Software
'''
import requests

m_sUser = 'urs'
m_sPassword= 'pw'
m_sBasePath = 'c:\\temp\\wasdi'
m_sSessionId = ''
m_sActiveWorkspace = ''
m_sBaseUrl = 'http://www.wasdi.net/wasdiwebserver/rest'

def setUser(sUser):
    """
    Set the WASDI User
    """
    global m_sUser
    m_sUser = sUser

def getUser():
    """
    Get the WASDI User
    """    
    global m_sUser
    return m_sUser
 
def setPassword(sPassword):
    """
    Set the WASDI Password
    """    
    global m_sPassword
    m_sPassword = sPassword

def getPassword():
    """
    Get the WASDI Password
    """    
    global m_sPassword
    return m_sPassword
    
def setBasePath(sBasePath):
    """
    Set the local Base Path for WASDI
    """    
    global m_sBasePath
    m_sBasePath = sBasePath

def getBasePath():
    """
    Get the local Base Path for WASDI
    """    
    global m_sBasePath
    return m_sBasePath
       
def setBaseUrl(sBaseUrl):
    """
    Set the WASDI API URL
    """    
    global m_sBaseUrl
    m_sBaseUrl = sBaseUrl

def getBaseUrl():
    """
    Get the WASDI API URL
    """    
    global m_sBaseUrl
    return m_sBaseUrl

def init():
    """
    Init WASDI Library. Call it after setting user, password, path and url or use it with a config file
    Return True if login was successful, False otherwise
    """    
    global m_sUser
    global m_sPassword
    global m_sBaseUrl
    global m_sSessionId
    
    headers = {'Content-Type': 'application/json'}
    
    sUrl = m_sBaseUrl + '/auth/login'
    
    sPayload = '{"userId":"' + m_sUser+ '","userPassword":"'+ m_sPassword + '" }'
    
    oResult = requests.post(sUrl, data = sPayload,headers=headers)
    
    if (oResult.ok == True):
    
        oJsonResult = oResult.json()
        m_sSessionCookie = oJsonResult['sessionId']
        return True
    else:
        return False

def hello():
    """
    Hello Wasdi to test the connection.
    Return the hello message as Text
    """    
    global m_sBaseUrl
    
    sUrl = m_sBaseUrl + '/wasdi/hello'
    oResult = requests.get(sUrl)
    return oResult.text

def getWorkspaces():
    """
    Get List of user workspaces
    Return an array of WASDI Workspace JSON Objects.
    Each Object is like this
    {
        "ownerUserId":STRING,
        "sharedUsers":[STRING],
        "workspaceId":STRING,
        "workspaceName":STRING    
    }
    """    
    global m_sBaseUrl
    global m_sSessionId
    
    headers = {'Content-Type': 'application/json','x-session-token': m_sSessionCookie}
    
    sUrl = m_sBaseUrl + '/ws/byuser'
        
    oResult = requests.get(sUrl, headers=headers)
    
    if (oResult.ok):
        oJsonResult = oResult.json()
        return oJsonResult
    else:
        return None
    
def getWorkspaceIdByName(sName):
    """
    Get Id of a Workspace from the name
    Return the WorkspaceId as a String, '' if there is any error
    """    
    global m_sBaseUrl
    global m_sSessionId
    
    headers = {'Content-Type': 'application/json','x-session-token': m_sSessionCookie}
    
    sUrl = m_sBaseUrl + '/ws/byuser'
        
    oResult = requests.get(sUrl, headers=headers)
    
    if (oResult.ok):
        oJsonResult = oResult.json()
        
        for oWorkspace in oJsonResult:
            if (oWorkspace['workspaceName'] == sName):
                return oWorkspace['workspaceId']
            
    return ''

def openWorkspace(sWorkspaceName):
    """
    Open a workspace
    return the WorkspaceId as a String, '' if there is any error
    """    
    global m_sActiveWorkspace
    
    m_sActiveWorkspace = getWorkspaceIdByName(sWorkspaceName)
    
    return m_sActiveWorkspace
    
def getProductsByWorkspace(sWorkspaceName):
    """
    Get the list of products in a workspace
    the list is an array of string. Can be empty if there is any error
    """    
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace
    
    sWorkspaceId = getWorkspaceIdByName(sWorkspaceName)
    
    m_sActiveWorkspace = sWorkspaceId
    
    headers = {'Content-Type': 'application/json','x-session-token': m_sSessionCookie}
    payload = {'sWorkspaceId': sWorkspaceId}
    
    sUrl = m_sBaseUrl + '/product/byws'
    
    asProducts = []
        
    oResult = requests.get(sUrl, headers=headers, params=payload)
    
    if (oResult.ok):
        oJsonResults = oResult.json()
        
        for oProduct in oJsonResults:
            asProducts.append(oProduct['fileName'])
    
    return asProducts

def getFullProductPath(sProductName):
    """
    Get the full local path of a product given the product name.
    Use the output of this API to open the file
    """    
    global m_sBasePath
    global m_sActiveWorkspace
    global m_sUser
    
    sFullPath = m_sBasePath
    
    if (not(sFullPath.endswith('/') or sFullPath.endswith('\\'))):
        sFullPath = sFullPath + '/'
    
    sFullPath = sFullPath + m_sUser + '/' + m_sActiveWorkspace + '/' + sProductName
    
    return sFullPath

def getSavePath():
    """
    Get the local base save path for a product. To save use this path + fileName. Path already include '/' as last char
    """    
    global m_sBasePath
    global m_sActiveWorkspace
    global m_sUser
    
    sFullPath = m_sBasePath
    
    if (not(sFullPath.endswith('/') or sFullPath.endswith('\\'))):
        sFullPath = sFullPath + '/'
    
    sFullPath = sFullPath + m_sUser + '/' + m_sActiveWorkspace + '/'
    
    return sFullPath
    
def getWorkflows():
    """
        Get the list of products in a workspace
        Return None if there is any error
        Return an array of WASI Workspace JSON Objects if everything is ok:
        
        {
            "description":STRING,
            "name": STRING,
            "workflowId": STRING
        }        
        
    """    
    global m_sBaseUrl
    global m_sSessionId
    
    
    headers = {'Content-Type': 'application/json','x-session-token': m_sSessionCookie}
    
    sUrl = m_sBaseUrl + '/processing/getgraphsbyusr'
        
    oResult = requests.get(sUrl, headers=headers)
    
    if (oResult.ok):
        oJsonResults = oResult.json()
        return oJsonResults
    else:
        return None
    
def executeWorkflow(sInputFileName, sOutputFileName, sWorkflowName):
    """
    Executes a WASDI SNAP Workflow
    return the Process Id if every thing is ok
    return '' if there was any problem
    """    
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace
    
    sWorkflowId = ''
    aoWorkflows = getWorkflows()
        
    for oWorkflow in aoWorkflows:
        if (oWorkflow['name'] == sWorkflowName):
            sWorkflowId = oWorkflow['workflowId']
            break
    
    headers = {'Content-Type': 'application/json','x-session-token': m_sSessionCookie}
    payload = {'workspace': m_sActiveWorkspace, 'source': sInputFileName,'destination':sOutputFileName,'workflowId':sWorkflowId}
    
    sUrl = m_sBaseUrl + '/processing/graph_id'
    
    oResult = requests.get(sUrl, headers=headers, params=payload)
    
    sProcessId = ''
    
    if (oResult.ok):
        oJsonResults = oResult.json()
        
        if (oJsonResults['boolValue']):
            sProcessId = oJsonResults['stringValue']
    
    return sProcessId

def getProcessStatus(sProcessId):
    """
    get the status of a Process
    return the status or '' if there was any error
    
    STATUS are  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR
    """    
    global m_sBaseUrl
    global m_sSessionId
    
    headers = {'Content-Type': 'application/json','x-session-token': m_sSessionCookie}
    payload = {'sProcessId': sProcessId}
    
    sUrl = m_sBaseUrl + '/process/byid'
    
    oResult = requests.get(sUrl, headers=headers, params=payload)
    
    sStatus = ''
    
    if (oResult.ok):
        oJsonResult = oResult.json()
        sStatus = oJsonResult['status']
    
    return sStatus

def updateProcessStatus(sProcessId, sStatus, iPerc):
    """
    Update the status of a process
    return the updated status as a String or '' if there was any problem
    """    
    global m_sBaseUrl
    global m_sSessionId
    
    headers = {'Content-Type': 'application/json','x-session-token': m_sSessionCookie}
    payload = {'sProcessId': sProcessId,'status': sStatus,'perc':iPerc}
    
    if (iPerc<0):
        print('iPerc < 0 not valid')
        return ''
    if (iPerc>100):
        print('iPerc > 100 not valid')
        return ''
    if (not(sStatus == 'CREATED' or sStatus == 'RUNNING' or sStatus == 'STOPPED' or sStatus == 'DONE' or sStatus == 'ERROR')):
        print('sStatus must be a string like one of  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR')
        return ''
    
    sUrl = m_sBaseUrl + '/process/updatebyid'
    
    oResult = requests.get(sUrl, headers=headers, params=payload)
    
    sStatus = ''
    
    if (oResult.ok):
        oJsonResult = oResult.json()
        sStatus = oJsonResult['status']
    
    return sStatus

def saveFile(sFileName):
    """
    Ingest a new file in the Active WASDI Workspace.
    The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS 
    To work be sure that the file is on the server
    """    
    global m_sBaseUrl
    global m_sSessionId
    global m_sActiveWorkspace
    
    headers = {'Content-Type': 'application/json','x-session-token': m_sSessionCookie}
    payload = {'file': sFileName,'workspace': m_sActiveWorkspace}
    
    sUrl = m_sBaseUrl + '/catalog/upload/ingestinws'
    
    oResult = requests.get(sUrl, headers=headers, params=payload)
    
    sProcessId = ''
    
    if (oResult.ok):
        oJsonResult = oResult.json()
        if (oJsonResult['boolValue']):
            sProcessId = oJsonResult['stringValue']        
    
    return sProcessId

if __name__ == '__main__':
    print('WASPY - The WASDI Python Library. Include in your code for space development processors. Visit www.wasdi.net')