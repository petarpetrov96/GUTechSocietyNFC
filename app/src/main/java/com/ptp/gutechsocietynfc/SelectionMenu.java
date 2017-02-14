package com.ptp.gutechsocietynfc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class SelectionMenu extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection_menu);
    }

    public void scanQR(View view) {
        Intent intent=new Intent(this,QRScanner.class);
        startActivity(intent);
    }

    public void scanNFC(View view) {
        Intent intent=new Intent(this,NFCScanner.class);
        startActivity(intent);
    }

}
