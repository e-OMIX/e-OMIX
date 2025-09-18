import { createRequire } from 'module';const require = createRequire(import.meta.url);
import {
  MatTooltipModule
} from "./chunk-WRWGCKFS.js";
import {
  MAT_TOOLTIP_DEFAULT_OPTIONS,
  MAT_TOOLTIP_DEFAULT_OPTIONS_FACTORY,
  MAT_TOOLTIP_SCROLL_STRATEGY,
  MAT_TOOLTIP_SCROLL_STRATEGY_FACTORY,
  MAT_TOOLTIP_SCROLL_STRATEGY_FACTORY_PROVIDER,
  MatTooltip,
  SCROLL_THROTTLE_MS,
  TOOLTIP_PANEL_CLASS,
  TooltipComponent,
  getMatTooltipInvalidPositionError
} from "./chunk-XCRZG6YE.js";
import "./chunk-DR5ABKLT.js";
import "./chunk-PD33PZZQ.js";
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

// node_modules/@angular/material/fesm2022/tooltip.mjs
var import_operators = __toESM(require_operators(), 1);
var import_rxjs = __toESM(require_cjs(), 1);
var matTooltipAnimations = {
  // Represents:
  // trigger('state', [
  //   state('initial, void, hidden', style({opacity: 0, transform: 'scale(0.8)'})),
  //   state('visible', style({transform: 'scale(1)'})),
  //   transition('* => visible', animate('150ms cubic-bezier(0, 0, 0.2, 1)')),
  //   transition('* => hidden', animate('75ms cubic-bezier(0.4, 0, 1, 1)')),
  // ])
  /** Animation that transitions a tooltip in and out. */
  tooltipState: {
    type: 7,
    name: "state",
    definitions: [
      {
        type: 0,
        name: "initial, void, hidden",
        styles: { type: 6, styles: { opacity: 0, transform: "scale(0.8)" }, offset: null }
      },
      {
        type: 0,
        name: "visible",
        styles: { type: 6, styles: { transform: "scale(1)" }, offset: null }
      },
      {
        type: 1,
        expr: "* => visible",
        animation: { type: 4, styles: null, timings: "150ms cubic-bezier(0, 0, 0.2, 1)" },
        options: null
      },
      {
        type: 1,
        expr: "* => hidden",
        animation: { type: 4, styles: null, timings: "75ms cubic-bezier(0.4, 0, 1, 1)" },
        options: null
      }
    ],
    options: {}
  }
};
export {
  MAT_TOOLTIP_DEFAULT_OPTIONS,
  MAT_TOOLTIP_DEFAULT_OPTIONS_FACTORY,
  MAT_TOOLTIP_SCROLL_STRATEGY,
  MAT_TOOLTIP_SCROLL_STRATEGY_FACTORY,
  MAT_TOOLTIP_SCROLL_STRATEGY_FACTORY_PROVIDER,
  MatTooltip,
  MatTooltipModule,
  SCROLL_THROTTLE_MS,
  TOOLTIP_PANEL_CLASS,
  TooltipComponent,
  getMatTooltipInvalidPositionError,
  matTooltipAnimations
};
//# sourceMappingURL=@angular_material_tooltip.js.map
