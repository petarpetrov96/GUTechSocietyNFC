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
import android.widget.Toast;

import com.google.zxing.Result;

import java.net.HttpURLConnection;
import java.net.URL;

import me.dm7.barcodescanner.zxing.ZXingScannerView;


public class QRScanner extends Activity implements ZXingScannerView.ResultHandler {

    private ZXingScannerView mScannerView;
    private NfcAdapter mAdapter;
    private boolean acceptNFC=false;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;

    String lastQrCode="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        if(acceptNFC==true) {
            switchToNFC();
        }
        else {
            switchToQR();
        }
        mAdapter.enableForegroundDispatch(this,pendingIntent,intentFiltersArray,techListsArray);
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
        mAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void handleResult(Result rawResult) {
        String qrCode=rawResult.getText();
        lastQrCode=qrCode;
        switchToNFC();
    }

    @Override
    public void onNewIntent(Intent intent) {
        if(acceptNFC==false)
            return;
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
            String tempString=tagId+"_"+lastQrCode;
            CheckInNFC temp=new CheckInNFC();
            temp.execute(new String[] {tempString});
            switchToQR();
        }
    }

    private void switchToQR() {
        acceptNFC=false;
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
        setContentView(mScannerView);
    }

    private void switchToNFC() {
        acceptNFC=true;
        mScannerView.stopCamera();
        setContentView(R.layout.activity_qrscanner);
    }

    private class CheckInNFC extends AsyncTask<String,Void,Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            int code=0;
            try {
                MCrypt mcrypt=new MCrypt();
                byte[] req=mcrypt.encrypt("OMFG_registernfc_"+params[0]);
                URL url = new URL("http://212.233.162.154/request.php?req="+mcrypt.bytesToHex(req));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                code=connection.getResponseCode();
                connection.disconnect();
            }
            catch(Exception e) {
                Log.e("TEST",e.getMessage());
                code=404;
            }
            return code;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result==200) {
                Toast.makeText(getApplicationContext(),"NFC tag added successfully!",Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(getApplicationContext(),"Error while adding NFC tag",Toast.LENGTH_LONG).show();
            }
        }
    }

}
