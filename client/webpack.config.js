const HtmlWebpackPlugin = require("html-webpack-plugin");
const CopyPlugin = require("copy-webpack-plugin");
const path = require("path");
var asControllerList = ["./controllers/HomeController.js",
    "./controllers/EditorController.js",
    "./controllers/CatalogController.js",
    "./controllers/ImportController.js",
    "./controllers/MarketPlaceController.js",
    "./controllers/RootController.js",
    "./controllers/SearchOrbitController.js",
    "./controllers/ValidateUserController.js",
    "./controllers/WasdiApplicationDetailsController.js",
    "./controllers/WasdiApplicationUIController.js",
    "./controllers/WorkspaceController.js",
];
// This part contains the search service
var asSearchService = ["./services/search/advanced-filter-service.js",
    "./services/search/advanced-search-service.js",
    "./services/search/search-service.js",
    "./services/search/light-search-service.js"
];


module.exports = {
    mode: 'development',
    entry: {
        // in this file are required all dependencies from external source
        // served through node_modules or libs
        vendor: {
            import: './vendor.js'
        },
        // this entry report all controllers
        // each controller must be declared inside the window object in order to be
        // initialized as module by app.js
        controllers: {
            import:'./controllers.js',
            dependOn: ['vendor'],

        },
        app: {
            import: './app.js',
            dependOn: ['controllers','vendor'],
        },
        // this entry report the modules that use factory() so that
        // must be declared after app.js
        search: {
            import: './services/search/searchServices.js',
            dependOn: ['app']

        },

        directives: {
            import: './directives.js',
            dependOn: ['vendor']

        },
        bundle: './index.js',
        /*    services: {
                import: './services.js'
            },

            controllers: {
                import: asControllerList,
                dependOn: ['services'],
            },



            searchservices: {
                import: asSearchService, // this are the factory methods that relies on wasdi module
                dependOn: 'app'
            },*/

    },
    module: {
        rules: [
            {
                test: /\.css$/i,
                use: ["style-loader", "css-loader"],
            },
            {
                test: /\.(woff(2)?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
                use: [
                    {
                        loader: 'file-loader',
                        options: {
                            name: '[name].[ext]',
                            outputPath: 'fonts/'
                        }
                    }
                ]
            },
            {
                test: /\.(png|gif|TTF|jpg|JPG)(\?v=\d+\.\d+\.\d+)?$/,
                use: [
                    {
                        loader: 'file-loader',
                        options: {
                            name: '[name].[ext]',
                            outputPath: 'assets/'
                        }
                    }
                ]
            }
        ],
    },
    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: '[name].js',
        clean: true
    },
    plugins: [
        new HtmlWebpackPlugin({
            template: path.resolve(__dirname, ".", "index-optimized.html")
        }),
        new CopyPlugin({
            patterns: [
                {from: "./assets", to: "./assets"},
                {from: "./config", to: "./config"},
                {from: "./css", to: "./css"},
                /*{from: "./controllers", to: "./controllers"},*/
                {from: "./dialogs", to: "./dialogs"},
                {from: "./directives", to: "./directives"},
                {from: "./fonts", to: "./fonts"},
                {from: "./environments", to: "./environments"},

                {from: "./languages", to: "./languages"},
                {from: "./models", to: "./models"},
                {from: "./partials", to: "./partials"},
                {from: "./lib", to: "./lib"},
                {from: "./keycloak.json", to: "./keycloak.json"},
                {from: "./services", to: "./services"},
                {from: "./favicon.ico", to: "./favicon.ico"},

            ],
        })
    ],
    optimization: {
        runtimeChunk: 'single',
    },
}
;
