/**
 * Created by p.campanella on 04/11/2016.
 */

angular.module('wasdi.SIDirective', [])
    .directive('spaceninvaders', ['$document', function ($document) {
        "use strict";
        return {
            restrict : 'EAC',
            replace : true,
            scope :{
            },
            template: "<canvas width='800' height='600'></canvas>",
            link: function (scope, element, attribute) {
                var w, h, iSquareWidth, iSquareHeight;
                var iGridSizeX = 40;
                var iGridSizeY = 30;
                var iSlowCounter = 0;
                var iSlowLimit = 8;
                var bGaming = false;
                var bGameOver = false;
                var iPoints = 0;

                // Key codes
                var KEYCODE_UP = 38;
                var KEYCODE_LEFT = 37;
                var KEYCODE_RIGHT = 39;
                var KEYCODE_DOWN = 40;
                var KEYCODE_SPACE = 32;

                var oDirection = { x: 0, y: 1};

                var aoSnakePoints = [ { x:20,y:15}];
                var oGoalPoint = {};

                var sSnakeColor = "#43526B";
                var sGoalColor = "#009036";
                var sBorderColor = "Black";
                var sFontColor = "#009036";

                $document.on("keydown keypress", KeyPress);

                scope.$on(
                    "$destroy",
                    function( event ) {
                        $document.off("keydown keypress", KeyPress);
                    }
                );

                drawGame();
                function drawGame() {

                    if (scope.stage) {
                        // Normal Redraw
                        scope.stage.autoClear = true;
                        scope.stage.removeAllChildren();
                        scope.stage.update();
                    } else {

                        InitNewGame();

                        // Create the state
                        scope.stage = new createjs.Stage(element[0]);

                        // Take dimension
                        w = scope.stage.canvas.width;
                        h = scope.stage.canvas.height;

                        // Compute square dimension
                        iSquareWidth = w/iGridSizeX;
                        iSquareHeight = h/iGridSizeY;

                        //scope.stage.addEventListener("stagemousedown", handleJumpStart);

                        // Create Gol
                        oGoalPoint = GetGoalPoint();

                        // Start Timer
                        createjs.Ticker.timingMode = createjs.Ticker.RAF;
                        createjs.Ticker.addEventListener("tick", tick);
                    }

                    // Next position of the snake
                    var oNewPoint = {};
                    var oLastPosition = aoSnakePoints[aoSnakePoints.length-1];

                    // Sum direction vector
                    oNewPoint.x = oLastPosition.x + oDirection.x;
                    oNewPoint.y = oLastPosition.y + oDirection.y;

                    if (bGaming) {
                        if (SnakeFault(oNewPoint)) {
                            bGaming = false;
                            bGameOver = true;
                        }
                        else {
                            // Add it to the snake
                            aoSnakePoints.push(oNewPoint);

                            // Was goal reached?
                            if (oNewPoint.x == oGoalPoint.x && oNewPoint.y == oGoalPoint.y) {
                                // Yes get new goal!
                                oGoalPoint = GetGoalPoint();
                                // And speed up
                                if (iSlowLimit>0) iSlowLimit--;
                            }
                            else {
                                // No: remove the tail
                                aoSnakePoints.splice(0,1);
                            }
                        }
                    }

                    // Drawing cycle
                    var iSnakeCells = aoSnakePoints.length;

                    // Draw the border
                    var oBoundsShape = new createjs.Shape();
                    oBoundsShape.graphics.beginStroke(sBorderColor).drawRect(0, 0, w, h);
                    scope.stage.addChild(oBoundsShape);

                    // Draw the snake
                    for (var iCells=0; iCells<iSnakeCells; iCells++) {
                        var oSquare = new createjs.Shape();
                        var oRect = PointToRectangle(aoSnakePoints[iCells]);
                        oSquare.graphics.beginFill(sSnakeColor).drawRect(oRect.x, oRect.y, oRect.width, oRect.height);
                        scope.stage.addChild(oSquare);
                    }

                    // Draw the goal
                    var oGoalShape = new createjs.Shape();
                    var oGoalRect = PointToRectangle(oGoalPoint);
                    oGoalShape.graphics.beginFill(sGoalColor).drawRect(oGoalRect.x, oGoalRect.y, oGoalRect.width, oGoalRect.height);
                    scope.stage.addChild(oGoalShape);

                    if (bGaming == false && bGameOver == false) {
                        // Show Start Text
                        var oText = new createjs.Text();
                        oText.x = oText.y = 0;
                        oText.font = "68px astronaut";
                        oText.color = sFontColor;
                        oText.text = "Press Space to Play";

                        scope.stage.addChild(oText);
                    }
                    else if (bGaming == false && bGameOver == true ) {

                        // Show Game Over Text
                        var oText = new createjs.Text();
                        oText.x = oText.y = 0;
                        oText.font = "68px astronaut";
                        oText.color = sFontColor;
                        oText.text = "Game Over";

                        scope.stage.addChild(oText);
                    }

                }

                function PointToRectangle(oPoint) {
                    var oRet =  {};

                    oRet.x = oPoint.x*iSquareWidth;
                    oRet.width = iSquareWidth;
                    oRet.y = oPoint.y*iSquareHeight;
                    oRet.height = iSquareHeight;

                    return oRet;
                }

                function GetGoalPoint() {

                    var bFound = false;

                    while (!bFound) {
                        // Create Gol Point
                        oGoalPoint.x = parseInt(Math.random() * iGridSizeX);
                        oGoalPoint.y = parseInt(Math.random() * iGridSizeY);

                        bFound = true;

                        // Check is not on the snake
                        var iSnakeSize = aoSnakePoints.length;
                        for (var i=0; i<iSnakeSize; i++) {
                            var oPoint = aoSnakePoints[i];
                            if (oPoint.x == oGoalPoint.x && oPoint.y == oGoalPoint.y) {
                                bFound=false;
                                break;
                            }
                        }
                    }


                    return oGoalPoint;
                }

                function SnakeFault(oNewPoint) {
                    // Check is not on the snake
                    var iSnakeSize = aoSnakePoints.length;
                    for (var i=0; i<iSnakeSize; i++) {
                        var oPoint = aoSnakePoints[i];
                        if (oPoint.x == oNewPoint.x && oPoint.y == oNewPoint.y) {
                            return true;
                        }
                    }

                    if (oNewPoint.x<0) return true;
                    if (oNewPoint.x>=iGridSizeX) return true;
                    if (oNewPoint.y<0) return true;
                    if (oNewPoint.y>=iGridSizeY) return true;

                    return false;
                }

                function tick(event) {

                    if (bGaming) {
                        if (iSlowCounter==iSlowLimit) {
                            iSlowCounter = 0;
                            drawGame();
                        }
                        else {
                            iSlowCounter++;
                        }
                    }

                    scope.stage.update(event);
                }

                function KeyPress(event) {
                    //console.log("KeyPress");

                    if (event.which == KEYCODE_DOWN) {
                        if (oDirection.y!=-1) {
                            oDirection.x = 0;
                            oDirection.y = 1;
                        }
                    }
                    else if (event.which == KEYCODE_LEFT) {
                        if (oDirection.x != 1) {
                            oDirection.x = -1;
                            oDirection.y = 0;
                        }
                    }
                    else if (event.which == KEYCODE_RIGHT) {
                        if (oDirection.x != -1) {
                            oDirection.x = 1;
                            oDirection.y = 0;
                        }
                    }
                    else if (event.which == KEYCODE_UP) {
                        if (oDirection.y != 1) {
                            oDirection.x = 0;
                            oDirection.y = -1;
                        }
                    }
                    else if (event.which == KEYCODE_SPACE) {

                        if (bGameOver) {
                            bGameOver = !bGameOver;
                            drawGame();
                            scope.stage.update(event);

                            InitNewGame();
                        }
                        else {
                            bGaming = !bGaming;
                        }
                    }

                    event.preventDefault();
                }

                function InitNewGame() {
                    oDirection = { x: 0, y: 1};
                    aoSnakePoints = [ { x:15,y:15}, { x:16,y:15}, { x:17,y:15}, { x:18,y:15}, { x:19,y:15}, { x:20,y:15}, { x:20,y:14}, { x:20,y:13},
                        { x:20,y:12}, { x:20,y:11}, { x:20,y:10}, { x:20,y:9}, { x:20,y:8}, { x:20,y:7}, { x:19,y:7}, { x:18,y:7}, { x:17,y:7}, { x:16,y:7},
                        { x:15,y:7}, { x:14,y:7}, { x:13,y:7}, { x:12,y:7}, { x:12,y:8}, { x:12,y:9}, { x:12,y:10}, { x:12,y:11},{ x:12,y:12}, { x:12,y:13},
                        { x:12,y:14}, { x:12,y:15}];
                    iPoints = 0;
                    oGoalPoint = GetGoalPoint();
                    iSlowLimit = 10;
                }

            }
        }
    }]);