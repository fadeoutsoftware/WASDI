const path = require('path');
const webpack = require('webpack');

module.exports = {
    entry: './src/index.js',
    target: 'web',
    mode : 'development',
    output: {
        filename: 'wasdi.js',
        clean: true,
        path: path.resolve(__dirname, 'dist'),
        library: {
            name: 'wasdi',
            type: 'umd',

        },
        libraryTarget: 'var'
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