import { createRequire } from 'module';const require = createRequire(import.meta.url);
import {
  MatFormFieldModule
} from "./chunk-N2MI6WYT.js";
import {
  MAT_ERROR,
  MAT_FORM_FIELD,
  MAT_FORM_FIELD_DEFAULT_OPTIONS,
  MAT_PREFIX,
  MAT_SUFFIX,
  MatError,
  MatFormField,
  MatFormFieldControl,
  MatHint,
  MatLabel,
  MatPrefix,
  MatSuffix,
  getMatFormFieldDuplicatedHintError,
  getMatFormFieldMissingControlError,
  getMatFormFieldPlaceholderConflictError
} from "./chunk-SECKD6YH.js";
import "./chunk-G3O7AQTP.js";
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
import "./chunk-FQL6O3MQ.js";
import "./chunk-UJA2PO5W.js";
import "./chunk-O5SOX6PJ.js";
import "./chunk-WKY562PQ.js";
import "./chunk-N6JPIGK3.js";
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

// node_modules/@angular/material/fesm2022/form-field.mjs
var import_rxjs = __toESM(require_cjs(), 1);
var import_operators = __toESM(require_operators(), 1);
var matFormFieldAnimations = {
  // Represents:
  // trigger('transitionMessages', [
  //   // TODO(mmalerba): Use angular animations for label animation as well.
  //   state('enter', style({opacity: 1, transform: 'translateY(0%)'})),
  //   transition('void => enter', [
  //     style({opacity: 0, transform: 'translateY(-5px)'}),
  //     animate('300ms cubic-bezier(0.55, 0, 0.55, 0.2)'),
  //   ]),
  // ])
  /** Animation that transitions the form field's error and hint messages. */
  transitionMessages: {
    type: 7,
    name: "transitionMessages",
    definitions: [
      {
        type: 0,
        name: "enter",
        styles: {
          type: 6,
          styles: { opacity: 1, transform: "translateY(0%)" },
          offset: null
        }
      },
      {
        type: 1,
        expr: "void => enter",
        animation: [
          { type: 6, styles: { opacity: 0, transform: "translateY(-5px)" }, offset: null },
          { type: 4, styles: null, timings: "300ms cubic-bezier(0.55, 0, 0.55, 0.2)" }
        ],
        options: null
      }
    ],
    options: {}
  }
};
export {
  MAT_ERROR,
  MAT_FORM_FIELD,
  MAT_FORM_FIELD_DEFAULT_OPTIONS,
  MAT_PREFIX,
  MAT_SUFFIX,
  MatError,
  MatFormField,
  MatFormFieldControl,
  MatFormFieldModule,
  MatHint,
  MatLabel,
  MatPrefix,
  MatSuffix,
  getMatFormFieldDuplicatedHintError,
  getMatFormFieldMissingControlError,
  getMatFormFieldPlaceholderConflictError,
  matFormFieldAnimations
};
//# sourceMappingURL=@angular_material_form-field.js.map
