.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _AddAppUIControl:

Add Application User Interface controls
======================================================

Introduction
---------------------------
This tutorial is done to show how to add a new user control to the WASDI Application interface.

All the WASDI Applications takes in input a generic dictionary of parameters. The developer knows the meaning of the parameter of his own application and can document it with a readme.md file.
In the backend of the wasdi app, it is also possible to create an automatic user interface to show the application in the marketplace.
Look the `How to create a User Interface (UI) <https://wasdi.readthedocs.io/en/latest/UITutorial.html>`_ for more general info about this.

Here we see how to create and add a new control that WASDI App developers will be able to add their own user interface.

Existing Controls 
---------------------------
The concept of User Interface control is very old in the IT history; it is present for sure from the first versions of Windows and Apple operating systems. The user controls are a component of the user interface where the user can interact to insert, read or update a specific kind of data.

The most easy and common control is probably a TextBox: a box where the user can insert a text.

Common controls, already supported in WASDI, are for example: 

* TextBox: a box to insert strings
* ComboBox: a list of elements that can be hidden. The user can choose one element of the list
* NumericSlider: a slider that let the user insert an integer number
* NumericField: similar to a textbox, but accepts only floating numbers and not generic strings
* DateTimePicker: a control desinged to insert a date
* Checkbox: usually a sort of switch to input a boolean value

Controls more specific for WASDI are:

* SelectArea: a map that let the user insert a bounding box
* SearchEOImage: a mini-search engine for EO Images
* ProductComboBox: a combobox auto populated with the names of a product in a workspace

In this tuturial we will add the ListBox control: a list of elements where the user can choose zero, one or more elements. 

.. note::

	This tutorial requires the WASDI Client project already configured in your environment
	

WASDI UI definition language
---------------------------
WASDI UI are described by Json Files. 
Each control has a minimum strucure:

.. code-block:: json

	{
		"param": "PARAM_NAME",
		"type": "textbox",
		"label": "Description",
		"tooltip": "Quick help",
		"required": false
	}
	
* param: name of the param that will be given to the app
* type: type of the control. When we add a new control, we add a new type.
* label: what we show as description of the parameter to the user
* tooltip: a quick help that will be shown when the user will hover the mouse over the control
* required: true if the input of this param is mandatory otherwise false

Every control MUST have these data as minimum. Then, the designer, can decide to add more parameters specific of his own control.

For example for our ListBox we define:

.. code-block:: json

	{
		"param": "PARAM_NAME",
		"type": "listbox",
		"label": "description",
		"values": [],
		"required": false,
		"tooltip":""
	}
	
In respect to the general control, we added our **values** parameter:

* values: array of strings. Each string will be an element of the list. The user will be able to choose one or more of these values.

View Element Factory
---------------------------
In the **lib/factories** folder there is the **ViewElementFactory.js** file.

This file contains the definition of all the View Elements. Each control is a View Element. This file contains a class for each control supported.
So the first step is to add our class to the ViewElementFactory:


