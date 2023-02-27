function ViewElementFactory() {

    /**
     * Creates a single view element from a control in input.
     * @param oControl Control Description: minimum properties are param, type, label
     * @returns {TextBox|SelectArea|ProductList|ProductsCombo|CheckBox|SearchEOImage|DateTimePicker|ListBox|*}
     */
    this.createViewElement = function (oControl) {

        // Return variable
        let oViewElement;

        // Check if we have a valid input
        if (utilsIsObjectNullOrUndefined(oControl)) return oViewElement;
        // If mandatory is not set, assume false
        if (utilsIsObjectNullOrUndefined(oControl.required)) oControl.required = false;
        // // adding an optional tooltip to all the possible components
        //

        // Find the right type and create the element
        if (oControl.type === "textbox") {

            // Text box
            oViewElement = new TextBox();

            // See if we have a default
            if (oControl.default) {
                oViewElement.m_sText = oControl.default;
            }
        }
        else if (oControl.type === "numeric") {

            // Numeric box
            oViewElement = new NumericBox();

            // See if we have a default
            if (oControl.default) {
                oViewElement.m_sText = parseFloat(oControl.default);
            }
            // check if the field can be parsed as number
            // this will solve the ignored constraints when the value is set to 0
            if (!isNaN(parseFloat(oControl.min))) {
                oViewElement.m_fMin = oControl.min;
            }
            // same for the max
            if (!isNaN(parseFloat(oControl.max))) {
                oViewElement.m_fMax = oControl.max;
            }
        }
        else if (oControl.type === "dropdown") {
            // Drop Down
            oViewElement = new DropDown();

            let iValues = 0;

            oViewElement.asListValues = [];

            for (; iValues < oControl.values.length; iValues++) {
                let oItem = {
                    name: oControl.values[iValues],
                    id: "" + iValues
                };

                oViewElement.asListValues.push(oItem);

                if (oControl.default === oItem.name) {
                    oViewElement.sSelectedValues = oItem;
                }
            }
        }
        else if (oControl.type === "bbox") {
            // Bounding Box from Map

            oViewElement = new SelectArea();

            if (!utilsIsObjectNullOrUndefined(oControl.maxArea)) {
                oViewElement.maxArea = oControl.maxArea;
            }
            if (!utilsIsObjectNullOrUndefined(oControl.maxSide)) {
                oViewElement.maxSide = oControl.maxSide;
            }

            if (!utilsIsObjectNullOrUndefined(oControl.maxRatioSide)) {
                oViewElement.maxRatioSide = oControl.maxRatioSide;
            }
        }
        else if (oControl.type === "date") {
            oViewElement = new DateTimePicker();
        }
        else if (oControl.type === "productlist") {
            oViewElement = new ProductList();
        }
        else if (oControl.type === "searcheoimage") {
            oViewElement = new SearchEOImage();
        }
        else if (oControl.type === "productscombo") {
            if (oControl.showExtension != undefined) {
                oViewElement = new ProductsCombo(oControl.showExtension);
            } else {
                oViewElement = new ProductsCombo(false); // back to default behaviour, in case showExtension is not specified
            }
        }
        else if (oControl.type === "boolean") {
            oViewElement = new CheckBox();

            if (utilsIsObjectNullOrUndefined(oControl.default) == false) {
                oViewElement.m_bValue = oControl.default;
            }
        }
        else if (oControl.type === "slider") {
            oViewElement = new Slider();

            if (utilsIsObjectNullOrUndefined(oControl.min) == false) {
                oViewElement.m_iMin = oControl.min;
            }
            if (utilsIsObjectNullOrUndefined(oControl.max) == false) {
                oViewElement.m_iMax = oControl.max;
            }
            if (utilsIsObjectNullOrUndefined(oControl.default) == false) {
                oViewElement.m_iValue = oControl.default;
            }
        }
        else if (oControl.type === "hidden") {
            oViewElement = new Hidden();
            oViewElement.m_oValue = oControl.default;
        }
        else if (oControl.type === "listbox") {
            // List Box
            oViewElement = new ListBox();

            let iValues = 0;

            oViewElement.aoElements = [];
            oViewElement.aoSelected = [];

            for (; iValues < oControl.values.length; iValues++) {
                oViewElement.aoElements.push(oControl.values[iValues]);
            }
        } else if (oControl.type === 'table') {

            oViewElement = new Table();

            for (let sRowHeader = 0; sRowHeader < oControl.rows; sRowHeader++) {
                const sElement = oControl.row_headers[sRowHeader];
                oViewElement.aoTableVariables.push([])
                for (let sColHeader = 0; sColHeader < oControl.columns; sColHeader++) {
                    const sElement = oControl.col_headers[sColHeader];
                    oViewElement.aoTableVariables[sRowHeader].push('');
                }
            }

            for(let sRowHeader = 0; sRowHeader < oControl.rows; sRowHeader++){
                const sElement = oControl.row_headers[sRowHeader];
                oViewElement.aoTableVariables[1].push(sElement);
           }
        }
        else {
            oViewElement = new TextBox();
        }
        if (!utilsIsObjectNullOrUndefined(oControl.tooltip)) {
            oViewElement.tooltip = oControl.tooltip;
        }

        oViewElement.type = oControl.type;
        oViewElement.label = oControl.label;
        oViewElement.paramName = oControl.param;
        oViewElement.required = oControl.required;
        oViewElement.rowHeaders = oControl.row_headers;
        oViewElement.colHeaders = oControl.col_headers;

        return oViewElement;
    }

    this.getTabElements = function (oTab) {

        let aoTabElements = [];

        for (let iControl = 0; iControl < oTab.controls.length; iControl++) {
            let oControl = oTab.controls[iControl];

            let oViewElement = this.createViewElement(oControl);

            aoTabElements.push(oViewElement);
        }

        return aoTabElements;
    }
}

