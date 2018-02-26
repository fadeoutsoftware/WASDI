angular.module('wasdi.ImageEditorDirective', [])
    .directive('imageeditor', function () {
        "use strict";
        return{
            restrict : 'EAC',
            // template: "<canvas id='test' width='800' height='600'></canvas> ",
            templateUrl:"directives/image_editor/ImageEditorView.html",
            scope :{
                // onClick: '&',
                urlImage : '=',
                body : '=',
                isLoaded : '='
                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
            },
            link: function(scope, elem, attrs) {
                var iDefaultValueZoom = 100;
                //default value canvas
                var iDefaultHeight = 280;
                //default value canvas
                var iDefaultWidth = 560;
                var stage = new createjs.Stage("imageviewcanvas");
                var oBitmap = new createjs.Bitmap(scope.urlImage);

                // For mobile devices.
                createjs.Touch.enable(stage);

                // this lets our drag continue to track the mouse even when it leaves the canvas:
                // play with commenting this out to see the difference.
                stage.mouseMoveOutside = true;

                stage.addChild(oBitmap);

                //create tick
                createjs.Ticker.on("tick", tick);

                function tick(event) {
                    // Other stuff
                    stage.update(event);
                };

                var bItIsClicked = false;
                var oMouseDownPoint ={
                    stageX:"",
                    stageY:""
                };
                var oMouseLastPoint ={
                    stageX:"",
                    stageY:""
                };

                // var oController = this;
                stage.on("stagemousedown", function(evt) {
                    bItIsClicked = true;
                    oMouseDownPoint.stageX = evt.stageX;
                    oMouseDownPoint.stageY = evt.stageY;
                    console.log("stagemousedown");
                    // console.log("evt.stageX :" +evt.stageX);
                    // console.log("evt.stageY :" +evt.stageY);

                });

                stage.on("stagemouseup", function(evt) {
                    bItIsClicked = false;
                    oMouseLastPoint.stageX = evt.stageX;
                    oMouseLastPoint.stageY = evt.stageY;
                    console.log("stagemouseup");
                    // console.log("evt.stageX :" +evt.stageX);
                    // console.log("evt.stageY :" +evt.stageY);
                    //TODO Draw Rectangle(boh?)

                });

                stage.on("stagemousemove", function(evt) {
                    if( bItIsClicked === true )
                    {
                        //update actual position
                        oMouseLastPoint.stageX = evt.stageX;
                        oMouseLastPoint.stageY = evt.stageY;
                        // console.log("evt.stageX :" +evt.stageX);
                        // console.log("evt.stageY :" +evt.stageY);

                        // console.log("------------- DEBUG stagemousemove-------------");
                        // console.log("oMouseLastPoint.stageX" + oMouseLastPoint.stageX);
                        // console.log("oMouseLastPoint.stageY" +  oMouseLastPoint.stageY);
                        // console.log("oMouseDownPoint.stageX" + oMouseDownPoint.stageX);
                        // console.log("oMouseDownPoint.stageY" + oMouseDownPoint.stageY);
                        var oSquarePoints = scope.calculateSquarePointsByMousePoints(oMouseDownPoint,oMouseLastPoint);

                        // console.log("oSquarePoints.x "+ oSquarePoints.x);
                        // console.log("oSquarePoints.y " + oSquarePoints.y);
                        // console.log("oSquarePoints.width " + oSquarePoints.width);
                        // console.log("oSquarePoints.height " + oSquarePoints.height);

                        if(utilsIsObjectNullOrUndefined(scope.Square) === true)
                        {

                            //var oSquare = scope.createSquare(oSquarePoints.x,oSquarePoints.y,oSquarePoints.width,oSquarePoints.height);
                            var oSquare = scope.createSquare(oMouseDownPoint.stageX,oMouseDownPoint.stageY,oMouseLastPoint.stageX-oMouseDownPoint.stageX,oMouseLastPoint.stageY-oMouseDownPoint.stageY);

                            if(utilsIsObjectNullOrUndefined(oSquare) === false)
                            {
                                // add new square
                                scope.Square = oSquare;
                                stage.addChild(oSquare);
                                // stage.setChildIndex( oSquare, stage.numChildren - 1);
                            }

                        }
                        else
                        {
                            // console.log("------------------ Update ------------------");
                            // console.log("oSquarePoints.x "+ oSquarePoints.x);
                            // console.log("oSquarePoints.y " + oSquarePoints.y);
                            // console.log("oSquarePoints.width " + oSquarePoints.width);
                            // console.log("oSquarePoints.height " + oSquarePoints.height);

                            scope.updateSquare(scope.Square, oMouseDownPoint.stageX,oMouseDownPoint.stageY,oMouseLastPoint.stageX-oMouseDownPoint.stageX,oMouseLastPoint.stageY-oMouseDownPoint.stageY);
                        }

                        stage.update();
                        //TODO REMOVE OLD RECTANGLE (UPDATE IT ?)
                        console.log("stagemousemove");

                    }

                });



                stage.update();

                // scope.Square = square;
                scope.Square = null;
                scope.Stage = stage;
                scope.Bitmap = oBitmap;
                scope.iDefaultHeight = iDefaultHeight;
                scope.iDefaultWidth = iDefaultWidth;

                scope.updateSquare = function(oSquare,iMidPointX,iMidPointY,iWidth,iHeight){
                    oSquare.graphics.clear().setStrokeStyle(2).beginStroke("#009036").beginFill("#43516A").drawRect(iMidPointX, iMidPointY, iWidth, iHeight);
                    oSquare.alpha = 0.5;
                    return oSquare;
                }

                scope.createSquare = function(iMidPointX,iMidPointY,iWidth,iHeight){
                    var square = new createjs.Shape();
                    square.graphics.setStrokeStyle(2).beginStroke("#009036").beginFill("#43516A").drawRect(iMidPointX, iMidPointY, iWidth, iHeight);
                    square.alpha = 0.5;
                    return square;
                }

                scope.calculateSquarePointsByMousePoints = function(oMouseDownPoint,oMouseLastPoint){
                    var oMidPoint = utilsGetMidPoint(oMouseDownPoint.stageX,oMouseDownPoint.stageY,oMouseLastPoint.stageX,oMouseLastPoint.stageY);

                    if(utilsIsObjectNullOrUndefined(oMidPoint))
                    {
                        return false;
                    }

                    // oMouseDownPoint=(x1,y1)
                    // oMouseLastPoint=(x2,y2)
                    // oPointA=(x2,y1)?
                    // oPointB=(x1,y2)?
                    /*
                    // RECTANGLE EXAMPLE :
                    *   (x1,y1) ------------------------- (x2,y1)
                    *           -------------------------
                    *           -------------------------
                    *           -------------------------
                    *           -------------------------
                    *   (x1,y2)                           (x2,y2)
                    *
                    * */
                    var oPointA = {
                            x:oMouseLastPoint.stageX,
                            y:oMouseDownPoint.stageY
                    };
                    var oPointB = {
                            x:oMouseDownPoint.stageX,
                            y:oMouseLastPoint.stageY
                    };
                    // console.log("------------------------- Points -------------------------")
                    // console.log("oPointA.X" + oPointA.x);
                    // console.log("oPointA.Y" + oPointA.y);
                    // console.log("oPointB.X" + oPointB.x);
                    // console.log("oPointB.Y" + oPointB.y);
                    var fWidth = utilsCalculateDistanceBetweenTwoPoints (oMouseDownPoint.stageX,oMouseDownPoint.stageY,oPointA.x,oPointA.y);
                    var fHeight = utilsCalculateDistanceBetweenTwoPoints (oMouseDownPoint.stageX,oMouseDownPoint.stageY,oPointB.x,oPointB.y);
                    // console.log("------------------ Return value ------------------");
                    // console.log("oMidPoint.x "+ oMidPoint.x);
                    // console.log("oMidPoint.y " + oMidPoint.y);
                    // console.log("fWidth " + fWidth);
                    // console.log("fHeight " + fHeight);
                    return {
                        x:oMidPoint.x,
                        y:oMidPoint.y,
                        width:fWidth,
                        height:fHeight
                    }
                    // var oSquare = scope.createSquare(oMidPoint.x,oMidPoint.y,fWidth,fHeight);
                    // return oSquare;
                };

                // scope.createDraggerContainer = function(){
                //     var dragger = new createjs.Container();
                //     dragger.x = dragger.y = 0;
                //
                // }
                // scope.CreateDraggableSquare = function(){
                //     dragger.addChild(square); //, label
                //     stage.addChild(dragger);
                // }
                scope.$watch('urlImage', function (newValue, oldValue, scope)
                {
                    if(utilsIsObjectNullOrUndefined(newValue) === false && newValue !== "empty")
                    {
                        var oBitmap =  new createjs.Bitmap(newValue);
                        scope.Stage.addChild(oBitmap);

                    }
                    else {
                        scope.Stage.autoClear = true;
                        scope.Stage.removeAllChildren();
                        scope.Stage.update();
                    }

                });
            }
        };
    });