import { createRequire } from 'module';const require = createRequire(import.meta.url);
import {
  Animation,
  AnimationDriver,
  AnimationEngine,
  AnimationRenderer,
  AnimationRendererFactory,
  AnimationStyleNormalizer,
  BaseAnimationRenderer,
  ENTER_CLASSNAME,
  LEAVE_CLASSNAME,
  NoopAnimationDriver,
  NoopAnimationStyleNormalizer,
  TransitionAnimationPlayer,
  WebAnimationsDriver,
  WebAnimationsPlayer,
  WebAnimationsStyleNormalizer,
  allowPreviousPlayerStylesMerge,
  camelCaseToDashCase,
  containsElement,
  createEngine,
  getParentElement,
  invokeQuery,
  normalizeKeyframes,
  validateStyleProperty,
  validateWebAnimatableStyleProperty
} from "./chunk-ZGEZKDH7.js";
import "./chunk-WTZZ57D2.js";
import "./chunk-4W6I5BHJ.js";
import "./chunk-WGRCPX6P.js";
import "./chunk-YHCV7DAQ.js";
export {
  AnimationDriver,
  NoopAnimationDriver,
  Animation as ɵAnimation,
  AnimationEngine as ɵAnimationEngine,
  AnimationRenderer as ɵAnimationRenderer,
  AnimationRendererFactory as ɵAnimationRendererFactory,
  AnimationStyleNormalizer as ɵAnimationStyleNormalizer,
  BaseAnimationRenderer as ɵBaseAnimationRenderer,
  ENTER_CLASSNAME as ɵENTER_CLASSNAME,
  LEAVE_CLASSNAME as ɵLEAVE_CLASSNAME,
  NoopAnimationStyleNormalizer as ɵNoopAnimationStyleNormalizer,
  TransitionAnimationPlayer as ɵTransitionAnimationPlayer,
  WebAnimationsDriver as ɵWebAnimationsDriver,
  WebAnimationsPlayer as ɵWebAnimationsPlayer,
  WebAnimationsStyleNormalizer as ɵWebAnimationsStyleNormalizer,
  allowPreviousPlayerStylesMerge as ɵallowPreviousPlayerStylesMerge,
  camelCaseToDashCase as ɵcamelCaseToDashCase,
  containsElement as ɵcontainsElement,
  createEngine as ɵcreateEngine,
  getParentElement as ɵgetParentElement,
  invokeQuery as ɵinvokeQuery,
  normalizeKeyframes as ɵnormalizeKeyframes,
  validateStyleProperty as ɵvalidateStyleProperty,
  validateWebAnimatableStyleProperty as ɵvalidateWebAnimatableStyleProperty
};