/**
 * Basic class for UI components
 */
class UIComponent {
    constructor() {
        //TODO remove text and defaults to empty string
        this.tooltip = "";
    }

}

/**
 * Search EO Image Control Class
 * @constructor
 */
class SearchEOImage extends UIComponent {
    constructor() {
        super();
        this.oTableOfProducts = new ProductList();
        this.oStartDate = new DateTimePicker();
        this.oEndDate = new DateTimePicker();
        this.oSelectArea = new SelectArea();
        this.aoProviders = [];
        this.aoProviders.push("ONDA");
        this.aoMissionsFilters = [];


        /*
        let tst7 = oFactory.CreateViewElement("searcheoimage");
        tst7.sLabel = "Sono una light search";
        tst7.oStartDate.m_sDate =  moment().subtract(1, 'days').startOf('day');
        tst7.oEndDate.m_sDate = moment();
        tst7.oSelectArea.iHeight = 200;
        tst7.oSelectArea.iWidth = 500;
        tst7.aoProviders.push(providers.ONDA);
        tst7.aoMissionsFilters.push({name:"sentinel-1" },{name:"sentinel-2" });
        tst7.oTableOfProducts.isAvailableSelection = true;
        tst7.oTableOfProducts.isSingleSelection = true;
        */

        /**
         * This control does not really return a value
         * @returns {string}
         */
        this.getValue = function () {
            return "";
        }

        /**
         * This control does not really return a value
         * @returns {string}
         */
        this.getStringValue = function () {
            return "";
        }
    };
}

/**
 * Product List Control Class
 * @constructor
 */
class ProductList extends UIComponent {
    constructor() {
        super();

        this.aoProducts = [];
        this.isAvailableSelection = false;
        this.isSingleSelection = true;
        this.oSingleSelectionLayer = {};

        /**
         * Return the selected product
         * @returns {{}}
         */
        this.getValue = function () {
            return this.oSingleSelectionLayer;
        }

        /**
         * Return the name of the selected product
         * @returns {{}}
         */
        this.getStringValue = function () {
            return this.oSingleSelectionLayer;
        }

    };
}

/**
 * Date Time Picker Control Class
 * @constructor
 */
