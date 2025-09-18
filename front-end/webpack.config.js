const NodePolyfillPlugin = require('node-polyfill-webpack-plugin');

module.exports = {
  plugins: [
    new NodePolyfillPlugin()
  ],
  resolve: {
    fallback: {
      "process": require.resolve("process/browser"),
      "util": require.resolve("util/")
    }
  }
};
