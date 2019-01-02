package com.example.nardier.beaconapptest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;

public class Principal extends AppCompatActivity implements View.OnClickListener, BeaconConsumer,
        RangeNotifier {
    protected final String TAG = Principal.this.getClass().getSimpleName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final long DEFAULT_SCAN_PERIOD_MS = 6000l;
    private static final String ALL_BEACONS_REGION = "AllBeaconsRegion";
    private AlertDialog alerta;
    private BeaconManager mBeaconManager;
    private Region mRegion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getStartButton().setOnClickListener(this);
        getStopButton().setOnClickListener(this);
        mBeaconManager = BeaconManager.getInstanceForApplication(this);
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        ArrayList<Identifier> identifiers = new ArrayList<>();
        mRegion = new Region(ALL_BEACONS_REGION, identifiers);
    }

    @Override
    public void onClick(View view) {
        if (view.equals(findViewById(R.id.startReadingBeaconsButton))) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
                    askForLocationPermissions();
                } else {
                    prepareDetection();
                }

            } else {
                prepareDetection();
            }
        } else if (view.equals(findViewById(R.id.stopReadingBeaconsButton))) {
            stopDetectingBeacons();
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
            }
        }
    }

    private void prepareDetection() {
        if (!isLocationEnabled()) {
            askToTurnOnLocation();
        } else {

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                showToastMessage("Erro Bluetooth");
            } else if (mBluetoothAdapter.isEnabled()) {
                startDetectingBeacons();
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                startDetectingBeacons();
            } else if (resultCode == RESULT_CANCELED) { // User refuses to enable bluetooth
                showToastMessage("Erro Bluetooth");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startDetectingBeacons() {
        mBeaconManager.setForegroundScanPeriod(DEFAULT_SCAN_PERIOD_MS);
        mBeaconManager.bind(this);
        getStartButton().setEnabled(false);
        getStartButton().setAlpha(.5f);
        getStopButton().setEnabled(true);
        getStopButton().setAlpha(1);
    }

    @Override
    public void onBeaconServiceConnect() {
        try {
            mBeaconManager.startRangingBeaconsInRegion(mRegion);
            showToastMessage("Procurando Beacons");
        } catch (RemoteException e) {
            Log.d(TAG, "Erro ao iniciar o scanner " + e.getMessage());
        }
        mBeaconManager.addRangeNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if (beacons.size() == 0) {
            showToastMessage("Procurando...");
        }

        for (Beacon beacon : beacons) {
            geracupom(beacon.getId1().toString());
        }
    }

    private void geracupom(String beacon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Novo cupom encontrado");
        if (beacon.equals("0x6ba0d642b706d165fd3d")) {
            builder.setMessage("Código do cupom: INFARMA123 do beacon COMPRADO:" + beacon);
        }
        if (beacon.equals("0xff15f058f97fa4ea919c")) {
            builder.setMessage("Código do cupom: INFARMA312 do beacon do BERG :" + beacon);
        }
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
//                Toast.makeText(Principal.this, "positivo=" + arg1, Toast.LENGTH_SHORT).show();
            }
        });
        alerta = builder.create();
        alerta.show();

    }

    private void stopDetectingBeacons() {
        try {
            mBeaconManager.stopMonitoringBeaconsInRegion(mRegion);
            showToastMessage("Scanner parado");
        } catch (RemoteException e) {
            Log.d(TAG, "Não foi possível parar o scanner " + e.getMessage());
        }
        mBeaconManager.removeAllRangeNotifiers();
        mBeaconManager.unbind(this);
        getStartButton().setEnabled(true);
        getStartButton().setAlpha(1);
        getStopButton().setEnabled(false);
        getStopButton().setAlpha(.5f);
    }

    private void askForLocationPermissions() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Requer permissão de localização");
        builder.setMessage("Garanta a permissão de localização para continuar");
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onDismiss(DialogInterface dialog) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    prepareDetection();
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Funcionalidade limitada");
                    builder.setMessage("Não é possível localizar os beacons devido a permissão de localização");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean networkLocationEnabled = false;
        boolean gpsLocationEnabled = false;
        try {
            networkLocationEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            gpsLocationEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            Log.d(TAG, "Erro ao obter a localização");
        }
        return networkLocationEnabled || gpsLocationEnabled;
    }

    private void askToTurnOnLocation() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage("Localização desabilitada");
        dialog.setPositiveButton("Configuração de localização", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
            }
        });
        dialog.show();
    }

    private Button getStartButton() {
        return (Button) findViewById(R.id.startReadingBeaconsButton);
    }

    private Button getStopButton() {
        return (Button) findViewById(R.id.stopReadingBeaconsButton);
    }

    private void showToastMessage(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBeaconManager.removeAllRangeNotifiers();
        mBeaconManager.unbind(this);
    }
}
