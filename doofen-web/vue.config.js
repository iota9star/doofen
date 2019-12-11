const path = require('path');
const webpack = require('webpack');

function resolve(dir) {
  return path.join(__dirname, dir);
}

const vueConfig = {
  outputDir: '../doofen-server/resources/views',
  filenameHashing: false,
  devServer: {
    port: 50000,
    disableHostCheck: true,
    proxy: 'http://localhost:80'
  },
  configureWebpack: {
    plugins: [
      // Ignore all locale files of moment.js
      new webpack.IgnorePlugin(/^\.\/locale$/, /moment$/)
    ]
  },
  chainWebpack: (config) => {
    config.resolve.alias
      .set('@$', resolve('src'));
  },
  css: {
    loaderOptions: {
      less: {
        modifyVars: {
          /* less 变量覆盖，用于自定义 ant design 主题 */
          // 'primary-color': '#181759',
          // 'link-color': '#181759',
          'border-radius-base': '0'
        },
        javascriptEnabled: true
      }
    }
  },
  // disable source map in production
  productionSourceMap: false,
  lintOnSave: undefined,
  // babel-loader no-ignore node_modules/*
  transpileDependencies: []
};

// vue.config.js
module.exports = vueConfig;
