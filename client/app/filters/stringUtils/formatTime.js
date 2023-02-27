angular.module('wasdi.stringUtilsTime', [])

.filter('convertMsToTime', function() {
	return function(lMilliseconds) {

		this.padTo2Digits = function(lNum) {  
			return lNum.toString().padStart(2, "0");
		}

        let lSeconds = Math.floor(lMilliseconds / 1000);
        let lMinutes = Math.floor(lSeconds / 60);
        let lHours = Math.floor(lMinutes / 60);

        lSeconds = lSeconds % 60;
        lMinutes = lMinutes % 60;

        return `${this.padTo2Digits(lHours)}:${this.padTo2Digits(lMinutes)}:${this.padTo2Digits(lSeconds)}`;
	}
});