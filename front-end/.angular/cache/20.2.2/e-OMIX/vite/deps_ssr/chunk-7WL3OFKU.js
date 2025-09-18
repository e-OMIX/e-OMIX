import { createRequire } from 'module';const require = createRequire(import.meta.url);
import {
  require_cjs
} from "./chunk-WGRCPX6P.js";
import {
  __toESM
} from "./chunk-YHCV7DAQ.js";

// node_modules/@angular/cdk/fesm2022/data-source.mjs
var import_rxjs = __toESM(require_cjs(), 1);
var DataSource = class {
};
function isDataSource(value) {
  return value && typeof value.connect === "function" && !(value instanceof import_rxjs.ConnectableObservable);
}

export {
  DataSource,
  isDataSource
};
//# sourceMappingURL=chunk-7WL3OFKU.js.map
