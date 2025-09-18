import {
  MatTooltipModule
} from "./chunk-JMBK67RQ.js";
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
} from "./chunk-BUKMRUHS.js";
import "./chunk-JEWVAH47.js";
import "./chunk-AWORGRTI.js";
import "./chunk-IQBNUTOP.js";
import "./chunk-OX3NRC6A.js";
import "./chunk-VENV3F3G.js";
import "./chunk-L2BZS5YT.js";
import "./chunk-EDCYGRC5.js";
import "./chunk-Q3LWZEU3.js";
import "./chunk-VXQMTBHK.js";
import "./chunk-ATUXWALT.js";
import "./chunk-Q73LLBSM.js";
import "./chunk-SLBB6ILP.js";
import "./chunk-2ZKSKDON.js";
import "./chunk-UIQGGOYN.js";
import "./chunk-MSAGNNJB.js";
import "./chunk-7UJZXIJQ.js";
import "./chunk-Z4WX5I27.js";
import "./chunk-D5TD4WC3.js";
import "./chunk-WHYJS52J.js";
import "./chunk-TYINGCUB.js";
import "./chunk-P3OIMHUY.js";
import "./chunk-2U34DP7Y.js";
import "./chunk-GS7EAZJH.js";
import "./chunk-OUSM42MY.js";
import "./chunk-S4JUVB6R.js";
import "./chunk-RSS3ODKE.js";
import "./chunk-TXDUYLVM.js";

// node_modules/@angular/material/fesm2022/tooltip.mjs
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