.. code-block:: javascript

	/**
	 * List Box Control Class
	 * @constructor
	 */
	 class ListBox extends UIComponent { constructor() {
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
	
Our class derive from the base UIComponent one. 

The class must implement:

* this.getValue = function () { ... }: here the class must be able to take the input of the user and return it in the native format (date for dates, number for slider, string for text...)
* this.getStringValue = function () { ... }: here the class must be able to take the input of the user and return it in a string representation

The two methods are needed beacuse not all the languages can support the input of native parameters so there is the option to translate all the input of the user in strings.
The developer will later convert the strings in the code of the WASDI app.

The class CAN implement:

* this.isValid = function (asMessage) { ... }: must return true of false, making a validation of the user input. The asMessages parameter is an array of strings: if the validation is not ok the control can add here a message that will be shown to the user.

After the class has been created, we must move in the **createViewElement** method in the same file. 

This method will be called by the Marketplace UI window to initialize the user interface. It receive in input the json part of the actual control that must be created.

Here there is a cascade of if-else to detect the type of control: we need to add our control:

.. code-block:: javascript

        else if (oControl.type === "listbox") {
            // List Box
            oViewElement = new ListBox();

            let iValues = 0;

            oViewElement.aoElements = [];
            oViewElement.aoSelected = [];

            for (; iValues < oControl.values.length; iValues++) {
                oViewElement.aoElements.push(oControl.values[iValues]);
            }
        }        

All the default properties (label, type, paramName, required) are set by the function. In our branch of the if we just need to:

* Create our class
* Read the "extended" properites we defined in our json definition and use it to initialize our class

This step, is obviously strongly dependant by the control we are implementing: here for exaple we red the values list of string and we save it in elements of our class. We also initialize another array, the one of selected elements, that will be filled by our directive...

Directive
---------------------------
In the **directives/wasdiApp** folder there are all the Angualar Directive that are the physical implementation of the user control. Every time you create a new control you will create also a new directive.

.. note::

	When you add your directive you will have to include the js file in the index.html and declare it in app.js module. If you have it, you will also have to include the .scss file in the style.scss. To build, remember also to add the require of the js file in the directive.js file.

Each control, or ViewElement, has a corresponding directive. The directive is left up the the developer, it has to represent the specific type of input you want to add to WASDI.
It is supposed to interact view the ViewElement class, we will see how in short.

For the moment here an example of our ListBox Directive:

The controller:

.. code-block:: javascript

	angular.module('wasdi.wapListBox', [])
		.directive('waplistbox', function () {
			"use strict";
			return{
				restrict:"E",
				scope :{
					 optionsDirective:'=options',
					//options:'=',
					selectedDirective:'=selected'
					// * Text binding ('@' or '@?') *
					// * One-way binding ('<' or '<?') *
					// * Two-way binding ('=' or '=?') *
					// * Function binding ('&' or '&?') *
				},

				templateUrl:"directives/wasdiApps/wapListBox/wapListBox.html",
				link: function(scope, elem, attrs) {

					scope.pushOptionInSelectedList = function(sBandInput)
					{

						if(utilsIsStrNullOrEmpty(sBandInput) == true) return false;

						var iNumberOfSelectedBand = scope.selectedDirective.length;
						var bFinded = false;
						for(var iIndexBand = 0; iIndexBand < iNumberOfSelectedBand; iIndexBand++)
						{
							if(scope.selectedDirective[iIndexBand] == sBandInput)
							{
								scope.selectedDirective.splice(iIndexBand,1);
								bFinded=true;
								break;
							}
						}

						if(bFinded == false)
						{
							scope.selectedDirective.push(sBandInput);
						}
						return true;
					};
					
					scope.isOptionSelected = function(sBandInput)
					{
						if(utilsIsStrNullOrEmpty(sBandInput) == true) return false;

						var bResult=utilsFindObjectInArray(scope.selectedDirective ,sBandInput);
						if(utilsIsObjectNullOrUndefined(bResult) == true) return false;

						if(bResult == -1)
						{
							return false;
						}

						return true;
					}
				}
			};
		});


The view:

.. code-block:: html
	
	<div class="waplistbox-directive">
		<input type="text"  class="form-control" placeholder="Search..." ng-model="textFilter">

		<div class="list-group" >
			<a href="" class="list-group-item" ng-repeat="option in optionsDirective | filter:textFilter track by $index " ng-class="{active: isOptionSelected(option)}" ng-click="pushOptionInSelectedList(option);">
				{{option}}
			</a>
		</div>
	</div>
	

The style:

.. code-block:: scss	

	.waplistbox-directive
	{

	  .list-group
	  {
		overflow-y: auto;
		max-height:160px;
		min-height: 50px;

		border: 1px solid #43516A;
		border-top-left-radius: 4px;
		border-top-right-radius: 4px;
		a
		{
		  //color: $wasdi-blue-logo;
		  //border-color: $wasdi-blue-logo;
		  border-color: transparent;
		}
		a:hover
		{
		  background-color: darkgrey;
		}
	  }

	  .list-group-item.active, .list-group-item.active:focus, .list-group-item.active:hover {
		z-index: 2;
		color: #fff;
		background-color: #009036;
		border-color:white;//#337ab7;
	  }
	}
	
Is out of the scope of this tutorial to go in the details of the Angular code of this directive. Just recap that it suppose to receve an attribute called **options**, with the array of strings to display; also an attribute called **selected**, again an array, where it will push all the selected elements.

 
Add your Directive to the User Interface
---------------------------

Now we have all the elements, we need to add our control to the Marketplace Application User Interface page.
The file is in **partials/wasdiapplicationui.html**.
This page just show in a cycle all the controls requested by the developer in the UI of the app. The page will take care to show or hide the different controls. All we have to do is add in the "TAB CONTENT" section, our directive:

.. code-block:: javascript

	<!--list box-->
	<div class="col-xs-12 col-md-10 col-lg-8 border-bottom py-2"
		ng-if="viewElement.type === 'listbox'">
		<div class="input-text-label pt-2">{{viewElement.label}}</div>
		<waplistbox options="viewElement.aoElements" selected="viewElement.aoSelected"
			tooltip="viewElement.tooltip"></waplistbox>
	</div>        

As you can notice, the directive receive in input the ViewElement instanced with the code we wrote before. 
So here is request to us to use our own directive with our own ViewElement Ojbect, to initilize the control and retrive back the value inserted by the user.

In our case is done in this snippet:

**<waplistbox options="viewElement.aoElements" selected="viewElement.aoSelected"**

Where we put as options of the directive the elements we got from the JSON and we ask to save the output in aoSelected, that will be used by our ListBox class in the getValue method.

Add a button to the online editor
---------------------------
To help our developers, there is a very basic on line editor of the UI. There every control has a button that the Developer can click to see a mockup of the json required to define that control.

The file is a dialog and can be found:

**dialogs/processor/TabUIProcessor.html**

and 

**dialogs/processor/ProcessorController.js**

In the html we just need to add our button:

.. code-block:: html

	<div class="addUIElementCommand" ng-click="m_oController.addUIElement('listbox')">List Box</div>
	
In the JS, is again simple: just add  in the **addUIElement** your pre-defined json sample:

.. code-block:: javascript

	 else if (sElementType === "listbox") {
		sTextToInsert = '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "listbox",\n\t\t"label": "description",\n\t\t"values": [],\n\t\t"required": false,\n\t\t"tooltip":""\n\t},';
	}

That's it, you created a new User Interface Control for WASDI Applications!!

Welcome to Space, Have fun!