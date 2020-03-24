;--------------------------------------------------------------------------------------------------------------------------
; WASDI Corporation
; WASDI IDL Lib
; Tested with IDL 8.7.2
; IDL WASDI Lib Version 3.0.0
; Last Update: 
;
; History
; 3.0.0 - 2020-03-23
;   Added support to distributed nodes
;
; 2.0.4 - 2020-01-26
;   Added time stamp to internal log
;
; 2.0.3 - 2020-01-23
;   Added support to WAITING and READY states
;
; 0.1.16
;	Removed debug prints
;
; 0.1.15
;
;	Fixed path generation for shared workspaces
;--------------------------------------------------------------------------------------------------------------------------

PRO STARTWASDI, sConfigFilePath
  ; Define a set of shared variables
  COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner, workspaceurl
  
  IF (sConfigFilePath EQ !NULL) THEN BEGIN
	print, 'Config File null, return'
	RETURN
  END
  
  ; Open the Config File
  openr,lun,sConfigFilePath, /GET_LUN
  
  print, 'Start Wasdi: config file path = ', sConfigFilePath

  ; Initialize Shared Variables
  basepath='/data/wasdi/'
  user = ''
  password = ''
  activeworkspace = ''
  workspaceowner = ''
  workspaceurl = ''
  token = ''
  myprocid = ''
  baseurl='217.182.93.57'
  parametersfilepath='./parameters.txt'
  downloadactive = '1'
  isonserver = '0'
  verbose = '0'
  params = DICTIONARY()

  sConfigFileLine = ''
  
  ; Read the config.properties file
  WHILE NOT EOF(lun) DO BEGIN & $
  
    READF, lun, sConfigFileLine & $
    asKeyValue = STRSPLIT(sConfigFileLine,'=',/EXTRACT)
	
	CASE asKeyValue[0] OF
		'USER': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					user = asKeyValue[1]
				END
			END
		'PASSWORD': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					password = asKeyValue[1]
				END
			END
		'SESSIONID': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					token = asKeyValue[1]
				END
			END
		'WORKSPACE': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					activeworkspace = asKeyValue[1]
				END
			END
		'BASEPATH': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					basepath = asKeyValue[1]
				END
			END
		'MYPROCID': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					myprocid = asKeyValue[1]
				END
			END
		'BASEURL': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					baseurl = asKeyValue[1]
				END
			END
		'PARAMETERSFILEPATH': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					parametersfilepath = asKeyValue[1]
				END
			END
		'DOWNLOADACTIVE': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					downloadactive = asKeyValue[1]
				END
			END
		'ISONSERVER': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					isonserver = asKeyValue[1]
				END
			END
		'VERBOSE': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					verbose = asKeyValue[1]
				END
			END	
		'UPLOADACTIVE': BEGIN
				IF n_elements(asKeyValue) gt 1 THEN BEGIN
					uploadactive = asKeyValue[1]
				END
			END					
		ELSE: print, 'Config.properties invalid row: ', asKeyValue[0]
	END
	
  ENDWHILE
  
  ; Close the config file and free the file unit
  FREE_LUN, lun  
  
  REFRESHPARAMETERS
  
  print, 'call to init Wasdi'
  
  INITWASDI,user,password,basepath,token,myprocid
  
  IF (activeworkspace EQ !NULL) OR (STRLEN(activeworkspace) LE 1) THEN BEGIN
    print, 'Workspace not set'
  END ELSE BEGIN
	WASDIOPENWORKSPACE, activeworkspace
	print, 'Workspace ', activeworkspace, ' opened'
  END 
  
  print, 'Wasdi initialized, welcome to space'
      
END

PRO REFRESHPARAMETERS
	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner 
	
	print, 'Wasdi configuration read, try to read params'

	iExistsParametersFile = FILE_TEST(parametersfilepath) 
  
	IF (iExistsParametersFile EQ 1) THEN BEGIN
  
		sParametersFileLine = ''
	
		print, 'Open parameters file ', parametersfilepath
		openr,lun,parametersfilepath, /GET_LUN
	
		; Read the parameters.txt file
		WHILE NOT EOF(lun) DO BEGIN & $
	
			READF, lun, sParametersFileLine & $
			
			IF (sParametersFileLine NE !NULL) THEN BEGIN
				IF (sParametersFileLine NE '') THEN BEGIN
					asKeyValue = STRSPLIT(sParametersFileLine,'=',/EXTRACT)
				
					IF (n_elements(asKeyValue) GE 2) THEN BEGIN
						params[asKeyValue[0]] = asKeyValue[1]
						
						IF (verbose EQ '1') THEN BEGIN
							print, 'parameter added: key=', asKeyValue[0], ' value=', asKeyValue[1]
						END
					END
					
					IF (n_elements(asKeyValue) EQ 1) THEN BEGIN
						params[asKeyValue[0]] = !NULL
						
						IF (verbose EQ '1') THEN BEGIN
							print, 'parameter added: key=', asKeyValue[0], ' value=!NULL'
						END			
					END
				END
			END
			
		ENDWHILE
	
		; Close the config file and free the file unit
		FREE_LUN, lun  	
	
	END ELSE BEGIN
		IF (verbose EQ '1') THEN BEGIN
			print, 'parameters file ', parametersfilepath, ' not found'
		END
	END
END

; IDL HTTP GET Function Utility
FUNCTION WASDIHTTPGET, sUrlPath, sHostName
	
	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
	
	IF (sUrlPath EQ !NULL) THEN BEGIN
		print, 'Url Path is null, return'
		RETURN, !NULL
	END
  
	IF (token EQ !NULL) OR (STRLEN(token) LE 1) THEN BEGIN
		sessioncookie = ''
	END ELSE BEGIN
		sessioncookie = token
	END 
	
	IF (sHostName EQ !NULL) THEN BEGIN
		sHostName = '217.182.93.57'
	END

	CATCH, iErrorStatus 

	IF iErrorStatus NE 0 THEN BEGIN
		IF (verbose EQ '1') THEN BEGIN
			print, 'WasdiHttpGet ERROR STATUS=', STRTRIM(STRING(iErrorStatus),2), ' returning !NULL'
		END
		
		RETURN, !NULL
	ENDIF

	IF (verbose EQ '1') THEN BEGIN
		print, 'WasdiHttpGet Url ', sUrlPath
	END
  
	; Create a new url object
	oUrl = OBJ_NEW('IDLnetUrl')

	; This is an http transaction
	oUrl->SetProperty, URL_SCHEME = 'http'

	; Use the http server string
	oUrl->SetProperty, URL_HOSTNAME = sHostName

	; name of remote path
	oUrl->SetProperty, URL_PATH = sUrlPath
	oUrl->SetProperty, HEADERS = ['Content-Type: application/json','x-session-token: '+sessioncookie]

	; Call the http url and get result
	serverJSONResult = oUrl->Get( /STRING_ARRAY)
	;print,serverJSONResult

	; Parse the result in JSON
	wasdiResult = JSON_PARSE(serverJSONResult)

	; Close the connection to the remote server, and destroy the object
	oUrl->CloseConnections
	OBJ_DESTROY, oUrl

	; Return the JSON
	RETURN, wasdiResult
