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
        oViewElement.type = type;
        oViewElement.sLabel = "";

        return oViewElement;
    }
}


var TableOfProducts = function(){
    this.aoProducts = [];
}

var DateBox = function(){
    this.oDate = null;
}

var SelectArea = function () {
    this.oBoundingBox = {
        northEast : "",
        southWest : ""
    };
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
