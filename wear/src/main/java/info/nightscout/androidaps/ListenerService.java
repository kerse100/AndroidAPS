package info.nightscout.androidaps;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

/**
 * Created by emmablack on 12/26/14.
 */
public class ListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String WEARABLE_DATA_PATH = "/nightscout_watch_data";
    private static final String WEARABLE_RESEND_PATH = "/nightscout_watch_data_resend";
    private static final String WEARABLE_CANCELBOLUS_PATH = "/nightscout_watch_cancel_bolus";

    private static final String OPEN_SETTINGS = "/openwearsettings";
    private static final String NEW_STATUS_PATH = "/sendstatustowear";
    public static final String BASAL_DATA_PATH = "/nightscout_watch_basal";
    public static final String BOLUS_PROGRESS_PATH = "/nightscout_watch_bolusprogress";

    public static final int NOTIFICATION_ID = 001;

    private static final String ACTION_RESEND = "com.dexdrip.stephenblack.nightwatch.RESEND_DATA";
    private static final String ACTION_CANCELBOLUS = "com.dexdrip.stephenblack.nightwatch.CANCELBOLUS";

    private static final String ACTION_RESEND_BULK = "com.dexdrip.stephenblack.nightwatch.RESEND_BULK_DATA";
    GoogleApiClient googleApiClient;
    private long lastRequest = 0;


    public class DataRequester extends AsyncTask<Void, Void, Void> {
        Context mContext;

        DataRequester(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (googleApiClient.isConnected()) {
                if (System.currentTimeMillis() - lastRequest > 20 * 1000) { // enforce 20-second debounce period
                    lastRequest = System.currentTimeMillis();

                    NodeApi.GetConnectedNodesResult nodes =
                            Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                    for (Node node : nodes.getNodes()) {
                        Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), WEARABLE_RESEND_PATH, null);
                    }
                }
            } else
                googleApiClient.connect();
            return null;
        }
    }

    public class BolusCancelTask extends AsyncTask<Void, Void, Void> {
        Context mContext;

        BolusCancelTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (googleApiClient.isConnected()) {
                    NodeApi.GetConnectedNodesResult nodes =
                            Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                    for (Node node : nodes.getNodes()) {
                        Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), WEARABLE_CANCELBOLUS_PATH, null);
                    }

            } else {
                googleApiClient.blockingConnect(15, TimeUnit.SECONDS);
                if (googleApiClient.isConnected()) {
                    NodeApi.GetConnectedNodesResult nodes =
                            Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                    for (Node node : nodes.getNodes()) {
                        Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), WEARABLE_CANCELBOLUS_PATH, null);
                    }

                }
            }
            return null;
        }
    }

    public void requestData() {
        new DataRequester(this).execute();
    }

    public void cancelBolus() {
        new BolusCancelTask(this).execute();
    }

    public void googleApiConnect() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        Wearable.MessageApi.addListener(googleApiClient, this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_RESEND.equals(intent.getAction())) {
            googleApiConnect();
            requestData();
        } else if(intent != null && ACTION_CANCELBOLUS.equals(intent.getAction())){
            googleApiConnect();
            cancelBolus();
        }
        //TODO: add action to cancel bolus
        return START_STICKY;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        DataMap dataMap;

        for (DataEvent event : dataEvents) {

            if (event.getType() == DataEvent.TYPE_CHANGED) {


                String path = event.getDataItem().getUri().getPath();
                if (path.equals(OPEN_SETTINGS)) {
                    Intent intent = new Intent(this, NWPreferences.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else if (path.equals(BOLUS_PROGRESS_PATH)) {
                    int progress = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getInt("progresspercent", 0);
                    String status = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getString("progressstatus", "");
                    showBolusProgress(progress, status);
                } else if (path.equals(NEW_STATUS_PATH)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Intent messageIntent = new Intent();
                    messageIntent.setAction(Intent.ACTION_SEND);
                    messageIntent.putExtra("status", dataMap.toBundle());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
                } else if (path.equals(BASAL_DATA_PATH)){
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Intent messageIntent = new Intent();
                    messageIntent.setAction(Intent.ACTION_SEND);
                    messageIntent.putExtra("basals", dataMap.toBundle());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
                } else {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Intent messageIntent = new Intent();
                    messageIntent.setAction(Intent.ACTION_SEND);
                    messageIntent.putExtra("data", dataMap.toBundle());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
                }
            }
        }
    }

    private void showBolusProgress(int progresspercent, String progresstatus) {
        Intent cancelIntent = new Intent(this, ListenerService.class);
        cancelIntent.setAction(ACTION_CANCELBOLUS);
        PendingIntent cancelPendingIntent = PendingIntent.getService(this, 0, cancelIntent, 0);;

        long[] vibratePattern;
        boolean vibreate = PreferenceManager
                .getDefaultSharedPreferences(this).getBoolean("vibrateOnBolus", true);
        if(vibreate){
            vibratePattern = new long[]{0, 50, 1000};
        } else {
            vibratePattern = new long[]{0, 1, 1000};
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_icon)
                        .setContentTitle("Bolus Progress")
                        .setContentText(progresspercent + "% - " + progresstatus)
                        .setContentIntent(cancelPendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setVibrate(vibratePattern)
                        .addAction(R.drawable.ic_cancel, "CANCEL BOLUS", cancelPendingIntent);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

        if (progresspercent == 100){
            scheduleDismiss();
        }
    }

    private void scheduleDismiss() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                NotificationManagerCompat notificationManager =
                        NotificationManagerCompat.from(ListenerService.this);
                notificationManager.cancel(NOTIFICATION_ID);
            }
        });
        t.start();
    }



    public static void requestData(Context context) {
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_RESEND);
        context.startService(intent);
    }

    @Override
    public void onConnected(Bundle bundle) {
        requestData();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        if (googleApiClient != null) {
            Wearable.MessageApi.removeListener(googleApiClient, this);
        }
    }
}