class DateTimePicker extends UIComponent {
    constructor() {
        super();


        this.m_sDate = null;

        /**
         * Returns the selected Date
         * @returns {string|null} Date as a string in format YYYY-MM-DD
         */
        this.getValue = function () {
            if (this.m_sDate) {
                return this.m_sDate;
            } else {
                return null;
            }
        }

        /**
         * Returns the selected Date
         * @returns {string|null} Date as a string in format YYYY-MM-DD
         */
        this.getStringValue = function () {
            if (this.m_sDate) {
                return this.m_sDate;
            } else {
                return "";
            }
        }

    };
}



/**
 * Select Area (bbox) Control Class
 * @constructor
 */
class SelectArea extends UIComponent {
    constructor() {
        super();
        // using zero as default to relax the constraints
        this.maxArea = 0;
        this.maxSide = 0;
        this.maxRatioSide = 0;
        this.oBoundingBox = {
            northEast: "",
            southWest: ""
        };
        this.iWidth = "";
        this.iHeight = "";

        /**
         * Return the bbox as a JSON Obkect
         * @returns {{southWest: {lat: "", lon:""}, northEast: {lat: "", lon:""}}|string}
         */
        this.getValue = function () {
            try {

                var sTest = this.getStringValue();

                if (sTest!="") {
                    return this.oBoundingBox;
                }
                else {
                    return null;
                }
            }
            catch (e) {
                return null;
            }
        }

        /**
         * Return the bounding box as a string.
         * @returns {string} BBox as string: LATN,LONW,LATS,LONE
         */
        this.getStringValue = function () {
            try {
                if (this.oBoundingBox) {
                    return "" + this.oBoundingBox.northEast.lat.toFixed(2) + "," + this.oBoundingBox.southWest.lng.toFixed(2) + "," + this.oBoundingBox.southWest.lat.toFixed(2) + "," + + this.oBoundingBox.northEast.lng.toFixed(2);
                }
                else {
                    return "";
                }
            }
            catch (e) {
                return "";
            }
        }
        this.isValid = function(asMessages){
            // this checks that the value assigned is different from the default.
            return this.oBoundingBox.northEast != "" && this.oBoundingBox.southWest != "" ; 
        }
    };
}



/**
 * Text Box Control Class
 * @constructor
 */
class TextBox extends UIComponent {
    constructor() {
        super();

        this.m_sText = "";

        /**
         * Get the value of the textbox
         * @returns {string} String in the textbox
         */
        this.getValue = function () {
            return this.m_sText;
        }

        /**
         * Get the value of the textbox
         * @returns {string} String in the textbox
         */
        this.getStringValue = function () {
            return this.m_sText;
        }
    };
}
/**
 * Numeric Control Class
 * @constructor
 */
class NumericBox extends UIComponent {
    constructor() {
        super();

        this.m_sText = "";
        this.m_fMin = null;
        this.m_fMax = null;

        this.isValid = function(asMessages) {
            try {
                let fValue = parseFloat(this.m_sText)
                // if we can't parse the value as a number
                if (isNaN(fValue)) {
                    if (utilsIsObjectNullOrUndefined(this.required)==false) {
                        if (this.required) {
                            asMessages.push(this.label + " - Please check parameters ");
                            return false;        
                        }
                    }
                }
                if (utilsIsObjectNullOrUndefined(this.m_fMin)==false) {
                    if (fValue<this.m_fMin) {
                        asMessages.push(this.label + " - Value must be greater than " + this.m_fMin);
                        return false;
                    }
                }

                if (utilsIsObjectNullOrUndefined(this.m_fMax)==false) {
                    if (fValue>this.m_fMax) {
                        asMessages.push(this.label + " - Value must be smaller than " + this.m_fMax);
                        return false;
                    }
                }                
            }
            catch(oError) {
                return false;
            }

            return true;
        }

        /**
         * Get the value of the numericbox
         * @returns {string} Value in the numericbox
         */
        this.getValue = function () {
            var fFloatValue = parseFloat(this.m_sText);
            if (isNaN(fFloatValue)) return null;
            else return fFloatValue;
        }

        /**
         * Get the string from the numericbox
         * @returns {string} String in the numericbox
         */
        this.getStringValue = function () {
            return this.m_sText.toString();
        }
    };
}

