package com.ciheul.bigmaps.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ciheul.bigmaps.R;

public class NavigateFragment extends Fragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("BigMaps", "NavigateFragment: onActivityCreated");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("BigMaps", "NavigateFragment: onCreated");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("BigMaps", "NavigateFragment: onCreateView");
        View view = (View) inflater.inflate(R.layout.fragment_navigate, container, false);
        TextView tvHello = (TextView) view.findViewById(R.id.hellofragment);
        tvHello.setText("Navigate");

        return view;
    }

}