END

; IDL HTTP POST UTILITY FUNCTION
FUNCTION WASDIHTTPPOST, sUrlPath, sBody, sHostName

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
	
	IF (sUrlPath EQ !NULL) THEN BEGIN
		print, 'Url Path is null, return'
		RETURN, !NULL
	END
	
	IF (sHostName EQ !NULL) THEN BEGIN
		sHostName = '217.182.93.57'
	END	

	sessioncookie = token

	; Create a new url object
	oUrl = OBJ_NEW('IDLnetUrl')

	; This is an http transaction
	oUrl->SetProperty, URL_SCHEME = 'http'

	; Use the http server string
	oUrl->SetProperty, URL_HOSTNAME = sHostName

	; name of remote path
	oUrl->SetProperty, URL_PATH = sUrlPath
	oUrl->SetProperty, HEADERS = ['Content-Type: application/json','x-session-token: '+sessioncookie]

	IF (verbose EQ '1') THEN BEGIN
		print, 'WasdiHttpPost Url ', sUrlPath
	END  

	; CALL THE HTTP POST URL WITH BODY
	serverJSONResult = oUrl->Put(sBody, /STRING_ARRAY,/POST, /BUFFER)

	; Close the connection to the remote server, and destroy the object
	oUrl->CloseConnections
	OBJ_DESTROY, oUrl

	wasdiResult = ''
	
	IF (STRLEN(serverJSONResult) GT 0) THEN BEGIN
		; PARSE THE JSON RESULT
		wasdiResult = JSON_PARSE(serverJSONResult)		
	END 
	
	RETURN, wasdiResult
END

;Utility method to get value of a key in a ordered hash
FUNCTION GETVALUEBYKEY, jsonResult, sKey
	
	;IF (jsonResult EQ !NULL) THEN BEGIN
	;	RETURN, !NULL
	;END
	
	IF (sKey EQ !NULL) THEN BEGIN
		RETURN, !NULL
	END

	oJSONObject = jsonResult
	aoKeys = oJSONObject.keys()
	aoValues = oJSONObject.values()
	sValue = ""

	FOR j=0,n_elements(aoKeys)-1 DO BEGIN

		IF aoKeys[j] EQ sKey THEN BEGIN
			sValue = aoValues[j]
			break
		ENDIF

	ENDFOR

	RETURN, sValue

END

FUNCTION GETDATESTRINGFROMJULDAY, julDay
	
	IF (julDay EQ !NULL) THEN BEGIN
		print, 'GetDate: input null return'
		RETURN, !NULL
	END

	CALDAT, julDay, Month1, Day1, Year1

	sYear = STRTRIM(STRING(Year1),2)
	sMonth = STRTRIM(STRING(Month1),2)
	sDay = STRTRIM(STRING(Day1),2)
	
	IF (STRLEN(sMonth) EQ 1) THEN BEGIN
		sMonth = '0'+ sMonth
	END 
	
	IF (STRLEN(sDay) EQ 1) THEN BEGIN
		sDay = '0'+ sDay
	END 
	
	sDate = sYear + '-' + sMonth + '-' + sDay
	
	RETURN, sDate
END

FUNCTION WASDITIMESTAMP, format, $
    NO_PREFIX=no_prefix, $
    RANDOM_DIGITS=random_digits, $
    UTC=utc, $
    VALID=valid
	
    On_Error, 2

    ; Set keyword values.
    no_prefix = Keyword_Set(no_prefix)
    IF N_Elements(format) EQ 0 THEN format = 0
    IF N_Elements(random_digits) GT 0  THEN BEGIN
        DEFSYSV, '!FSC_RandomNumbers', EXISTS=exists
        IF exists THEN BEGIN
            randomNumber = !FSC_RandomNumbers -> GetRandomDigits(random_digits)
        ENDIF ELSE BEGIN
            DEFSYSV, '!FSC_RandomNumbers', Obj_New('RandomNumberGenerator')
            randomNumber = !FSC_RandomNumbers -> GetRandomDigits(random_digits)
        ENDELSE
    ENDIF
    
    ; Get some values for the current time.
    time = Systime(UTC=Keyword_Set(utc))
    day = Strmid(time, 0, 3)
    date = String(StrMid(time, 8, 2), Format='(I2.2)') ; Required because UNIX and Windows differ in time format.
    month = Strmid(time, 4, 3)
    year = Strmid(time, 20, 4)
    stamp = Strmid(time, 11, 8)
    months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']
    m = (Where(months EQ StrUpCase(month))) + 1
   
    ; Select the time stamp format.
    CASE format OF
        1:  timeStamp = day + '_' +month + '_' + date + '_' + stamp + '_' + year
        2:  timestamp = date + '_' + String(m, FORMAT='(I2.2)') + '_' + year + '_' + stamp
        3:  timestamp = date + String(m, FORMAT='(I2.2)') + year + '_' + stamp
        4:  timestamp = date + String(m, FORMAT='(I2.2)') + year
        5:  timestamp = String(m, FORMAT='(I2.2)') + '_' + date + '_' + year + '_' + stamp
        6:  timestamp = String(m, FORMAT='(I2.2)') + date + year + '_' + stamp        
        7: timestamp = String(m, FORMAT='(I2.2)') + date + year
        8: timestamp = year + String(m, FORMAT='(I2.2)') + date + '@' + stamp
        ELSE: timeStamp = StrLowCase(day) + '_' + StrLowCase(month) + '_' + date + '_' + stamp + '_' + year
    ENDCASE

    ; Add an first-letter underscore, unless the user explicitly asked not to.
    IF ~no_prefix THEN BEGIN 
       timestamp = '_' + timestamp
    ENDIF

    ; Convert to a valid string, if required.
    IF Keyword_Set(valid) THEN timestamp = IDL_Validname(timeStamp, /CONVERT_ALL)
    
    ; Add the random numbers, if required.
    IF N_Elements(randomNumber) NE 0 THEN BEGIN
        timeStamp = timeStamp + '_' + randomNumber
    ENDIF
	
	; Replace : with _
	timeStamp = STRJOIN(STRSPLIT(timeStamp,':', /EXTRACT), '_')
    
    Return, timeStamp
    
END

