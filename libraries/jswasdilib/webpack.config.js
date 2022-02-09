const path = require('path');
const webpack = require('webpack');

module.exports = {
    entry: './src/index.ts',
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                loader: 'ts-loader',
                exclude: /node_modules/
            }
        ]
    },
    target: 'web',
    mode: 'production',
    output: {
        filename: 'wasdi.js',
        clean: true,
        path: path.resolve(__dirname, 'dist'),
        library: {
            name: 'wasdi',
            type: 'umd'
        },

        libraryTarget: 'var'
    },
    resolve: {
        extensions: ['.tsx', '.ts', '.js'],
    },
    /*resolve :{
        fallback: {
            "child_process" : require.resolve("child_process"),
            "fs" : false,
            "http": require.resolve("stream-http"),
            "https": require.resolve("https-browserify")
        }
    }*/
};