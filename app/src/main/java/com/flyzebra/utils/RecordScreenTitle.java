//package com.flyzebra.screenrecord;
//
//import android.app.ActivityManager;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.service.quicksettings.Tile;
//import android.util.Log;
//import android.widget.Switch;
//
//import com.android.systemui.Dependency;
//import com.android.systemui.plugins.qs.QSTile.BooleanState;
//import com.android.systemui.qs.QSHost;
//import com.android.systemui.qs.tileimpl.QSTileImpl;
//
///**
// * Quick settings tile: Control RecordScreen
// **/
//public class RecordScreenTitle extends QSTileImpl<BooleanState> implements
//        RecordScreenController.RecordScreenListener {
//
//    private final String TAG = "RecordScreenTitle";
//    private final Icon startIcon = ResourceIcon.get(R.drawable.ic_start_record_screen);
//    private final Icon stopIcon = ResourceIcon.get(R.drawable.ic_stop_record_screen);
//
//    private final RecordScreenController mRecordScreenController;
//    private ServiceConnection mServiceConnection;
//    private BooleanState mState;
//
//    public RecordScreenTitle(QSHost host) {
//        super(host);
//        mRecordScreenController = Dependency.get(RecordScreenController.class);
//    }
//
//    @Override
//    protected void handleDestroy() {
//        super.handleDestroy();
//    }
//
//    @Override
//    public BooleanState newTileState() {
//        return new BooleanState();
//    }
//
//    @Override
//    public void handleSetListening(boolean listening) {
//        if (listening) {
//            mRecordScreenController.addCallback(this);
//        } else {
//            mRecordScreenController.removeCallback(this);
//        }
//    }
//
//    @Override
//    protected void handleUserSwitch(int newUserId) {
//    }
//
//    @Override
//    public Intent getLongClickIntent() {
//        return new Intent();
//    }
//
//    @Override
//    protected void handleClick() {
//        if (ActivityManager.isUserAMonkey()) {
//            return;
//        }
//        boolean newState = !mState.value;
//        mRecordScreenController.setRecordScreenController(newState);
//        refreshState(newState);
//        Log.d(TAG, "handleClick newState = " + newState);
//
//    }
//
//    @Override
//    public CharSequence getTileLabel() {
//        return mContext.getString(R.string.quick_settings_record_screen_label);
//    }
//
//    @Override
//    public int getMetricsCategory() {
//        return 0;
//    }
//
//    @Override
//    protected void handleLongClick() {
//        handleClick();
//    }
//
//    @Override
//    protected void handleUpdateState(BooleanState state, Object arg) {
//        mState = state;
//        if (state.slash == null) {
//            state.slash = new SlashState();
//        }
//        state.label = mHost.getContext().getString(R.string.quick_settings_record_screen_label);
//        if (!mRecordScreenController.isAvailable()) {
//            state.icon = startIcon;
//            state.slash.isSlashed = true;
//            state.contentDescription = mContext.getString(
//                    R.string.accessibility_quick_settings_record_screen_unavailable);
//            state.state = Tile.STATE_UNAVAILABLE;
//            return;
//        }
//        boolean value = ScreenUtil.isRecordScreen();
//        if (value) {
//            state.label = mContext.getString(R.string.accessibility_quick_settings_record_screen_changed_off);
//            state.icon = stopIcon;
//        } else {
//            state.label = mContext.getString(R.string.accessibility_quick_settings_record_screen_changed_on);
//            state.icon = startIcon;
//        }
//        state.value = !value;
//        state.slash.isSlashed = !state.value;
//        state.contentDescription = mContext.getString(R.string.quick_settings_record_screen_label);
//        state.expandedAccessibilityClassName = Switch.class.getName();
//        state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
//        Log.d(TAG, "handleUpdateState  "  + " state.value = " + state.value);
//    }
//
//    @Override
//    protected String composeChangeAnnouncement() {
//        if (mState.value) {
//            return mContext.getString(R.string.accessibility_quick_settings_record_screen_changed_on);
//        } else {
//            return mContext.getString(R.string.accessibility_quick_settings_record_screen_changed_off);
//        }
//    }
//
//    @Override
//    public void onRecordScreenChanged(boolean enabled) {
//        refreshState(enabled);
//    }
//
//
//}
