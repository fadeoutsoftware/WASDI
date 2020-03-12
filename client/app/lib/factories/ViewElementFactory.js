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
        oViewElement.type = type;
        oViewElement.sLabel = "";

        return oViewElement;
    }
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
