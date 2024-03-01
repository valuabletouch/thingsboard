import { IframeState } from './iframe.models';
import { IframeActions, IframeActionTypes } from './iframe.actions';

export const initialState: IframeState = {
  value: null
};

export function iframeReducer(
  state: IframeState = initialState,
  action: IframeActions
): IframeState {
  switch (action.type) {
    case IframeActionTypes.SET_IFRAME:
      return { ...state, value: action.value };
    default:
      return state;
  }
}
