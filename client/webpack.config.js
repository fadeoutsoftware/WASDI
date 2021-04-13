const HtmlWebpackPlugin = require("html-webpack-plugin");
const CopyPlugin = require("copy-webpack-plugin");
const path = require("path");
var asControllerList = ["./app/controllers/HomeController.js",
    "./app/controllers/EditorController.js",
    "./app/controllers/CatalogController.js",
    "./app/controllers/ImportController.js",
    "./app/controllers/MarketPlaceController.js",
    "./app/controllers/RootController.js",
    "./app/controllers/SearchOrbitController.js",
    "./app/controllers/ValidateUserController.js",
    "./app/controllers/WasdiApplicationDetailsController.js",
    "./app/controllers/WasdiApplicationUIController.js",
    "./app/controllers/WorkspaceController.js",
];
// This part contains the search service
var asSearchService = ["./app/services/search/advanced-filter-service.js",
    "./app/services/search/advanced-search-service.js",
    "./app/services/search/search-service.js",
    "./app/services/search/light-search-service.js"
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
            import: './app/services/search/searchServices.js',
            dependOn: ['app']

        },

        directives: {
            import: './directives.js',
            dependOn: ['vendor']

        },
        bundle: './index.js',
        /*    services: {
                import: './app/services.js'
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
                {from: "./app/assets", to: "./app/assets"},
                {from: "./app/config", to: "./app/config"},
                {from: "./app/css", to: "./app/css"},
                /*{from: "./app/controllers", to: "./app/controllers"},*/
                {from: "./app/dialogs", to: "./app/dialogs"},
                {from: "./app/directives", to: "./app/directives"},
                {from: "./app/fonts", to: "./app/fonts"},
                {from: "./app/environments", to: "./app/environments"},

                {from: "./app/languages", to: "./app/languages"},
                {from: "./app/models", to: "./app/models"},
                {from: "./app/partials", to: "./app/partials"},
                {from: "./app/lib", to: "./app/lib"},
                {from: "./app/keycloak.json", to: "./app/keycloak.json"},
                {from: "./app/services", to: "./app/services"},
                {from: "./app/favicon.ico", to: "./app/favicon.ico"},

            ],
        })
    ],
    optimization: {
        runtimeChunk: 'single',
    },
}
;
