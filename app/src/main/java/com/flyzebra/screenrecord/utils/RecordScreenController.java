package com.flyzebra.screenrecord.utils;

public interface RecordScreenController extends CallbackController<RecordScreenController.RecordScreenListener> {

    void setRecordScreen(boolean newState);

    boolean isEnabled();

    void setRecordScreenController(boolean enabled);

    boolean getRecordScreenController();

    void addCallback(RecordScreenListener callback);

    void removeCallback(RecordScreenListener callback);

    boolean isAvailable();


    public interface RecordScreenListener {

        /**
         * Called when the flashlight was turned off or on.
         *
         * @param enabled true if the flashlight is currently turned on.
         */
        void onRecordScreenChanged(boolean enabled);
    }
}
