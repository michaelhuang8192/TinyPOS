package com.tinyappsdev.tinypos.ui.BaseUI;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import com.tinyappsdev.tinypos.R;



public class SimpleCustomDialog<AI extends ActivityInterface> extends BaseDialog<AI> {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        Bundle bundle = getArguments();

        builder.setView(onCreateCustomView(savedInstanceState, builder, inflater, null))
                .setTitle(bundle.getString("title"))
                .setMessage(bundle.getString("message"));
        builder.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                onConfirm();
            }
        }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                onCancel();
            }
        });

        return  builder.create();
    }

    public View onCreateCustomView(Bundle savedInstanceState,
                                   AlertDialog.Builder builder,
                                   LayoutInflater inflater,
                                   ViewGroup parent)
    {
        return null;
    }

    public void onConfirm() { dismiss(); }
    public void onCancel() { dismiss(); }
}