; Method Used to login in wasdi. Do not need to call, is called in the Init
FUNCTION WASDILOGIN,wuser,wpassword

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

	; Path of login API
	UrlPath = '/wasdiwebserver/rest/auth/login'
	; Create body json
	LoginString='{  "userId":"'+wuser+'",  "userPassword":"'+wpassword+'"}'

	; Send post request
	serverJSONResult = WASDIHTTPPOST(UrlPath, LoginString, !NULL)

	; get back the session key
	sessionCookie = GETVALUEBYKEY(serverJSONResult, "sessionId")

	RETURN, sessionCookie
END

; Method Used to login in wasdi with the User Session
FUNCTION WASDICHECKSESSION,sessionId

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
	
	IF (sessionId EQ !NULL) THEN BEGIN
		RETURN, !NULL
	END

	; Path of Check Session API
	UrlPath = '/wasdiwebserver/rest/auth/checksession'

	; Send post request
	serverJSONResult = WASDIHTTPGET(UrlPath, !NULL)
	
	sUser = GETVALUEBYKEY(serverJSONResult, 'userId')
	
	IF (sUser EQ user) THEN BEGIN
		RETURN, sessionId	
	END ELSE BEGIN
		RETURN, !NULL
	END
END

;Init WASDL Library
PRO INITWASDI,sUser,sPassword,sBasePath,sSessionId,sMyProdId

	; Define a set of shared variables
	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

	basepath = sBasePath
	user = sUser
	password = sPassword
	myprocid=sMyProdId
	token = sSessionId

	IF (sSessionId EQ !NULL) OR (STRLEN(sSessionId) LE 1) THEN BEGIN
		print, 'InitWasdi: login with user and password'
		sessioncookie = WASDILOGIN(sUser,sPassword)
		token = sessioncookie
		print, 'User Logged in'
	END ELSE BEGIN
		print, 'InitWasdi: login using session id'
		
		sSessionCheck = WASDICHECKSESSION(sSessionId)
		
		IF (sSessionCheck EQ !NULL) THEN BEGIN
			print, 'Session [', sSessionId, '] NOT VALID User NOT LOGGED'
			token = ''
		END ELSE IF (N_ELEMENTS(sSessionCheck) EQ 0) THEN BEGIN
			print, 'Session NOT VALID, User NOT LOGGED'
			token = ''
		END ELSE BEGIN
			token = sSessionId
		END
		
	END
END


; Hello WASDI
PRO HELLOWASDI
	wasdiResult = WASDIHTTPGET('/wasdiwebserver/rest/wasdi/hello',!NULL)
END

; Get the status of a WASDI Process
FUNCTION WASDIGETPROCESSSTATUS, sProcessID

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

	; API URL
	UrlPath = '/wasdiwebserver/rest/process/byid?sProcessId='+sProcessID

	; Call get status
	wasdiResult = WASDIHTTPGET(UrlPath,!NULL)

	; read response JSON.
	sStatus = GETVALUEBYKEY(wasdiResult, 'status')

	; Status will be one of CREATED,  RUNNING,  STOPPED,  DONE,  ERROR
	RETURN, sStatus
END

FUNCTION WASDIWAITPROCESS, sProcessID

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

	sStatus=' '
	
	print, 'Waiting for scheduled process to finish'
	;WASDIUPDATEPROCESSSTATUS(myprocid, 'WAITING', -1)
	
	iTotalTime = 0
	WHILE sStatus ne 'DONE' and sStatus ne 'STOPPED' and sStatus ne 'ERROR' DO BEGIN
		sStatus = WASDIGETPROCESSSTATUS(sProcessID)
		WAIT, 2
		iTotalTime = iTotalTime + 2
	ENDWHILE
	
	print, 'Process Done (took ', STRTRIM(STRING(iTotalTime),2), ' seconds)'
	
	;WASDIUPDATEPROCESSSTATUS(myprocid, 'READY', -1)

	RETURN, sStatus
END


FUNCTION WASDIWAITPROCESSES, asProcessIDs

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
	
	sStatus=''
	
	print, 'Waiting for ', STRTRIM(STRING(N_ELEMENTS(asProcessIDs)),2), ' processes to finish'
	;WASDIUPDATEPROCESSSTATUS(myprocid, 'WAITING', -1)
	
	asProcessToCeck = [asProcessIDs]
	asNewList = []
	
	iProcessCount = N_ELEMENTS(asProcessToCeck)
	
	iTotalTime = 0
	
	WHILE iProcessCount GT 0 DO BEGIN
	
		FOR iProcesses=0, iProcessCount -1 DO BEGIN
		
			sProcessID = asProcessToCeck[iProcesses]
			sStatus = WASDIGETPROCESSSTATUS(sProcessID)
			
			IF (sStatus EQ 'DONE') OR (sStatus EQ 'STOPPED') OR (sStatus EQ 'ERROR') THEN BEGIN
				print, 'Process ', sProcessID, ' finished with status ', sStatus
			END ELSE BEGIN
				asNewList = [asNewList, sProcessID]
			END
			
		ENDFOR
		
		asProcessToCeck = [asNewList]
		asNewList = []
		iProcessCount = N_ELEMENTS(asProcessToCeck)
		
		WAIT, 2
		
		iTotalTime = iTotalTime + 2
		
	ENDWHILE 
	
	; Read again the status to give back the ordered result list
	
	iProcessCount = N_ELEMENTS(asProcessIDs)
	
	asResults = []
	
	FOR iProcesses=0, iProcessCount -1 DO BEGIN
	
		sProcessID = asProcessIDs[iProcesses]
		sStatus = WASDIGETPROCESSSTATUS(sProcessID)
		
		asResults = [asResults, sStatus]
		
	ENDFOR
	
	print, 'All Processes Are Done (took approx ', STRTRIM(STRING(iTotalTime),2), '  seconds)'
	;WASDIUPDATEPROCESSSTATUS(myprocid, 'READY', -1)

	RETURN, asResults
END

; Get list of workspace of the user
FUNCTION WASDIGETWORKSPACES

	; API URL
	UrlPath = '/wasdiwebserver/rest/ws/byuser'  

	RETURN, WASDIHTTPGET(UrlPath, !NULL)
END


; converts a ws name in a ws id. For internal use
; Changes the workpsaceowner shared variable
FUNCTION WASDIGETWORKSPACEIDBYNAME, workspacename

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

	workspaceId = "";

	; API URL
	UrlPath = '/wasdiwebserver/rest/ws/byuser'

	; Get the list of users workpsaces
	wasdiResult = WASDIHTTPGET(UrlPath, !NULL)

	; Search the Workspace with the desired name
	FOR i=0,n_elements(wasdiResult)-1 DO BEGIN

		oWorkspace = wasdiResult[i]

		; Check the name property
		sName = GETVALUEBYKEY(oWorkspace, 'workspaceName')

		IF sName EQ workspaceName THEN BEGIN
			; found it
			sId = GETVALUEBYKEY(oWorkspace, 'workspaceId')
			workspaceId = sId
			BREAK
		ENDIF
	ENDFOR

	IF (workspaceId EQ '') THEN BEGIN
		print, 'WASDIGETWORKSPACEIDBYNAME Workspace ', workspacename, ' NOT FOUND'
	END

	; return the found id or ""
	RETURN, workspaceId
  
