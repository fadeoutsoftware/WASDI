function ViewElementFactory() {

    /**
     * Creates a single view element from a control in input.
     * @param oControl Control Description: minimum properties are param, type, label
     * @returns {TextBox|SelectArea|ProductList|ProductsCombo|CheckBox|SearchEOImage|DateTimePicker|*}
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
                oViewElement.m_sValue = parseFloat(oControl.default);
                oViewElement.m_sText = parseFloat(oControl.default);
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
            //oViewElement.maxarea = oControl.maxarea;

            // oViewElement.maxarea= oControl.maxArea;
            // oViewElement.maxside  = oControl.maxSide;
            // oViewElement.maxratioSide  = oControl.maxRatioSide;
            if (!utilsIsObjectNullOrUndefined(oControl.maxArea)){
                oViewElement.maxArea = oControl.maxArea;
            }
            if (!utilsIsObjectNullOrUndefined(oControl.maxSide)){
                oViewElement.maxSide = oControl.maxSide;
            }

            if (!utilsIsObjectNullOrUndefined(oControl.maxRatioSide)){
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
        else {
            oViewElement = new TextBox();
        }
        if (!utilsIsObjectNullOrUndefined(oControl.tooltip)){
                 oViewElement.tooltip = oControl.tooltip;
             }

        oViewElement.type = oControl.type;
        oViewElement.label = oControl.label;
        oViewElement.paramName = oControl.param;
        oViewElement.required = oControl.required

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
 * Search EO Image Control Class
 * @constructor
 */
let SearchEOImage = function () {
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

/**
 * Product List Control Class
 * @constructor
 */
let ProductList = function () {
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

/**
 * Date Time Picker Control Class
 * @constructor
 */
let DateTimePicker = function () {
    this.m_sDate = null;

    /**
     * Returns the selected Date
     * @returns {string|null} Date as a string in format YYYY-MM-DD
     */
    this.getValue = function () {
        if (this.m_sDate) {
            return this.m_sDate;
        }
        else {
            return "";
        }
    }

    /**
     * Returns the selected Date
     * @returns {string|null} Date as a string in format YYYY-MM-DD
     */
    this.getStringValue = function () {
        if (this.m_sDate) {
            return this.m_sDate;
        }
        else {
            return "";
        }
    }

};

/**
 * Basic class for UI components
 */
class UIComponent{
    constructor(){
        this.tooltip = "";
    }
}

/**
 * Select Area (bbox) Control Class
 * @constructor
 */
class SelectArea extends UIComponent{
    constructor(){
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
            return this.oBoundingBox;
        }
        catch (e) {
            return "";
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
};
}



/**
 * Text Box Control Class
 * @constructor
 */
let TextBox = function () {
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
/**
 * Numeric Control Class
 * @constructor
 */
let NumericBox = function () {
    this.m_sValue = 0;
    this.m_sText = this.m_sValue.toString();

    /**
     * Get the value of the numericbox
     * @returns {string} Value in the numericbox
     */
    this.getValue = function () {
        return parseFloat(this.m_sText);
    }

    /**
     * Get the string from the numericbox
     * @returns {string} String in the numericbox
     */
    this.getStringValue = function () {
        return this.m_sValue.toString();
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
let CheckBox = function () {
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

/**
 * Drop Down Control Class
 * @constructor
 */
let DropDown = function () {
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
};


/**
 * Products Combo Control Class
 * @constructor
 */
let ProductsCombo = function (bShowExt) {
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

/**
 * Slider for a numeric input
 * @constructor
 */
let Slider = function () {
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
