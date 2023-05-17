const HtmlWebpackPlugin = require("html-webpack-plugin");
const CopyPlugin = require("copy-webpack-plugin");
const CompressionPlugin = require("compression-webpack-plugin");
const TerserPlugin = require("terser-webpack-plugin");
const path = require("path");

module.exports = {
    // MODE :
    // "production" must be used to prepare the package for deploy
    mode: 'production',

    // "development" must be used for development and debug purpose
    // adding devtools initialized with "eval-source-map"
    // allows the developer to have the source bundled, available in the
    // Chrome debugger.
    // With this options enabled a mapping of the original sources are
    // bundled and allow step debugging, directly in the browser.
    /*
    mode: 'production',
    devtool : "eval-source-map",
     */
    entry: {
        // in this file are required all dependencies from external source
        // served through node_modules or libs
        vendor: {
            import: './vendor.js'
        },
        // this entry includes all the services declared in wasdi.
        // Each service must declare a global variable under
        // window scope, in order to work
        services:{
            import :'./services.js'
        },
        // this entry include all controllers
        // each controller must be declared inside the window object in order to be
        // initialized as module by app.js
        controllers: {
            import:'./controllers.js',
            dependOn: ['vendor','services'],

        },
        // Watch out for this one the app.js used was required to be migrated,
        // excluding some unused js controllers and services.
        // This file resides in /client directory and is the one used
        // to declare the app and its dependencies.
        // The legacy one is in /client/app !
        app: {
            import: './app.js',
            dependOn: ['controllers','vendor'],
        },
        // this entry report the modules that use factory() so that
        // must be declared after app.js
        search: {
            import: './app/services/search/searchServices.js',
            dependOn: ['app','bundle']

        },

        directives: {
            import: './directives.js',
            dependOn: ['vendor']

        },
        // Bundle entry contains CSS loaded throught style loader and
        // WASDI Services
        bundle: './index.js',
    },
    module: {
        rules: [
            {
                test: /\.css$/i,
                use: ["style-loader", "css-loader"],
            },
            // sass
            {
                test: /\.scss$/i,
                use: ["style-loader", "css-loader", "sass-loader"],
            },
            {// Fonts assets
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
            {// Images assets
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
        filename: '[name].[contenthash].js',
        clean: true
    },
    plugins: [
        new CompressionPlugin(
            {   
                test: /\.js(\?.*)?$/i,
              }
        ),
        new HtmlWebpackPlugin({
            template: path.resolve(__dirname, ".", "app/index-maintenance.html"),
            templateParameters: {
                KCURL : process.env.KEYCLOAK_URL
            }
        }),
        new CopyPlugin({
            patterns: [
                {from: "./app/assets", to: "./assets"},
                {from: "./app/config", to: "./config"},
                {from: "./app/dialogs", to: "./dialogs"},
                {from: "./app/directives", to: "./directives"},
                {from: "./app/environments", to: "./environments"},
                {from: "./app/languages", to: "./languages"},
                {from: "./app/models", to: "./models"},
                {from: "./app/partials", to: "./partials"},
                {from: "./app/lib", to: "./lib"},
                {from: "./app/keycloak.json", to: "./keycloak.json"},
                {from: "./app/favicon.ico", to: "./favicon.ico"},

            ],
        })
    ],
    optimization: {
        runtimeChunk: 'single',
        minimize: true,
        minimizer: [
            new TerserPlugin({
              parallel: true,
              terserOptions: {
                mangle: false,
                output: {
                  beautify: false
                }
              },
              
            }),
          ],
        // minimization reduce the ./dist folder footprint to just 30 MB over 45MB
        // but generates some error for modules created using factory() from angular
        // so for production mode is now disabled
        
    },
}
;
