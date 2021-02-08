package com.aashdit.myimageupload;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.aashdit.myimageupload.databinding.ActivityMainBinding;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity implements LocationListener,
        ConnectivityChangeReceiver.ConnectivityReceiverListener {


    public static final int PICK_IMAGE = 1;
    private static final String TAG = "MainActivity";
    double latitude = 0.0, longitude = 0.0;
    RealmResults<Offline> offlines;
    private LocationManager locationManager;
    private boolean isConnected;
    private ConnectivityChangeReceiver mConnectivityChangeReceiver;
    private ActivityMainBinding binding;
    private int type = 0;
    private String imgOne = "", imgTwo = "", imgThree = "";
    private Realm realm;

    public static String convertToBase64(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);

//        return encoded;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        realm = Realm.getDefaultInstance();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
        AndroidNetworking.initialize(getApplicationContext(), okHttpClient);
        binding.ivOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                type = 0;
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(/*Intent.createChooser(*/intent, /*"Select Picture"),*/ PICK_IMAGE);
            }
        });

        binding.ivTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                type = 1;
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(/*Intent.createChooser(*/intent, /*"Select Picture"),*/ PICK_IMAGE);
            }
        });

        binding.ivThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                type = 2;
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(/*Intent.createChooser(*/intent, /*"Select Picture"),*/ PICK_IMAGE);
            }
        });

        mConnectivityChangeReceiver = new ConnectivityChangeReceiver();
        mConnectivityChangeReceiver.setConnectivityReceiverListener(this);
        isConnected = mConnectivityChangeReceiver.getConnectionStatus(this);
        registerNetworkBroadcast();
        getLocation();
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            // do your work now
//                                Toast.makeText(getApplicationContext(), "All permissions are granted!", Toast.LENGTH_SHORT).show();
//                            handler.postDelayed(runnable, 2000);

                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permenantly, navigate user to app settings
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();


        mConnectivityChangeReceiver = new ConnectivityChangeReceiver();
        mConnectivityChangeReceiver.setConnectivityReceiverListener(this);
        isConnected = mConnectivityChangeReceiver.getConnectionStatus(this);

        binding.btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected) {
                    //TODO Upload Online

                    callApi();

                } else {
                    saveToLocalDb();
                }
            }
        });

    }

    private void saveToLocalDb() {
        Offline offline = new Offline();
        offline.latitude = latitude;
        offline.longitude = longitude;
        offline.image = imgOne;
        offline.isUploaded = false;

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.insertOrUpdate(offline);
            }
        });
    }

    private void callApi() {
        AndroidNetworking.post("https://craftlog.in/artist/base64upload.php")
                .setTag("Offline Sync")
                .addBodyParameter("imageData", "data:image/png;base64," + imgOne)
                .setPriority(Priority.HIGH)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {

                        Log.i(TAG, "onResponse: " + response);
                        Toast.makeText(MainActivity.this, "" + response, Toast.LENGTH_SHORT).show();
                        JSONObject resObj = null;
                        try {
                            resObj = new JSONObject(response);
                            if (resObj.optInt("status") == 1) {
                                Toast.makeText(MainActivity.this, resObj.optString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e(TAG, "onError: " + anError.getErrorDetail());
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {

            if (data.getData() != null) {
//                Bitmap photo = (Bitmap) data.getExtras().get("data");
                Uri selectedImage = data.getData();
                try {
                    Bitmap photo = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);

                    if (type == 0) {
                        binding.ivOne.setImageURI(selectedImage);

                        imgOne = convertToBase64(photo);
                    }
                    if (type == 1) {
                        binding.ivTwo.setImageBitmap(photo);

                        imgTwo = convertToBase64(photo);
                    }
                    if (type == 2) {
                        binding.ivThree.setImageBitmap(photo);

                        imgThree = convertToBase64(photo);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();

    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    private void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            assert locationManager != null;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.getInstance().setConnectivityListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterNetworkChanges();
    }

    private void registerNetworkBroadcast() {
        registerReceiver(mConnectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    protected void unregisterNetworkChanges() {
        try {
            unregisterReceiver(mConnectivityChangeReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

        if (locationManager != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        this.isConnected = isConnected;
        Toast.makeText(this, "" + isConnected, Toast.LENGTH_SHORT).show();

        if (isConnected) {
            offlines = realm.where(Offline.class).findAll();
            if (offlines != null && offlines.size() > 0) {
                uploadImages(0);
//                for (int i = 0; i < offlines.size(); i++) {
//                    uploadImageToFirebase(offlines.get(i).image, i, new ImageUploadListener() {
//                        @Override
//                        public void onImageUploaded(boolean isSuccess, int pos) {
//                            if (isSuccess){
//                                realm.executeTransaction(new Realm.Transaction() {
//                                    @Override
//                                    public void execute(Realm realm1) {
//                                        offlines.deleteFromRealm(pos);
//                                    }
//                                });
//                            }
//                        }
//                    });
//                }
            }
        }
    }

    private void uploadImages(int position) {

//        Log.i(TAG, "uploadImages: image "+);
        if (offlines.size() > 0 && offlines.get(position) != null) {
            AndroidNetworking.post("https://craftlog.in/artist/base64upload.php")
                    .setTag("Offline Sync")
                    .addBodyParameter("imageData", "data:image/png;base64," + offlines.get(position).image)
                    .setPriority(Priority.HIGH)
                    .build()
                    .getAsString(new StringRequestListener() {
                        @Override
                        public void onResponse(String response) {

                            Log.i(TAG, "onResponse: " + response);
                            Toast.makeText(MainActivity.this, "" + response, Toast.LENGTH_SHORT).show();
                            JSONObject resObj = null;
                            try {
                                resObj = new JSONObject(response);
                                if (resObj.optInt("status") == 1) {
                                    Toast.makeText(MainActivity.this, resObj.optString("message"), Toast.LENGTH_SHORT).show();

                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm1) {
                                            offlines.deleteFromRealm(position);
                                            if (offlines != null && offlines.size() > 0) {
                                                uploadImages(0);
                                            }
                                        }
                                    });
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onError(ANError anError) {
                            Log.e(TAG, "onError: " + anError.getErrorDetail());
                        }
                    });
        }


//        uploadImageToFirebase(offlines.get(position).image, position, new ImageUploadListener() {
//            @Override
//            public void onImageUploaded(boolean isSuccess, int pos) {
//                if (isSuccess) {
//
//                }
//            }
//        });
    }

//    private void uploadImages(int position) {
//
//        postImages = new ArrayList<>();
//
//        if (offlines != null && offlines.size() > 0) {
//            uploadImageToFirebase(offlines.get(position), position, (isSuccess, pos, imgURL) -> {
//                if (isSuccess) {
//                    pos++;
//                    retryCount = 0;
//                    postImages.add(imgURL);
//                    if (pos < bitmaps.size()) {
//                        uploadImages(pos);
//                    } else {
//                        createPost(true);
//                    }
//                } else {
//                    retryCount++;
//                    if (retryCount < 3) {
//                        uploadImages(pos);
//                    } else {
//                        Snackbar.make(binding.rlRoot, "Error Uploading Image", Snackbar.LENGTH_SHORT).show();
//                    }
//                }
//            });
//        }
//    }

    private void uploadImageToFirebase(String image, int pos, ImageUploadListener listener) {

//        listener.onImageUploaded();

//        if (!image.startsWith("https://")) {
//
//            reference.putFile(Uri.fromFile(new File(image)))
//                    .addOnSuccessListener(taskSnapshot -> reference.getDownloadUrl()
//                            .addOnSuccessListener(uri -> listener.onImageUploaded(true, pos, uri.toString()))
//                            .addOnFailureListener(e -> listener.onImageUploaded(false, pos, null)));
//        }
    }

    interface ImageUploadListener {
        void onImageUploaded(boolean isSuccess, int pos/*, String imageURL*/);
    }
}