package com.ptp.gutechsocietynfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class NFCScanner extends Activity implements ZXingScannerView.ResultHandler {

    private ZXingScannerView mScannerView;
    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;
    private boolean cameraActive=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcscanner);

        mScannerView = new ZXingScannerView(this);

        mAdapter=NfcAdapter.getDefaultAdapter(this);
        if(mAdapter==null) {
            finish();
            return;
        }

        pendingIntent=PendingIntent.getActivity(this,0,new Intent(this,getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);
        IntentFilter tag=new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        try {
            tag.addDataType("*");
        }
        catch(Exception e) {

        }
        intentFiltersArray=new IntentFilter[] {tag,};
        techListsArray=new String[][] { new String[] { NfcA.class.getName() }};
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.enableForegroundDispatch(this,pendingIntent,intentFiltersArray,techListsArray);
        if(cameraActive==true) {
            mScannerView.setResultHandler(this);
            mScannerView.startCamera();
            setContentView(mScannerView);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
        mAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        workonNFC(intent);
    }

    private String getHexTag(byte[] id) {
        StringBuilder t=new StringBuilder();
        for(int i=0;i<id.length;i++) {
            t.append(String.format("%02X",id[i]));
        }
        return t.toString();
    }

    private void workonNFC(Intent intent) {
        if(intent==null)
            return;
        String action=intent.getAction();
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable tag=intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Tag tag0=(Tag)(tag);
            byte[] id=tag0.getId();
            String tagId=getHexTag(id);
            TextView textView=(TextView)findViewById(R.id.nfc_info);
            textView.setText("Loading...");
            CheckInNFC temp=new CheckInNFC();
            temp.execute(new String[] {tagId});
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        String qrCode=rawResult.getText();
        cameraActive=false;
        setContentView(R.layout.activity_nfcscanner);
        TextView textView=(TextView)findViewById(R.id.nfc_info);
        textView.setText("Loading...");
        CheckInQR temp=new CheckInQR();
        temp.execute(new String[] {qrCode});
    }

    public void switchToQR(View view) {
        cameraActive=true;
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
        setContentView(mScannerView);
    }
    private class CheckInNFC extends AsyncTask<String,Void,Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            int code=0;
            String result="";
            try {
                MCrypt mcrypt=new MCrypt();
                byte[] req=mcrypt.encrypt("OMFG_checkinnfc_"+params[0]);
                URL url = new URL("http://212.233.162.154/request.php?req="+mcrypt.bytesToHex(req));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                code=connection.getResponseCode();
                InputStream is=connection.getInputStream();
                Scanner sc=new Scanner(is);
                result=sc.nextLine();
                sc.close();
                is.close();
                connection.disconnect();
            }
            catch(Exception e) {
                Log.e("TEST",e.getMessage());
                code=404;
            }
            if(code!=200)
                return -1;
            if(result.equals("NO"))
                return -2;
            else
                return Integer.parseInt(result);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result==-2) {
                Toast.makeText(getApplicationContext(),"This tag has not been registered!",Toast.LENGTH_LONG).show();
            }
            else if(result==-1) {
                Toast.makeText(getApplicationContext(),"Error while checking-in NFC tag",Toast.LENGTH_LONG).show();
            }
            else {
                TextView textView=(TextView)findViewById(R.id.nfc_info);
                textView.setText("This person has checked-in " + result + " times");
            }
        }
    }

    private class CheckInQR extends AsyncTask<String,Void,Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            int code=0;
            String result="";
            try {
                MCrypt mcrypt=new MCrypt();
                byte[] req=mcrypt.encrypt("OMFG_checkinqr_"+params[0]);
                URL url = new URL("http://212.233.162.154/request.php?req="+mcrypt.bytesToHex(req));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                code=connection.getResponseCode();
                InputStream is=connection.getInputStream();
                Scanner sc=new Scanner(is);
                result=sc.nextLine();
                sc.close();
                is.close();
                connection.disconnect();
            }
            catch(Exception e) {
                Log.e("TEST",e.getMessage());
                code=404;
            }
            if(code!=200)
                return -1;
            else
                return Integer.parseInt(result);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result==-1) {
                Toast.makeText(getApplicationContext(),"Error while checking-in QR code",Toast.LENGTH_LONG).show();
            }
            else {
                TextView textView=(TextView)findViewById(R.id.nfc_info);
                textView.setText("This person has checked-in " + result + " times");
            }
        }
    }
}
