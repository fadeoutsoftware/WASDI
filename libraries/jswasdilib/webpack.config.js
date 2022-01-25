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
    },
    resolve: {
        fallback: {
            "stream": require.resolve("stream-browserify"),
            "http": require.resolve("stream-http"),
            "url": require.resolve("url/"),
            "https": require.resolve("https-browserify"),
            "zlib": require.resolve("browserify-zlib"),
            "buffer": require.resolve("buffer/"),
            "util": require.resolve("util/"),
            "assert": require.resolve("assert/")

        }
    }
};