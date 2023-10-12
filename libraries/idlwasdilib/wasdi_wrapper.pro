PRO CALLWASDI
	CATCH, Error_status
	IF (Error_status NE 0L) THEN BEGIN
		WASDILOG, 'Error message: ' + !ERROR_STATE.MSG
		EXIT
	ENDIF
	edriftlistflood_archive
END
