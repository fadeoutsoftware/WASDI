const path = require('path');
const webpack = require('webpack');

module.exports = {
    entry: './src/index.js',
    target: 'web',
    mode : 'development',
    output: {
        filename: 'main.js',
        clean: true,
        path: path.resolve(__dirname, 'dist'),
    }
};