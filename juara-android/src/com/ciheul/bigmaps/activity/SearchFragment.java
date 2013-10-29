package com.ciheul.bigmaps.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.ciheul.bigmaps.R;
import com.ciheul.bigmaps.data.JuaraContentProvider;
import com.ciheul.bigmaps.data.JuaraDatabaseHelper;

public class SearchFragment extends ListFragment implements OnQueryTextListener {

    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;

    OnSelectedSearchResultListener callback;
    private SearchView searchView;

    public interface OnSelectedSearchResultListener {
        public void onRegionSelected(String regionName, Fragment fragment);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("BigMaps", "SearchFragment: onCreated");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnSelectedSearchResultListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSelectedSearchResultListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("BigMaps", "SearchFragment: onActivityCreated");

        // MODE_PRIVATE
        prefs = getActivity().getPreferences(0);
        prefsEditor = prefs.edit();

        setHasOptionsMenu(true);
        showSearchResult("");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("BigMaps", "SearchFragment: onCreateView");
        View view = (View) inflater.inflate(R.layout.fragment_search, container, false);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        Log.d("BigMaps", "SearchFragment: onCreateOptionsMenu");

        final MenuItem searchItem = menu.findItem(R.id.action_region_list);
        searchItem.expandActionView();
        searchItem.setOnActionExpandListener(new OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Log.d("BigMaps", "SearchFragment: onMenuItemActionExpand");
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                removeSearchFragment();
                prefsEditor.putBoolean("isSearchViewExpanded", true).commit();
                Log.d("BigMaps", "SearchFragment: onMenuItemActionCollapse");
                return false;
            }
        });

        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
    }

    private void removeSearchFragment() {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.remove(this).commit();
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Log.d("BigMaps", "SearchFragment: onQueryTextChange");
        showSearchResult(newText);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Log.d("BigMaps", "SearchFragment: position: " + String.valueOf(position) + " & " + "id: " +
        // String.valueOf(id));
        Cursor cursor = (Cursor) l.getItemAtPosition(position);
        String regionName = cursor.getString(1);
        Log.d("BigMaps", "SearchFragment: " + regionName);
        callback.onRegionSelected(regionName, this);
        prefsEditor.putBoolean("isSearchViewExpanded", true).commit();
    }

    private void showSearchResult(String query) {
        // Log.d("BigMaps", "SearchFragment: showSearchResult");
        String[] projection = { JuaraDatabaseHelper.COL_ADMINISTRATIVE_ID, JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME };
        String selection = "";
        if (query != "") {
            selection = JuaraDatabaseHelper.COL_ADMINISTRATIVE_TYPE + "='Kecamatan' AND "
                    + JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME + " LIKE '%" + query + "%'";
        } else {
            selection = JuaraDatabaseHelper.COL_ADMINISTRATIVE_TYPE + "='Kecamatan'";
        }

        Cursor cursor = getActivity().getContentResolver().query(JuaraContentProvider.ADMINISTRATIVE_CONTENT_URI,
                projection, selection, null, JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME);

        if (cursor != null) {
            String[] from = { JuaraDatabaseHelper.COL_ADMINISTRATIVE_NAME };
            int[] to = { android.R.id.text1 };
            SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(getActivity(),
                    android.R.layout.simple_list_item_1, cursor, from, to, 0);
            setListAdapter(simpleCursorAdapter);
        }

        // registerForContextMenu(getListView());
    }
}
