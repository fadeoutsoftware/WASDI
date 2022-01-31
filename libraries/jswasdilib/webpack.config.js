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
    }
};