/**
 * Drop Down Control Class
 * @constructor
 */
class DropDown extends UIComponent {
    constructor() {
        super();

        this.asListValues = [];
        this.sSelectedValues = "";
        this.oOnClickFunction = null;
        this.bEnableSearchFilter = true;
        this.sDropdownName = "";

        /**
         * Get the selected value
         * @returns {string}
         */
        this.getValue = function () {
            return this.sSelectedValues.name;
        }

        /**
         * Get the selected value
         * @returns {string}
         */
        this.getStringValue = function () {
            return this.sSelectedValues.name;
        }
    }
};



/**
 * Hidden Control Class
 * @constructor
 */
let Hidden = function () {
    this.m_oValue = "";

    /**
     * Get the value of the control
     * @returns {string} String in the control
     */
    this.getValue = function () {
        return this.m_oValue;
    }

    /**
     * Get the value of the control
     * @returns {string} String in the control
     */
    this.getStringValue = function () {
        return String(this.m_oValue);
    }
};

/**
 * Check box Control Class
 * @constructor
 */
class CheckBox extends UIComponent {
    constructor() {
        super();
        this.m_bValue = true;

        /**
         * Return the value of the checkbox
         * @returns {boolean} True if selected, False if not
         */
        this.getValue = function () {
            return this.m_bValue;
        }

        /**
         * Return the value of the checkbox as a string:
         * 1 = true 0 = false
         * @returns {string}
         */
        this.getStringValue = function () {
            if (this.m_bValue) {
                return "1";
            }
            else {
                return "0";
            }
        }

    };
}




/**
 * Products Combo Control Class
 * @constructor
 */
class ProductsCombo extends UIComponent {
    constructor(bShowExt) {
        super();

        this.asListValues = [];
        this.sSelectedValues = "";
        this.oOnClickFunction = null;
        this.bEnableSearchFilter = true;
        this.sDropdownName = "";
        this.bShowExtension = bShowExt;

        /**
         * Get the selected value
         * @returns {string}
         */
        this.getValue = function () {
            return this.sSelectedValues.name;
        }

        this.getStringValue = function () {
            return this.sSelectedValues.name;
        }
    };
}

/**
 * Slider for a numeric input
 * @constructor
 */
class Slider extends UIComponent {
    constructor() {
        super();

        this.m_iMin = 0;
        this.m_iMax = 10;
        this.m_iValue = 5;

        /**
         * Get the selected value
         * @returns {number}
         */
        this.getValue = function () {
            return this.m_iValue;
        }

        /**
         * Get the selected value as a string
         * @returns {number}
         */
        this.getStringValue = function () {
            return String(this.m_iValue);
        }

    }
}

/**
 * List Box Control Class
 * @constructor
 */
 class ListBox extends UIComponent {
    constructor() {
        super();

        this.aoElements = [];
        this.aoSelected = [];

        /**
         * Return the selected product
         * @returns {{}}
         */
        this.getValue = function () {
            return this.aoSelected;
        }

        /**
         * Return the name of the selected product
         * @returns {{}}
         */
        this.getStringValue = function () {

            let sReturn = "";
            let iSel = 0;

            if (this.aoSelected != undefined) {
                if (this.aoSelected != null) {
                    for (iSel = 0; iSel<this.aoSelected.length; iSel++) {
                        if (iSel>0) sReturn = sReturn + ";";
                        sReturn = sReturn + this.aoSelected[iSel];
                    }
                }
            }
            return sReturn;
        }

    };
}

class Table extends UIComponent {
    constructor() {
        super();

        this.aoTableVariables = [];

        /*
        * Return the selected product
        * @returns {{}}
        */
        this.getValue = function () {
            console.log(this.aoTableVariables)
            return this.aoTableVariables;
        }

        /*
         * Return the table array stringified
         * @returns {{}}
         */
        this.getStringValue = function () {
            console.log(JSON.stringify(this.aoTableVariables))
            return JSON.stringify(this.aoTableVariables);
        }

    }
}