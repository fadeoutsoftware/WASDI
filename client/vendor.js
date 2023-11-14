require('./app/lib/leaflet-mouseposition/Leaflet.MousePosition-master/src/L.Control.MousePosition.js');
require('./app/lib/leafletbetterWMS/L.TileLayer.BetterWMS.js');
require('./app/lib/leafletControlCustomMaster/Leaflet.Control.Custom.js');
require('./app/filters/stringUtils/removeSpaces.js');
require('./app/filters/stringUtils/formatFileSize.js');
require('./app/filters/stringUtils/formatTime.js');

require('./app/lib/enum/ProductsProviders.js'); // also unused?

require('./app/assets/slider-appdetails/js/lightslider.js');
require('./node_modules/leaflet-draw/dist/leaflet.draw.js');
require('./node_modules/jwt-decode/build/jwt-decode.min.js');

require('./node_modules/angular-translate/dist/angular-translate.min.js');
require('./node_modules/angular-translate-loader-static-files/angular-translate-loader-static-files.min.js');
require('./node_modules/angular-ui-bootstrap/dist/ui-bootstrap-tpls.js');
require('./node_modules/wms-capabilities/dist/wms-capabilities.min.js');
require('./node_modules/ng-file-upload/dist/ng-file-upload.min.js');
require('./node_modules/angular-colorpicker-directive/color-picker.min.js');
require('./node_modules/angular-moment-picker/src/angular-moment-picker.js');
//require('./node_modules/plotly.js/dist/plotly');

require('./node_modules/angularjs-slider/dist/rzslider.min.js');
//require('./node_modules/jstree/dist/jstree.min.js');
require('./node_modules/jstree/src/jstree.contextmenu.js');
require('./node_modules/angular-modal-service/dst/angular-modal-service.min.js');

// new method to solve global variables issue

import showdow from './node_modules/showdown/dist/showdown.js';

global.showdown = showdow;

/*
Removed due to ununsed old version generating errors on npm automation
import Plotly from './node_modules/plotly.js/dist/plotly.min.js';

global.Plotly = Plotly;*/

import moment from './node_modules/moment/moment.js';

global.moment = moment;

/*import jstree from './node_modules/jstree/dist/jstree.min.js';
global.$.jstree = jstree;
import 'jstree';
*/




