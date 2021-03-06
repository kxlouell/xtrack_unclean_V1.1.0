package com.example.xtrack;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.xtrack.Adapters.peerListAdapter;
import com.example.xtrack.AsyncTasks.SendRecieve;
import com.example.xtrack.AsyncTasks.plotTaskClient;
import com.example.xtrack.AsyncTasks.plotTaskServer;
import com.example.xtrack.BroadCastReciever.WiFiDirectBroadcastReciever;
import com.example.xtrack.InitThreads.ClientInit;
import com.example.xtrack.InitThreads.ServerInit;
import com.example.xtrack.Tools.ChangeDeviceName;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.chrono.IsoChronology;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.xtrack.AsyncTasks.sendTask;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import org.jetbrains.annotations.NotNull;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;


/**
 * Use the Mapbox Core Library to receive updates when the device changes location.
 */
public class trackingphase extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener {

    private int FINE_LOCATION_PERMISION_CODE = 1;
    public String STATUS;
    public int FIRST_RUN = 0;
    private static final String NEW_LAYER_ID = "NEW_LAYER_ID";
    double trying = -86.78160;
    private ServerSocket dumySskt;
    private Socket dumySkt;
    private static final String TAG = "Tracking Phase";

    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    private MapboxMap mapboxMap;
    private MapView mapView;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationChangeListeningActivityLocationCallback callback =
            new LocationChangeListeningActivityLocationCallback(this);
    LocationComponent locationComponent;

    ImageView btn_peersImageView;
    ImageButton btn;
    ListView peersListView;
    ImageView btn_notification;
    Style style;

    public TextView connectionStatus;
    String[] deviceNameArray;
    int[] image;

    MainActivity mActivity;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    WiFiDirectBroadcastReciever mReciever;
    IntentFilter mIntentFilter;
    ServerInit serverInit;
    ClientInit clientInit;
    SendRecieve sendRecieve;

    double lastLat = 0, lastlon = 0;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    WifiP2pDevice[] deviceArray;

    private static final String SOURCE_ID = "SOURCE_ID";
    private static final String ICON_ID = "ICON_ID";
    private static final String LAYER_ID = "LAYER_ID";
    FloatingActionButton centerLoc;
    SymbolManager symbolManager;
    Map<String, Symbol> symbolIP = new HashMap<>();

    public static final int INITAVATAR = 1;
    public static final int mTOAST = 2;
    public static final int PLOTLOCATION = 3;