END

; Get the URL of a Workspace
FUNCTION WASDIGETWORKSPACEURLBYWSID, workspaceid

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner, workspaceurl

	ownerUserId = "";
	
	; API URL
	UrlPath = '/wasdiwebserver/rest/ws?sWorkspaceId=' + workspaceid

	; Get the workpsace view model
	wasdiResult = WASDIHTTPGET(UrlPath, !NULL)
	
	; Catch the possible null excetpion
	CATCH, iErrorStatus 

	IF iErrorStatus NE 0 THEN BEGIN
		RETURN, ''
	ENDIF	

	; Check the name property
	sWsUrl = GETVALUEBYKEY(wasdiResult, 'apiUrl')
	
	asURLValues = STRSPLIT(sWsUrl,'/',/EXTRACT)	
	
	sIpAddress = ''
	
	IF (N_ELEMENTS(asURLValues) GT 0) THEN BEGIN
		sIpAddress = asURLValues[1]
	ENDIF

	; return the found address or ""
	RETURN, sIpAddress
  
END


; Get the owner of a Workspace
FUNCTION WASDIGETWORKSPACEOWNERBYWSID, workspaceid

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

	ownerUserId = "";
	
	; API URL
	UrlPath = '/wasdiwebserver/rest/ws/byuser'

	; Get the list of users workpsaces
	wasdiResult = WASDIHTTPGET(UrlPath, !NULL)

	; Search the Workspace with the desired name
	FOR i=0,n_elements(wasdiResult)-1 DO BEGIN

		oWorkspace = wasdiResult[i]

		; Check the name property
		sId = GETVALUEBYKEY(oWorkspace, 'workspaceId')

		IF sId EQ workspaceid THEN BEGIN
			; found it
			ownerUserId = GETVALUEBYKEY(oWorkspace, 'ownerUserId')
			BREAK
		ENDIF
	ENDFOR

	IF (ownerUserId EQ '') THEN BEGIN
		print, 'WASDIGETWORKSPACEOWNERBYWSID Workspace ', workspaceid, ' NOT FOUND'
	END

	; return the found id or ""
	RETURN, ownerUserId
  
END


; Check if a product exists already. returns 1 if exists, 0 if not
FUNCTION WASDICHECKPRODUCTEXISTS, filename

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

	workspaceId = "";

	; API URL
	UrlPath = '/wasdiwebserver/rest/product/byname?sProductName='+filename+'&workspace='+activeworkspace

	; Get the list of users workpsaces
	wasdiResult = WASDIHTTPGET(UrlPath, !NULL)

	iReturn = 0

	; Catch the possible null excetpion
	CATCH, iErrorStatus 

	IF iErrorStatus NE 0 THEN BEGIN
		RETURN, iReturn
	ENDIF

	; try to check the file name
	sCheckFileName = GETVALUEBYKEY(wasdiResult, 'fileName')

	IF (filename EQ sCheckFileName) THEN BEGIN
		iReturn = 1
	END

	; return the flag
	RETURN, iReturn

END

; Check if a product exists already. returns 1 if exists, 0 if not
FUNCTION WASDIGETPRODUCTBBOX, filename

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

	workspaceId = "";

	; API URL
	UrlPath = '/wasdiwebserver/rest/product/byname?sProductName='+filename+'&workspace='+activeworkspace

	; Get the list of users workpsaces
	wasdiResult = WASDIHTTPGET(UrlPath, !NULL)

	; Catch the possible null excetpion
	CATCH, iErrorStatus 

	IF iErrorStatus NE 0 THEN BEGIN
		RETURN, !NULL
	ENDIF

	; try to check the file name
	sBbox = GETVALUEBYKEY(wasdiResult, 'bbox')

	; return the Bounding Box
	RETURN, sBbox

END

; Delete product
FUNCTION WASDIDELETEPRODUCT, filename

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner, workspaceurl

	workspaceId = activeworkspace;

	; API URL
	UrlPath = '/wasdiwebserver/rest/product/delete?sProductName='+filename+'&bDeleteFile=true&sWorkspaceId='+workspaceId+'&bDeleteLayer=true'

	; Get the list of users workpsaces
	wasdiResult = WASDIHTTPGET(UrlPath, workspaceurl)

	; The API does not return a text
	CATCH, iErrorStatus 

	IF iErrorStatus NE 0 THEN BEGIN
		RETURN, 1
	ENDIF

	; return the flag
	RETURN, 1

END


; Open a  Workspace by name
pro WASDIOPENWORKSPACE,workspacename

  COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner, workspaceurl
  ; Set active Workspace and owner
  activeworkspace = WASDIGETWORKSPACEIDBYNAME(workspacename)
  workspaceowner = WASDIGETWORKSPACEOWNERBYWSID(activeworkspace)
  workspaceurl = WASDIGETWORKSPACEURLBYWSID(activeworkspace)
  ;print, workspaceurl
  
  
END

; Get the list of products in a WS. Takes the name in input gives in output an array of string with on element for each file
FUNCTION WASDIGETPRODUCTSBYWORKSPACE,workspacename

  COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
  
  WASDIOPENWORKSPACE, workspacename
  
  workspaceid = activeworkspace

  ; API url
  UrlPath = '/wasdiwebserver/rest/product/byws?sWorkspaceId='+workspaceid

  ; Get the list of products
  wasdiResult = WASDIHTTPGET(UrlPath, !NULL)
  
  ; Create the output array
  asProductsNames = []

  ; Convert JSON in a String Array
  FOR i=0,n_elements(wasdiResult)-1 DO BEGIN

    oProduct = wasdiResult[i]
    sFileName = GETVALUEBYKEY(oProduct, 'fileName')
    asProductsNames=[asProductsNames,sFileName]

  ENDFOR

  ; Return the array
  RETURN, asProductsNames
END

; Get the list of products in a WS. Takes the name in input gives in output an array of string with on element for each file
FUNCTION WASDIGETACTIVEWORKSPACEPRODUCTS

  COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

  ; API url
  UrlPath = '/wasdiwebserver/rest/product/namesbyws?sWorkspaceId='+activeworkspace

  ; Get the list of products
  wasdiResult = WASDIHTTPGET(UrlPath, !NULL)
  
  iResults = n_elements(wasdiResult)
  
  IF iResults GT 0 THEN BEGIN  
	  ; Create the output array
	  asProductsNames = STRARR(iResults)

	  ; Convert JSON in a String Array
	  FOR i=0,iResults-1 DO BEGIN

		;oProduct = wasdiResult[i]
		;sFileName = GETVALUEBYKEY(oProduct, 'fileName')
		;sFileName = oProduct['fileName']
		
		;asProductsNames[i]=sFileName
		asProductsNames[i]=wasdiResult[i]

	  ENDFOR
  END ELSE BEGIN
	asProductsNames = []
  END
  
  ; Return the array
  RETURN, asProductsNames
   
