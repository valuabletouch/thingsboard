import { Action } from '@ngrx/store';

export enum IframeActionTypes {
  SET_IFRAME = '[Iframe] Set'
}

export class ActionSetIframe implements Action {
  readonly type = IframeActionTypes.SET_IFRAME;

  constructor(readonly value: boolean) {}
}

export type IframeActions =
  | ActionSetIframe;