    private ConnectivityManager mCManager;
    private ConnectivityManager.NetworkCallback mCallback;
    SharedPreferences sprefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sprefs = this.getSharedPreferences(this.getString(R.string.AVATAR), trackingphase.MODE_PRIVATE);
        STATUS = sprefs.getString("USERTYPE", null);
        String devName = sprefs.getString("NAME", null) + " | " + STATUS;
        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));
        // This contains the MapView in XML and needs to be called after the access token is configured.

        setContentView(R.layout.tracking_phase);
        initialWork(savedInstanceState);
        exqListener();
        ChangeDeviceName changeDeviceName = new ChangeDeviceName(mManager, mChannel);
        changeDeviceName.setDeviceName(devName);
        if (STATUS == "Host") {
            if (ActivityCompat.checkSelfPermission(trackingphase.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(trackingphase.this, "Group Created! Client can now Connect!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {

                }
            });
        }

    }

    Handler threadHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case INITAVATAR:
                    System.out.println("Handler Case AvatarInit");
                    Bundle bb = (Bundle) msg.obj;
                    System.out.println("threadHandler: " + bb);
                    style.addImage(bb.getString("Inet"), BitmapFactory.decodeResource(
                            trackingphase.this.getResources(), bb.getInt("Avatar")));
                    SymbolOptions symbolOptions = new SymbolOptions()
                            .withLatLng(new LatLng(0, 0))
                            .withIconImage(bb.getString("Inet"))
                            .withIconSize(0.1f)
                            .withSymbolSortKey(2f)
                            .withDraggable(false)
                            .withTextAnchor("Dowps");
                    symbolIP.put(bb.getString("Inet"), symbolManager.create(symbolOptions));
                    System.out.println("threadHandler: " + symbolIP);
                    break;
                case mTOAST:
                    Toast.makeText(trackingphase.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case PLOTLOCATION:
                    Bundle locationBundle = (Bundle) msg.obj;
                    if (STATUS == "Host") {
                        plotTaskServer ptaskServer = new plotTaskServer(trackingphase.this, serverInit.getDevicesSR(), locationBundle, symbolIP, symbolManager);
                        ptaskServer.execute();
                    } else if (STATUS == "Client") {
                        plotTaskClient plotTaskClient = new plotTaskClient(trackingphase.this, clientInit.sendRecieve, locationBundle, symbolIP, symbolManager);
                        plotTaskClient.execute();
                    }
                    break;
            }
        }
    };

    Handler handlerSockets = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {

        }
    };

    public void initialWork(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        btn = (ImageButton) findViewById(R.id.backbtn);
        btn_peersImageView = (ImageView) findViewById(R.id.btn_peers);
        btn_notification = (ImageView) findViewById(R.id.btn_notification);
        btn_peersImageView.setVisibility(View.VISIBLE);
        connectionStatus = (TextView) findViewById(R.id.connectionStatus);
        mActivity = new MainActivity();

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReciever = new WiFiDirectBroadcastReciever(mManager, mChannel, this, mCManager, mCallback);

//Intent Filters
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        peersListView = findViewById(R.id.peerList);

        centerLoc = (FloatingActionButton) findViewById(R.id.centerLoc);
    }

    public void exqListener() {
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openmainactivity();
            }
        });

        btn_peersImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverPeer();
            }
        });

        peersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice device = deviceArray[position];
                System.out.println("Device Name is " + device.deviceName.contains("Client"));
                System.out.println("Status is "+STATUS);
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                config.groupOwnerIntent = WifiP2pConfig.GROUP_OWNER_INTENT_MIN;

                if (STATUS.equals("Client") && device.deviceName.contains("Client")) {
                    new AlertDialog.Builder(trackingphase.this)
                            .setTitle("That Device is Client")
                            .setMessage("Can't connect to a Client when you're also a Client")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                } else if (STATUS.equals("Host") && device.deviceName.contains("Host")) {
                    new AlertDialog.Builder(trackingphase.this)
                            .setTitle("Connecting Host as a Host")
                            .setMessage("Can't connect to a Host when you're also a Host")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                } else if (STATUS.equals("Host") && device.deviceName.contains("Client")) {
                    new AlertDialog.Builder(trackingphase.this)
                            .setTitle("Connecting Client as a Host")
                            .setMessage("Can't connect to a Client when you're a Host")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                } else if (STATUS.equals("Client") && device.deviceName.contains("Host")) {
                    if (ActivityCompat.checkSelfPermission(trackingphase.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions();
                    } else {
                        System.out.println("Access Fine Location Permitted!");
                    }
                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(), "Connecting to" + device.deviceName, Toast.LENGTH_SHORT).show();
                            mManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(trackingphase.this, "Local Services Cleared!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(int reason) {
                                    switch (reason) {
                                        case WifiP2pManager.P2P_UNSUPPORTED:
                                            Toast.makeText(trackingphase.this, "Local Services Clear Failed: P2p Unsuported", Toast.LENGTH_SHORT).show();
                                            break;
                                        case WifiP2pManager.BUSY:
                                            Toast.makeText(trackingphase.this, "Local Services Clear Failed: BUSY", Toast.LENGTH_SHORT).show();
                                            break;
                                        case WifiP2pManager.ERROR:
                                            Toast.makeText(trackingphase.this, "Local Services Clear Failed: Enternal Error", Toast.LENGTH_SHORT).show();
                                            break;
                                    }
                                }
                            });
                        }

                        @Override
                        public void onFailure(int reason) {
                            Toast.makeText(getApplicationContext(), "Cant connect to " + device.deviceName, Toast.LENGTH_SHORT).show();
                            Toast.makeText(getApplicationContext(), "Error Code: " + reason, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

        });

        centerLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double lat = locationComponent.getLastKnownLocation().getLatitude();
                double lon = locationComponent.getLastKnownLocation().getLongitude();
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(lat, lon))
                        .zoom(18).build();
                mapboxMap.setCameraPosition(cameraPosition);
            }
        });
    }


    private void openmainactivity() {
        Intent intent = new Intent (getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void discoverPeer(){
        if (ContextCompat.checkSelfPermission(trackingphase.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        } else {
            requestPermissions();
        }
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(trackingphase.this, "Discover Started", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                String failReason = "Unknown";
                if (reason == 0) failReason = "Internal Error";
                if (reason == 1) failReason = "P2P Unsupported";
                if (reason == 2) failReason = "WiFi Direct Busy";
                Toast.makeText(trackingphase.this, "Discovery Failed", Toast.LENGTH_SHORT).show();
                Toast.makeText(trackingphase.this, "Error: " + failReason, Toast.LENGTH_SHORT).show();
            }
        });


    }

    public WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(final WifiP2pInfo info) {
            final InetAddress groupOwnerAddress = info.groupOwnerAddress;
            if (info.groupFormed && info.isGroupOwner) {
                connectionStatus.setText("Host");
                serverInit = new ServerInit( trackingphase.this, handlerSockets, threadHandler);
                serverInit.setName("HostSocketsThread");
                //serverInit.start();
            } else if (info.groupFormed) {
                connectionStatus.setText("Client");
                clientInit = new ClientInit(groupOwnerAddress,trackingphase.this,handlerSockets, threadHandler);
                clientInit.setName("ClientSocketThread");
                //clientInit.start();
            }
        }
    };

    public void disconnect() {
        if (mManager != null && mChannel != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions();
                return;
            }
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null) {
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Toast.makeText(getApplicationContext(),"Group Disconnect Success!", Toast.LENGTH_LONG).show();
                                Log.d(TAG, "Disconnected");
                                if(STATUS=="Host"){
                                    serverInit.interrupt();
                                }else if(STATUS=="Client"){
                                    clientInit.interrupt();
                                }
                            }

                            @Override
                            public void onFailure(int reason) {
                                Toast.makeText(getApplicationContext(),"Group Disconnected Failed!", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });
        }
    }
    //button

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(new Style.Builder().fromUri(Style.TRAFFIC_NIGHT)
                .withImage("ID_ICON_A1",
                        BitmapFactory.decodeResource(
                                trackingphase.this.getResources(), R.drawable.mapbox_marker_icon_default))
                .withSource(new GeoJsonSource("marker-source-id"))
                .withLayer(new SymbolLayer(NEW_LAYER_ID,
                        "marker-source-id").withProperties(
                        iconImage("ID_ICON_A1"),
                        visibility(VISIBLE),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true)
                )),
                new Style.OnStyleLoaded() {
                    @Override public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                        trackingphase.this.style = style;
                        symbolManager = new SymbolManager(mapView, mapboxMap, style);
                        symbolManager.setIconAllowOverlap(true);
                        symbolManager.setTextAllowOverlap(true);
                        symbolManager.setIconTranslate(new Float[]{-4f, 5f});

                        style.addImage("ID_ICON_A2", BitmapFactory.decodeResource(
                                trackingphase.this.getResources(), R.drawable.ic_unknown_user));
                    }
                 });
    }

    /**
     * Initialize the Maps SDK's LocationComponent
     */
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);


            initLocationEngine();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);


        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
        }
    }


    private static class LocationChangeListeningActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<trackingphase> activityWeakReference;
        trackingphase trackingPhase;

        LocationChangeListeningActivityLocationCallback(trackingphase activity) {
            this.activityWeakReference = new WeakReference<>(activity);
            this.trackingPhase = activity;
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */

        @Override
        public void onSuccess(LocationEngineResult result) {
            trackingphase activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();

                if (location == null) {
                    return;
                }

                // Create a Toast which displays the new location's coordinates
                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                    double lastlat = trackingPhase.lastLat;
                    double lastlon = trackingPhase.lastlon;
                    double newlat = 0;
                    double newlon = 0;
                    if(lastlat>result.getLastLocation().getLatitude()){
                        newlat = trackingPhase.lastLat - result.getLastLocation().getLatitude();
                    }else{
                        newlat = result.getLastLocation().getLatitude()-trackingPhase.lastLat;
                    }
                    if(lastlon>result.getLastLocation().getLongitude()){
                        newlon = trackingPhase.lastlon - result.getLastLocation().getLongitude();
                    }else{
                        newlon = result.getLastLocation().getLongitude()-trackingPhase.lastlon;
                    }

                    System.out.println(Thread.currentThread()+"Range "+newlat+" "+newlon);
                    /*
                    if (Math.abs(newlat) >=  0.000001||Math.abs(newlon) >= 0.000001) {
                        if (trackingPhase.serverInit != null && trackingPhase.serverInit.getDevicesSR().size() > 0 && trackingPhase.STATUS == "Host") {
                            System.out.println(Thread.currentThread() + " SendingLoc as Server!\n\n\n" + trackingPhase.FIRST_RUN);
                            Bundle bb = new Bundle();
                            bb.putDouble("lat", result.getLastLocation().getLatitude());
                            bb.putDouble("lon", result.getLastLocation().getLongitude());
                            System.out.println(Thread.currentThread() + " First Run is" + trackingPhase.FIRST_RUN);
                            int count = 0;
                            System.out.println("DevicesSR size: " + trackingPhase.serverInit.getDevicesSR().size());
                            for (SendRecieve sr : trackingPhase.serverInit.getDevicesSR()) {
                                if (trackingPhase.FIRST_RUN == 0) {
                                    int icon = trackingPhase.sprefs.getInt("ICON", 0);
                                    System.out.println(Thread.currentThread() + " ICON ID is " + trackingPhase.sprefs.getInt("ICON", 0));
                                    if (icon != 0) {
                                        sr.getHandler().obtainMessage(0, trackingPhase.sprefs.getInt("ICON", 0)).sendToTarget();
                                        System.out.println(Thread.currentThread() + " TrackingPhase " + trackingPhase.FIRST_RUN);
                                    }
                                    trackingPhase.FIRST_RUN = 1;
                                } else {
                                    sr.getHandler().obtainMessage(1, bb).sendToTarget();
                                    System.out.println(Thread.currentThread() + " Sending Location!");
                                }
                                count++;
                            }
                            System.out.println("Count is" + count);
                        } else if (trackingPhase.clientInit != null && trackingPhase.clientInit.getSocket().isConnected() && trackingPhase.STATUS == "Client") {
                            System.out.println("SendingLoc as Client!\n\n\n");
                            Bundle bb = new Bundle();
                            bb.putDouble("lat", result.getLastLocation().getLatitude());
                            bb.putDouble("lon", result.getLastLocation().getLongitude());
                            System.out.println("First Run is" + trackingPhase.FIRST_RUN);
                            if (trackingPhase.FIRST_RUN == 0) {
                                SharedPreferences sprefs = trackingPhase.getSharedPreferences(trackingPhase.getString(R.string.AVATAR), trackingphase.MODE_PRIVATE);
                                int icon = sprefs.getInt("ICON", 0);
                                System.out.println("ICON ID is " + sprefs.getInt("ICON", 0));
                                if (icon != 0) {
                                    trackingPhase.clientInit.sendRecieve.getHandler().obtainMessage(0, sprefs.getInt("ICON", 0)).sendToTarget();
                                    System.out.println("TrackingPhase " + trackingPhase.FIRST_RUN);
                                } else {
                                    System.out.println("Icon is null");
                                }
                                trackingPhase.FIRST_RUN = 1;
                            } else {
                                trackingPhase.clientInit.sendRecieve.getHandler().obtainMessage(1, bb).sendToTarget();
                                System.out.println(Thread.currentThread() + " Sending Location!");
                            }
                        }
                    }*/
                    trackingPhase.lastLat = result.getLastLocation().getLatitude();
                    trackingPhase.lastlon = result.getLastLocation().getLongitude();
                }
                }
            }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can't be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            trackingphase activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        registerReceiver(mReciever, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        unregisterReceiver(mReciever);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
        // Prevent leaks
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(callback);
        }
        mapView.onDestroy();
        btn_peersImageView.setVisibility(View.INVISIBLE);
    }

    public WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                image = new int[peerList.getDeviceList().size()];
                int index = 0;

                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    image[index] = R.drawable.ic_unknown_user;
                    index++;
                }
                peerListAdapter peerAdapter = new peerListAdapter(trackingphase.this,image, deviceNameArray);
                peersListView.setAdapter(peerAdapter);


                if (peers.size() == 0) {
                    Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    };

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public void requestPermissions() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("Ex-Track need to access your location")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(trackingphase.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        }else{
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISION_CODE);

        }
    }
}