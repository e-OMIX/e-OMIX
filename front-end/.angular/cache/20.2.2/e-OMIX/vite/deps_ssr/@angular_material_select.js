import { createRequire } from 'module';const require = createRequire(import.meta.url);
import {
  MAT_SELECT_CONFIG,
  MAT_SELECT_SCROLL_STRATEGY,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER_FACTORY,
  MAT_SELECT_TRIGGER,
  MatSelect,
  MatSelectChange,
  MatSelectModule,
  MatSelectTrigger
} from "./chunk-UVG3YU6G.js";
import "./chunk-N2MI6WYT.js";
import "./chunk-TVKDXQVV.js";
import "./chunk-SQQBE4TX.js";
import "./chunk-MSZFXFQV.js";
import "./chunk-XEO7G62X.js";
import {
  MatError,
  MatFormField,
  MatHint,
  MatLabel,
  MatPrefix,
  MatSuffix
} from "./chunk-SECKD6YH.js";
import "./chunk-G3O7AQTP.js";
import "./chunk-DR5ABKLT.js";
import "./chunk-PD33PZZQ.js";
import "./chunk-XCVTQQ2N.js";
import {
  MatOptgroup,
  MatOption
} from "./chunk-5JAVQZ4Y.js";
import "./chunk-RK4BOAT6.js";
import "./chunk-KPXPBEQG.js";
import "./chunk-YDPUAAA3.js";
import "./chunk-FY5FI4I3.js";
import "./chunk-ZOAG5BQY.js";
import "./chunk-YRCMWSUF.js";
import "./chunk-BFUJK5PP.js";
import "./chunk-5XYFHA5V.js";
import "./chunk-PYJ7FLI5.js";
import "./chunk-TEYZEDDL.js";
import "./chunk-UZQXUDE5.js";
import "./chunk-Z3QS4VDI.js";
import "./chunk-WRWIFK3K.js";
import "./chunk-JAJGLRA6.js";
import "./chunk-ZCPTMQPA.js";
import "./chunk-K3LPVQVO.js";
import "./chunk-NCSGHYGZ.js";
import "./chunk-LOLUTUV5.js";
import "./chunk-FQL6O3MQ.js";
import "./chunk-UJA2PO5W.js";
import "./chunk-O5SOX6PJ.js";
import "./chunk-WKY562PQ.js";
import "./chunk-N6JPIGK3.js";
import "./chunk-7WL3OFKU.js";
import "./chunk-IFSIVQEV.js";
import "./chunk-XZESSXJ7.js";
import "./chunk-U4VNP6SC.js";
import {
  require_operators
} from "./chunk-4W6I5BHJ.js";
import {
  require_cjs
} from "./chunk-WGRCPX6P.js";
import {
  __toESM
} from "./chunk-YHCV7DAQ.js";

// node_modules/@angular/material/fesm2022/select.mjs
var import_rxjs = __toESM(require_cjs(), 1);
var import_operators = __toESM(require_operators(), 1);
var matSelectAnimations = {
  // Represents
  // trigger('transformPanel', [
  //   state(
  //     'void',
  //     style({
  //       opacity: 0,
  //       transform: 'scale(1, 0.8)',
  //     }),
  //   ),
  //   transition(
  //     'void => showing',
  //     animate(
  //       '120ms cubic-bezier(0, 0, 0.2, 1)',
  //       style({
  //         opacity: 1,
  //         transform: 'scale(1, 1)',
  //       }),
  //     ),
  //   ),
  //   transition('* => void', animate('100ms linear', style({opacity: 0}))),
  // ])
  /** This animation transforms the select's overlay panel on and off the page. */
  transformPanel: {
    type: 7,
    name: "transformPanel",
    definitions: [
      {
        type: 0,
        name: "void",
        styles: {
          type: 6,
          styles: { opacity: 0, transform: "scale(1, 0.8)" },
          offset: null
        }
      },
      {
        type: 1,
        expr: "void => showing",
        animation: {
          type: 4,
          styles: {
            type: 6,
            styles: { opacity: 1, transform: "scale(1, 1)" },
            offset: null
          },
          timings: "120ms cubic-bezier(0, 0, 0.2, 1)"
        },
        options: null
      },
      {
        type: 1,
        expr: "* => void",
        animation: {
          type: 4,
          styles: { type: 6, styles: { opacity: 0 }, offset: null },
          timings: "100ms linear"
        },
        options: null
      }
    ],
    options: {}
  }
};
export {
  MAT_SELECT_CONFIG,
  MAT_SELECT_SCROLL_STRATEGY,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER,
  MAT_SELECT_SCROLL_STRATEGY_PROVIDER_FACTORY,
  MAT_SELECT_TRIGGER,
  MatError,
  MatFormField,
  MatHint,
  MatLabel,
  MatOptgroup,
  MatOption,
  MatPrefix,
  MatSelect,
  MatSelectChange,
  MatSelectModule,
  MatSelectTrigger,
  MatSuffix,
  matSelectAnimations
};
//# sourceMappingURL=@angular_material_select.js.map
