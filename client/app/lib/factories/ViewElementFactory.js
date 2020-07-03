function ViewElementFactory() {
    this.CreateViewElement = function (type) {
        var oViewElement;

        if (type === "textbox") {
            oViewElement = new TextBox();
        }
        if (type === "dropdown") {
            oViewElement = new DropDown();
        }
        if (type === "selectarea") {
            oViewElement = new SelectArea();
        }
        if(type === "date"){
            oViewElement = new DateBox();
        }
        if(type === "tableofproducts"){
            oViewElement = new TableOfProducts();
        }
        if(type === "lighserachproduct"){
            oViewElement = new LightSearchProduct();
        }
        oViewElement.type = type;
        oViewElement.sLabel = "";
        oViewElement.paramName = "";

        return oViewElement;
    }

    this.getTabElements = function (oTab) {

        let aoTabElements = [];

        for (let iControl=0; iControl<oTab.controls.length; iControl ++) {
            let oControl = oTab.controls[iControl];

            var oViewElement;

            if (oControl.type === "textbox") {
                oViewElement = new TextBox();

                if (oControl.default) {
                    oViewElement.sTextBox = oControl.default;
                }
            }
            else if (oControl.type === "dropdown") {
                oViewElement = new DropDown();
            }
            else if (oControl.type === "bbox") {
                oViewElement = new SelectArea();
            }
            else if (oControl.type === "date"){
                oViewElement = new DateBox();
            }
            else if (oControl.type === "tableofproducts"){
                oViewElement = new TableOfProducts();
            }
            else if (oControl.type === "lighserachproduct"){
                oViewElement = new LightSearchProduct();
            }
            else {
                oViewElement = new TextBox();
            }

            oViewElement.type = oControl.type;
            oViewElement.sLabel = oControl.label;
            oViewElement.paramName = oControl.param;

            aoTabElements.push(oViewElement);
        }

        return aoTabElements;
    }
}

var LightSearchProduct = function() {
    this.oTableOfProducts = new TableOfProducts();
    this.oStartDate = new DateBox();
    this.oEndDate = new DateBox();
    this.oSelectArea = new SelectArea();
    this.aoProviders = [];
    this.aoMissionsFilters = [];
};

var TableOfProducts = function(){
    this.aoProducts = [];
    this.isAvailableSelection = false;
    this.isSingleSelection = true;
    this.oSingleSelectionLayer = {};
};

var DateBox = function(){
    this.oDate = null;
};

var SelectArea = function () {
    this.oBoundingBox = {
        northEast : "",
        southWest : ""
    };
    this.iWidth = "";
    this.iHeight = "";
};

var TextBox = function () {
    this.sTextBox = "";
};

var DropDown = function () {
    this.asListValues = [];
    this.sSelectedValues = "";
    this.oOnClickFunction = null;
    this.bEnableSearchFilter = true;
    this.sDropdownName = "";
};
