const HtmlWebpackPlugin = require("html-webpack-plugin");
const CopyPlugin = require("copy-webpack-plugin");
const path = require("path");

module.exports = {
    mode: 'production',
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
        filename: '[name].js',
        clean: true
    },
    plugins: [
        new HtmlWebpackPlugin({
            template: path.resolve(__dirname, ".", "index-optimized.html")
        }),
        new CopyPlugin({
            patterns: [
                {from: "./app/assets", to: "./assets"},
                {from: "./app/config", to: "./config"},
                {from: "./app/dialogs", to: "./dialogs"},
                {from: "./app/directives", to: "./directives"},
                /*{from: "./app/fonts", to: "./fonts"},*/
                {from: "./app/environments", to: "./environments"},
                {from: "./app/languages", to: "./languages"},
                {from: "./app/models", to: "./models"},
                {from: "./app/partials", to: "./partials"},
                {from: "./app/lib", to: "./lib"},
                {from: "./app/keycloak.json", to: "./keycloak.json"},
                /*{from: "./app/services", to: "./services"},*/
                {from: "./app/favicon.ico", to: "./favicon.ico"},

            ],
        })
    ],
    optimization: {
        runtimeChunk: 'single',
        // minimization reduce the ./dist folder footprint of just a mere 3 MB over 45MB
        // but generates some error for modules created using facototy() from angular
        // so for production mode is now disabled
        minimize: false
    },
}
;
