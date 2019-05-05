package com.kirana.creditkhata;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

public class CustomDialogClass extends Dialog implements android.view.View.OnClickListener {

    private Activity c;
    public Dialog d;
    public FloatingActionButton yes, no;
    private Integer mode;
    private String header, body, editHint;
    private TextView headerTv, bodyTv;
    private EditText editBody;

    public CustomDialogClass(Activity a, int i, String header, String body, String editHint) {
        super(a);
        this.c = a;
        this.mode = i;
        this.header = header;
        this.body = body;
        this.editHint = editHint;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_alert_dialog);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        setCancelable(false);

        headerTv = findViewById(R.id.header_tv);
        bodyTv = findViewById(R.id.body_tv);
        editBody = findViewById(R.id.edit_body);
        yes = (FloatingActionButton) findViewById(R.id.frmOk);
        no = (FloatingActionButton) findViewById(R.id.frmNo);

        headerTv.setText(header);
        bodyTv.setText(body);

        if (editHint != null) {
            editBody.setVisibility(View.VISIBLE);
            editBody.setHint(editHint);
        }
        //also make a method later to return editText value when asked

        yes.setOnClickListener(this);
        no.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.frmOk:
                c.finish();
                break;
            case R.id.frmNo:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }
}