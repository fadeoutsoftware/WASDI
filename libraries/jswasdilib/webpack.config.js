const path = require('path');
const webpack = require('webpack');
const PrettierPlugin = require("prettier-webpack-plugin");
const TerserPlugin = require('terser-webpack-plugin');
const getPackageJson = require('./scripts/getPackageJson');



const {
  version,
  name,
  license,
  repository,
  author,
} = getPackageJson('version', 'name', 'license', 'repository', 'author');

const banner = `
  ${name} v${version}
  ${repository.url}

  Copyright (c) ${author.replace(/ *<[^)]*> */g, " ")} and project contributors.

  This source code is licensed under the ${license} license found in the
  LICENSE file in the root directory of this source tree.
`;
// Export of module, with types 
let TsExport = {
  mode: "production",
  devtool: 'source-map',
  entry: './src/lib/index.ts',
  output: {
    filename: 'wasdi-module.js',
    path: path.resolve(__dirname, 'build'),
    library: "wasdi",
    libraryTarget: 'umd',
    clean: true,
    globalObject: 'this' // This line was missing
  },
  optimization: {
    minimize: false,
    minimizer: [
      new TerserPlugin({ extractComments: false }),
    ],
  },
  module: {
    rules: [
      {
        test: /\.(m|j|t)s$/,
        exclude: /(node_modules|bower_components)/,
        use: {
          loader: 'babel-loader'
        }
      }
    ]
  },
  plugins: [
    new PrettierPlugin(),
    new webpack.BannerPlugin(banner)
  ],
  resolve: {
    extensions: ['.ts', '.js', '.json']
  }
}

// Export of vanilla JS 
let JsExport = {
  mode: "production",
  devtool: 'source-map',
  entry: './src/lib/index.ts',
  output: {
    filename: 'wasdi.js',
    path: path.resolve(__dirname, 'build'),
    library: {                                                                                                                                                                       
      type: "window",                                                                                                                                                                
    },  
    
  },
  optimization: {
    minimize: true,
    minimizer: [
      new TerserPlugin({ extractComments: false }),
    ],
  },
  module: {
    rules: [
      {
        test: /\.(m|j|t)s$/,
        exclude: /(node_modules|bower_components)/,
        use: {
          loader: 'babel-loader'
        }
      }
    ]
  },
  plugins: [
    new PrettierPlugin(),
    new webpack.BannerPlugin(banner),
  ],



  resolve: {
    extensions: ['.ts', '.js', '.json']
  }
}



module.exports = [TsExport,JsExport];