END

; Obtain the local full path of a EO File
FUNCTION WASDIGETFULLPRODUCTPATH, sProductName

  COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

  ; be sure to end the base path with /
  IF (not(  (basepath.charAt(strlen(basepath)-1) EQ '\') OR (basepath.charAt(strlen(basepath)-1) EQ '/'))) THEN BEGIN
    basepath = basepath + '/'
  END

  ; compose the full path
  ;sFullPath = basepath + user +'/' + activeworkspace + '/' +  sProductName
  sFullPath = basepath + workspaceowner +'/' + activeworkspace + '/' +  sProductName
  
  
  IF (isonserver EQ '0') THEN BEGIN
	IF (downloadactive EQ '1') THEN BEGIN
		result = FILE_TEST(sFullPath)
		
		IF (result NE '1') THEN BEGIN
			sOnlyPath = basepath + user +'/' + activeworkspace
			FILE_MKDIR, sOnlyPath
			print, 'WASDI File not present in local PC. Starting Autodownload'
			WASDIDOWNLOADFILE, sProductName, sFullPath
			print, 'WASDI File Downloaded'
		END
	END
  END

  RETURN, sFullPath
END

; Donwloads a File from WASDI
PRO WASDIDOWNLOADFILE, sProductName, sFullPath

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner, workspaceurl

	IF (token EQ !NULL) OR (STRLEN(token) LE 1) THEN BEGIN
		sessioncookie = ''
	END ELSE BEGIN
		sessioncookie = token
	END 

	sUrlPath = 'wasdiwebserver/rest/catalog/downloadbyname?filename='+sProductName+'&workspace='+activeworkspace

	IF (verbose EQ '1') THEN BEGIN
		print, 'WASDIDOWNLOADFILE Url ', sUrlPath
	END
	
	sUrlAddress = workspaceurl
	
	IF (sUrlAddress EQ !NULL) THEN BEGIN
		sUrlAddress = '217.182.93.57'
	ENDIF

	; Create a new url object
	oUrl = OBJ_NEW('IDLnetUrl')

	; This is an http transaction
	oUrl->SetProperty, URL_SCHEME = 'http'

	; Use the http server string
	oUrl->SetProperty, URL_HOSTNAME = sUrlAddress

	; name of remote path
	oUrl->SetProperty, URL_PATH = sUrlPath
	oUrl->SetProperty, HEADERS = ['Content-Type: application/json','x-session-token: '+sessioncookie]

	; Call the http url and get result
	sReceivedPath = oUrl->Get( FILENAME = sFullPath)

	print, 'WASDIDOWNLOADFILE Output Path = ', sReceivedPath

	; Close the connection to the remote server, and destroy the object
	oUrl->CloseConnections
	OBJ_DESTROY, oUrl
    
END

; Get the base path to use to save generated files
pro WASDIGETSAVEPATH, sFullPath

  COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

  ; be sure to end the base path with /
  IF (not(  (basepath.charAt(strlen(basepath)-1) eq '\') or (basepath.charAt(strlen(basepath)-1) eq '/'))) THEN BEGIN
    basepath = basepath + '/'
  ENDIF

  ; compose the full path
  ;sFullPath = basepath + user +'/' + activeworkspace + '/'
  sFullPath = basepath + workspaceowner +'/' + activeworkspace + '/'

END

; Get the list of available workflows
FUNCTION WASDIGETWORKFLOWS

  COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

  ; API url
  UrlPath = '/wasdiwebserver/rest/processing/getgraphsbyusr'

  ; Call API
  wasdiResult = WASDIHTTPGET(UrlPath, !NULL)

  RETURN, wasdiResult
END


; Execute a SNAP xml Workflow in WASDI
FUNCTION WASDIINTERNALEXECUTEWORKFLOW, asInputFileNames, asOutputFileNames, sWorkflow, iAsynch

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
	sessioncookie = token

	;get the list of workflows
	aoWorkflows = WASDIGETWORKFLOWS()
	
	sWorkflowId = !NULL

	; Search the named one
	FOR i=0,n_elements(aoWorkflows)-1 DO BEGIN

		oWorkflow = aoWorkflows[i]
		sWfName = GETVALUEBYKEY(oWorkflow, 'name')

		IF sWfName EQ sWorkflow THEN BEGIN
			sWfId = GETVALUEBYKEY(oWorkflow, 'workflowId')
			sWorkflowId = sWfId
			BREAK;
		ENDIF
	ENDFOR
	
	IF (sWorkflowId EQ !NULL) THEN BEGIN
		print, 'Workflow ', sWorkflow, ' not found'
		RETURN, 'ERROR'
	END

	; API url
	UrlPath = '/wasdiwebserver/rest/processing/graph_id?workspace='+activeworkspace

	; Generate input file names JSON array
	sInputFilesJSON = '['

	; For each input name
	FOR i=0,n_elements(asInputFileNames)-1 DO BEGIN

		sInputName = asInputFileNames[i]
		; wrap with '
		sInputFilesJSON = sInputFilesJSON + '"' + sInputName + '"'

		; check of is not the last one
		IF i LT n_elements(asInputFileNames)-1 THEN BEGIN
			; add ,
			sInputFilesJSON = sInputFilesJSON + ','
		ENDIF
	ENDFOR

	; close the array
	sInputFilesJSON = sInputFilesJSON + ']'

	;print, 'Input Files JSON ', sInputFilesJSON

	; Create the output file names array
	sOutputFilesJSON = '['

	; For each output name
	FOR i=0,n_elements(asOutputFileNames)-1 DO BEGIN

		sOutputName = asOutputFileNames[i]
		; wrap with '
		sOutputFilesJSON = sOutputFilesJSON + '"' + sOutputName + '"'

		; check of is not the last one
		IF i LT n_elements(asOutputFileNames)-1 then BEGIN
			; add , for the next one
			sOutputFilesJSON = sOutputFilesJSON + ','
		ENDIF

	ENDFOR

	; close the array
	sOutputFilesJSON = sOutputFilesJSON + ']'

	;print, 'Output File JSON ' + sOutputFilesJSON

	; compose the full execute workflow JSON View Model
	sWorkFlowViewModelString='{  "workflowId":"'+sWorkflowId+'",  "name":"'+sWfName +'",  "inputFileNames":'+sInputFilesJSON +',  "outputFileNames":'+sOutputFilesJSON+'}'

	IF (verbose eq '1') THEN BEGIN
		print, 'Workflow JSON ' , sWorkFlowViewModelString
	END

	wasdiResult = WASDIHTTPPOST(UrlPath, sWorkFlowViewModelString, !NULL)

	sResponse = GETVALUEBYKEY(wasdiResult, 'boolValue')

	sProcessID = ''

	; get the process id
	IF sResponse then BEGIN
		sValue = GETVALUEBYKEY(wasdiResult, 'stringValue')
		sProcessID=sValue
	ENDIF

	IF (iAsynch EQ 0) THEN BEGIN
		sStatus = "ERROR"

		; Wait for the process to finish
		IF sProcessID NE '' THEN BEGIN
			sStatus = WASDIWAITPROCESS(sProcessID)
		ENDIF
	END ELSE BEGIN
		; Return the 
		sStatus = sProcessID
	END

	RETURN, sStatus
END

; Execute a SNAP xml Workflow in WASDI without waiting the process to finish
FUNCTION WASDIASYNCHEXECUTEWORKFLOW, asInputFileNames, asOutputFileNames, sWorkflow
	RETURN, WASDIINTERNALEXECUTEWORKFLOW(asInputFileNames, asOutputFileNames, sWorkflow, 1)
END

; Execute a SNAP xml Workflow in WASDI
FUNCTION WASDIEXECUTEWORKFLOW, asInputFileNames, asOutputFileNames, sWorkflow
	RETURN, WASDIINTERNALEXECUTEWORKFLOW(asInputFileNames, asOutputFileNames, sWorkflow, 0)
END



; Execute a WASDI PROCESSOR
FUNCTION WASDIASYNCHEXECUTEPROCESSOR, sProcessorName, aoParameters

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
	sessioncookie = token

	; API url
	UrlPath = '/wasdiwebserver/rest/processors/run?workspace='+activeworkspace+'&name='+sProcessorName+'&encodedJson='

	; Generate input file names JSON array
	sParamsJSON = '{'

	; For each input name
	FOREACH sKey , aoParameters.Keys() DO BEGIN

		sParamsJSON = sParamsJSON + '"' + sKey + '":'
		
		sValue = aoParameters[sKey]
		
		IF (sValue NE !NULL) THEN BEGIN
			sParamsJSON = sParamsJSON + '"' + sValue + '" , '
		END ELSE BEGIN
			sParamsJSON = sParamsJSON + '"" , '
		END
		
	END

	sParamsJSON = STRMID(sParamsJSON, 0, STRLEN(sParamsJSON)-2)
	sParamsJSON = sParamsJSON + '}'
	
	IF (verbose EQ 1) THEN BEGIN
		print, 'Parameter JSON ', sParamsJSON
	END
	
	;Create a new url object
	oUrl = OBJ_NEW('IDLnetUrl')
	sEncodedParametersJSON = oUrl->URLEncode(sParamsJSON)

	UrlPath = UrlPath + sEncodedParametersJSON

	wasdiResult = WASDIHTTPGET(UrlPath, !NULL)

	sProcessID = GETVALUEBYKEY(wasdiResult, 'processingIdentifier')
	
	RETURN, sProcessID
END

; Create a Mosaic from a list of input images
FUNCTION WASDIMOSAIC, asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue

  COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
  sessioncookie = token


  ; API url
  UrlPath = '/wasdiwebserver/rest/processing/geometric/mosaic?sDestinationProductName='+sOutputFile+"&sWorkspaceId="+activeworkspace
 
  ; Generate input file names JSON array
  sInputFilesJSON = '['
 
  ; For each input name
  FOR i=0,n_elements(asInputFileNames)-1 DO BEGIN
   
    sInputName = asInputFileNames[i]
	; wrap with '
	sInputFilesJSON = sInputFilesJSON + '"' + sInputName + '"'
	
	; check of is not the last one
   IF i lt n_elements(asInputFileNames)-1 then BEGIN
	  ; add ,
      sInputFilesJSON = sInputFilesJSON + ','
    ENDIF
  ENDFOR
 
  ; close the array
  sInputFilesJSON = sInputFilesJSON + ']'
 
  IF (verbose eq '1') THEN BEGIN
	print, 'Input Files JSON ', sInputFilesJSON
  END
 
  sOutputFormat='GeoTIFF'
  IF (STRMATCH(sOutputFile, '*.tif', /FOLD_CASE) EQ 1) THEN BEGIN
	sOutputFormat='GeoTIFF'
  END ELSE IF (STRMATCH(sOutputFile, '*.dim', /FOLD_CASE) EQ 1)  THEN BEGIN
	sOutputFormat='BEAM-DIMAP'
  END
 
  ; compose the full MosaicSetting JSON View Model
  sMosaicSettingsString='{  "crs": "GEOGCS[\"WGS84(DD)\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\", 0.017453292519943295],  AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH]]",  "southBound": -1.0,"eastBound": -1.0, "westBound": -1.0, "northBound": -1.0, "pixelSizeX": -1.0, "pixelSizeY":  -1.0, "overlappingMethod": "MOSAIC_TYPE_OVERLAY", "showSourceProducts": false, "elevationModelName": "ASTER 1sec GDEM", "resamplingName": "Nearest", "updateMode": false, "nativeResolution": true, "combine": "OR", "sources":'+sInputFilesJSON +', "variableNames": [], "variableExpressions": [], "outputFormat":"' + sOutputFormat + '"'
  
  if (sNoDataValue NE !NULL) THEN BEGIN
	sMosaicSettingsString = sMosaicSettingsString + ', "noDataValue":' + sNoDataValue
  END
  
  if (sInputIgnoreValue NE !NULL) THEN BEGIN
	sMosaicSettingsString = sMosaicSettingsString + ', "inputIgnoreValue":' + sInputIgnoreValue
  END  
  
  sMosaicSettingsString = sMosaicSettingsString + ' }'
 
  IF (verbose eq '1') THEN BEGIN
	print, 'MOSAIC SETTINGS JSON ' , sMosaicSettingsString
	print, 'URL: ', UrlPath
  END

  wasdiResult = WASDIHTTPPOST(UrlPath, sMosaicSettingsString, !NULL)
 
  sResponse = GETVALUEBYKEY(wasdiResult, 'boolValue')
 
  sProcessID = ''
 
  ; get the process id
  IF sResponse then BEGIN
    sValue = GETVALUEBYKEY(wasdiResult, 'stringValue')
    sProcessID=sValue
  ENDIF
 
  sStatus = "ERROR"
 
  ; Wait for the process to finish
  IF sProcessID ne '' then BEGIN
    sStatus = WASDIWAITPROCESS(sProcessID)
  ENDIF
 
  RETURN, sStatus
END


; Create a Subset from an image
FUNCTION WASDISUBSET, sInputFile, sOutputFile, sLatN, sLonW, sLatS, sLonE

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
	sessioncookie = token


	; API url
	UrlPath = '/wasdiwebserver/rest/processing/geometric/subset?sSourceProductName='+sInputFile+'&sDestinationProductName='+sOutputFile+"&sWorkspaceId="+activeworkspace

	; compose the full SubsetSetting JSON View Model
	sSubsetSettingsString='{  "latN": '+ sLatN +',  "lonW": '+ sLonW +',"latS": '+ sLatS +', "lonE": '+ sLonE +'}'

	IF (verbose eq '1') THEN BEGIN
		print, 'SUBSET SETTINGS JSON ' , sSubsetSettingsString
		print, 'URL: ', UrlPath
	END

	wasdiResult = WASDIHTTPPOST(UrlPath, sSubsetSettingsString, !NULL)

	sResponse = GETVALUEBYKEY(wasdiResult, 'boolValue')

	sProcessID = ''

	; get the process id
	IF sResponse THEN BEGIN
		sValue = GETVALUEBYKEY(wasdiResult, 'stringValue')
		sProcessID=sValue
	ENDIF

	sStatus = "ERROR"

	; Wait for the process to finish
	IF sProcessID NE '' THEN BEGIN
		sStatus = WASDIWAITPROCESS(sProcessID)
	ENDIF

	RETURN, sStatus
END

FUNCTION WASDIGENERATEJSONARRAY, asArray

	; Generate output JSON array
	sOutputJSON = '['

	; For each input name
	FOR iElements=0,N_ELEMENTS(asArray)-1 DO BEGIN

		sElement = asArray[iElements]
		; wrap with '
		sOutputJSON = sOutputJSON + '"' + sElement + '"'

		; check of is not the last one
		IF iElements LT N_ELEMENTS(asArray)-1 THEN BEGIN
			; add ,
			sOutputJSON = sOutputJSON + ','
		ENDIF
	ENDFOR
	
	sOutputJSON = sOutputJSON + ']'
	
	RETURN, sOutputJSON
END


; Create a Subset from an image
FUNCTION WASDIMULTISUBSET, sInputFile, asOutputFile, asLatN, asLonW, asLatS, asLonE

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
	sessioncookie = token


	; API url
	UrlPath = '/wasdiwebserver/rest/processing/geometric/multisubset?sSourceProductName='+sInputFile+'&sDestinationProductName='+sInputFile+"&sWorkspaceId="+activeworkspace
	
	sOutputFilesJSON = WASDIGENERATEJSONARRAY(asOutputFile)
	
	sLatNJSON = WASDIGENERATEJSONARRAY(asLatN)
	sLonWJSON = WASDIGENERATEJSONARRAY(asLonW)
	sLatSJSON = WASDIGENERATEJSONARRAY(asLatS)
	sLonEJSON = WASDIGENERATEJSONARRAY(asLonE)

	
	; compose the full SubsetSetting JSON View Model
	sSubsetSettingsString='{ "outputNames": ' + sOutputFilesJSON + ', "latNList": '+ sLatNJSON +',  "lonWList": '+ sLonWJSON +',"latSList": '+ sLatSJSON +', "lonEList": '+ sLonEJSON +'}'

	IF (verbose eq '1') THEN BEGIN
		print, 'SUBSET MULTI SETTINGS JSON ' , sSubsetSettingsString
		print, 'URL: ', UrlPath
	END

	wasdiResult = WASDIHTTPPOST(UrlPath, sSubsetSettingsString, !NULL)

	sResponse = GETVALUEBYKEY(wasdiResult, 'boolValue')

	sProcessID = ''

	; get the process id
	IF sResponse THEN BEGIN
		sValue = GETVALUEBYKEY(wasdiResult, 'stringValue')
		sProcessID=sValue
	ENDIF

	sStatus = "ERROR"

	; Wait for the process to finish
	IF sProcessID NE '' THEN BEGIN
		sStatus = WASDIWAITPROCESS(sProcessID)
	ENDIF

	RETURN, sStatus
END


; Search Sentinel EO Images
;
; @param sPlatform Satellite Platform. Accepts "S1","S2"
; @param sDateFrom Starting date in format "YYYY-MM-DD"
; @param sDateTo End date in format "YYYY-MM-DD"
; @param dULLat Upper Left Lat Coordinate. Can be null.
; @param dULLon Upper Left Lon Coordinate. Can be null.
; @param dLRLat Lower Right Lat Coordinate. Can be null.
; @param dLRLon Lower Right Lon Coordinate. Can be null.
; @param sProductType Product Type. If Platform = "S1" -> Accepts "SLC","GRD", "OCN". If Platform = "S2" -> Accepts "S2MSI1C","S2MSI2Ap","S2MSI2A". Can be null.
; @param iOrbitNumber Sentinel Orbit Number. Can be null.
; @param sSensorOperationalMode Sensor Operational Mode. ONLY for S1. Accepts -> "SM", "IW", "EW", "WV". Can be null. Ignored for Platform "S1"
; @param sCloudCoverage Cloud Coverage. Sample syntax: [0 
; @return List of the available products as a LIST of Dictionary representing JSON Object:
; {
; 		footprint = <image footprint in WKT>
; 		id = <unique id of the product for the proviveder>
; 		link = <direct link for download>
; 		provider = <WASDI provider used for search>
; 		Size = <Product Size>
; 		title = <Name of the Product>
; 		properties = < Another JSON Object containing other product-specific info >
; }
;
FUNCTION WASDISEARCHEOIMAGE, sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat, dLRLon, sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage 

  COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
  sessioncookie = token

  ; API url
  UrlPath = '/wasdiwebserver/rest/search/querylist?'
  
  sQuery = "( platformname:";
  
  IF (sPlatform eq 'S2') THEN BEGIN
	 sQuery = sQuery + "Sentinel-2 "
  END ELSE BEGIN
	 sQuery = sQuery + "Sentinel-1"
  END

  IF (sProductType NE !NULL) THEN BEGIN
	 sQuery = sQuery + " AND producttype:" + sProductType
  END
  
  IF (sSensorOperationalMode NE !NULL) THEN BEGIN
	 sQuery = sQuery + " AND sensoroperationalmode:" + sSensorOperationalMode
  END
  
  IF (sCloudCoverage NE !NULL) THEN BEGIN
	 sQuery = sQuery + " AND cloudcoverpercentage:" + sCloudCoverage
  END  
		
  ; TODO: CloudCoverage for S2
  
  ;If available add orbit number
  IF (iOrbitNumber NE !NULL) THEN BEGIN
	 sQuery = sQuery + " AND relativeorbitnumber:" + iOrbitNumber
  END
  
  ;Close the first block
  sQuery = sQuery + ") "
  
  ;Date Block
  sQuery = sQuery + "AND ( beginPosition:[" + sDateFrom + "T00:00:00.000Z TO " + sDateTo + "T23:59:59.999Z]"
  sQuery = sQuery + "AND endPosition:[" + sDateFrom + "T00:00:00.000Z TO " + sDateTo + "T23:59:59.999Z]"
  
  ;Close the second block
  sQuery = sQuery + ") "
  
    
  IF ((dULLat NE !NULL) AND  (dULLon NE !NULL) AND (dLRLat NE !NULL) AND (dLRLon NE !NULL) ) THEN BEGIN
	sFootPrint = '( footprint:"intersects(POLYGON(( ' + dULLon + " " +dLRLat + "," + dULLon + " " + dULLat + "," + dLRLon + " " + dULLat + "," + dLRLon + " " + dLRLat + "," + dULLon + " " +dLRLat + ')))") AND ';
	
	sQuery = sFootPrint + sQuery;
  END

   ; Replace " with \"
  sEscapedQuery = STRJOIN(STRSPLIT(sQuery,'"', /EXTRACT), '\"')
  
  sQueryBody = '["' + sEscapedQuery + '"]'; 
  
  ; Create a new url object
  oUrl = OBJ_NEW('IDLnetUrl')
  sEncodedQuery = oUrl->URLEncode(sQuery)

  sQuery = "sQuery=" + sEncodedQuery + "&offset=0&limit=10&providers=ONDA";
  
  UrlPath = UrlPath + '?' + sQuery
    
  IF (verbose eq '1') THEN BEGIN
	print, 'SEARCH BODY  ' , sQueryBody
	print, 'SEARCH URL  ' , UrlPath
  END

  wasdiResult = WASDIHTTPPOST(UrlPath, sQueryBody, !NULL)
  
  RETURN, wasdiResult
END

; Return the name of a product having in input the result dictionary obtained by SEARCHEOIMAGE
FUNCTION GETFOUNDPRODUCTNAME, oFoundProduct
	sName = GETVALUEBYKEY(oFoundProduct,'title')
	sName = sName + '.zip'
	RETURN, sName
END

; Return the name of a product having in input the result dictionary obtained by SEARCHEOIMAGE
FUNCTION GETFOUNDPRODUCTLINK, oFoundProduct
	RETURN, GETVALUEBYKEY(oFoundProduct,'link')
END


; Import EO Image in WASDI
FUNCTION WASDIIMPORTEOIMAGE, oEOImage

  COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
  sessioncookie = token

  ; API url
  UrlPath = '/wasdiwebserver/rest/filebuffer/download'
  
  sFileLink = GETFOUNDPRODUCTLINK(oEOImage)
  sBoundingBox = GETVALUEBYKEY(oEOImage,'footprint')

  ; Create a new url object
  oUrl = OBJ_NEW('IDLnetUrl')
  sEncodedLink = oUrl->URLEncode(sFileLink)
  sEncodedBB = oUrl->URLEncode(sBoundingBox)

  sQuery = "sFileUrl=" + sEncodedLink + "&sProvider=ONDA&sWorkspaceId=" + activeworkspace + "&sBoundingBox=" + sEncodedBB
  
  UrlPath = UrlPath + '?' + sQuery
  
  wasdiResult = WASDIHTTPGET(UrlPath, !NULL)
   
  sResponse = GETVALUEBYKEY(wasdiResult, 'boolValue')
  
  sProcessID = ''
  
  ; get the process id
  IF sResponse then BEGIN
    sValue = GETVALUEBYKEY(wasdiResult, 'stringValue')
    sProcessID=sValue
  ENDIF
  
  sStatus = "ERROR"
  
  ; Wait for the process to finish
  IF sProcessID ne '' then BEGIN
    sStatus = WASDIWAITPROCESS(sProcessID)
  ENDIF  
  
  RETURN, wasdiResult
END

; Update the progress of this own process
PRO WASDIUPDATEPROGRESS, iPerc

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
	sMyProcId = myprocid

	sPerc = STRING(iPerc)

	IF (sMyProcId EQ !NULL) OR (STRLEN(sMyProcId) LE 1) THEN BEGIN
		print, 'Progress Update ', sPerc.Trim()
	END ELSE BEGIN
		; API url
		UrlPath = '/wasdiwebserver/rest/process/updatebyid?sProcessId='+sMyProcId+'&status=RUNNING&perc='+sPerc.Trim()+'&sendrabbit=1'
		wasdiResult = WASDIHTTPGET(UrlPath, !NULL)

		; Read updated status
		sStatus = GETVALUEBYKEY(wasdiResult, 'status')
	END
  
END

; Update the progress of this own process
PRO WASDILOG, sLog

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
	
	sTimestamp = SYSTIME()
	print, '[', myprocid, '] - ', sTimestamp,': ', sLog
	IF (isonserver EQ '1') THEN BEGIN
		; API url
		UrlPath = '/wasdiwebserver/rest/processors/logs/add?processworkspace='+myprocid
		wasdiResult = WASDIHTTPPOST(UrlPath, sLog, !NULL)
	END  
END

; Update the status of a WASDI Process
FUNCTION WASDIUPDATEPROCESSSTATUS, sProcessID, sStatus, iPerc

  COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner

  ; API URL
  UrlPath = '/wasdiwebserver/rest/process/updatebyid?sProcessId='+sProcessID+'&status='+sStatus+'&perc='+iPerc
  wasdiResult = WASDIHTTPGET(UrlPath, !NULL)
  
  ; get the output status
  sUpdatedStatus = GETVALUEBYKEY(wasdiResult, 'status')
  
  RETURN, sUpdatedStatus
END


; Adds a new product to the Workspace in WASDI
FUNCTION WASDISAVEFILE, sFileName

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner, workspaceurl

	; API url
	UrlPath = '/wasdiwebserver/rest/catalog/upload/ingestinws?file='+sFileName+'&workspace='+activeworkspace

	wasdiResult = WASDIHTTPGET(UrlPath, workspaceurl)

	; check bool response
	sResponse = GETVALUEBYKEY (wasdiResult, 'boolValue')

	sProcessId = ""

	; get the string value
	IF sResponse THEN BEGIN
		sValue = GETVALUEBYKEY (wasdiResult, 'stringValue')
		sProcessID=sValue
	ENDIF

	sStatus = "ERROR"

	; Wait for the process to finish
	IF sProcessID THEN BEGIN
		sStatus = WASDIWAITPROCESS(sProcessID)
	ENDIF

	RETURN, sStatus;
END


;Get a Parameter stored in the parameters file 
FUNCTION WASDIGETPARAMETER, sParameterName

	COMMON WASDI_SHARED, user, password, token, activeworkspace, basepath, myprocid, baseurl, parametersfilepath, downloadactive, isonserver, verbose, params, uploadactive, workspaceowner
	
	iKeyExists = params.HasKey(sParameterName)
	
	sValue = !NULL
	
	IF (iKeyExists EQ 1) THEN BEGIN
		sValue = params[sParameterName]
	END
    
	RETURN, sValue

END
