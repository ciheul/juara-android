package com.ciheul.bigmaps.extend;

import org.osmdroid.bonuspack.overlays.DefaultInfoWindow;
import org.osmdroid.bonuspack.overlays.ExtendedOverlayItem;
import org.osmdroid.views.MapView;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.ciheul.bigmaps.R;
import com.ciheul.bigmaps.data.BigBikeContentProvider;
import com.ciheul.bigmaps.data.BigBikeDatabaseHelper;

/**
 * A customized InfoWindow handling "itinerary" points (start, destination and via-points). We inherit from
 * DefaultInfoWindow as it already provides most of what we want. And we just add support for a "remove" button.
 * 
 * @author M.Kergall
 */
public class ViaPointInfoWindow extends DefaultInfoWindow {

    String shelterName;
    double longitude;
    double latitude;
    
    public ViaPointInfoWindow(final Context context, int layoutResId, MapView mapView) {
        super(layoutResId, mapView);

        Button btnMoreInfo = (Button) (mView.findViewById(R.id.bubble_moreinfo));
        btnMoreInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // TODO cumbersome. need to find the id out efficiently rather than querying the database
                String[] projection = { BigBikeDatabaseHelper.COL_SHELTER_ID };
                String selection = BigBikeDatabaseHelper.COL_SHELTER_NAME + "='" + shelterName + "'";
                Cursor cursor = context.getContentResolver().query(BigBikeContentProvider.SHELTER_CONTENT_URI,
                        projection, selection, null, null);
                if (cursor != null) {
                    SharedPreferences shareLocationPref = context.getSharedPreferences("prevLocation",
                            Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = shareLocationPref.edit();

                    Log.d("BigMaps", "ViaPoint lon: " + longitude);
                    Log.d("BigMaps", "ViaPoint lat: " + latitude);

                    editor.putString("longitude", String.valueOf(longitude));
                    editor.putString("latitude", String.valueOf(latitude));
                    editor.commit();

                    cursor.moveToFirst();

//                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(BigBikeDatabaseHelper.COL_SHELTER_ID));
//                    Intent shelterInfoIntent = new Intent(context, ShelterInfoActivity.class);
//                    Uri shelterUri = Uri.parse(BigBikeContentProvider.SHELTER_CONTENT_URI + "/" + id);
//                    shelterInfoIntent.putExtra(BigBikeContentProvider.SHELTER_CONTENT_ITEM_TYPE, shelterUri);
//                    context.startActivity(shelterInfoIntent);
                }
                cursor.close();
            }
        });
    }

    @Override
    public void onOpen(ExtendedOverlayItem item) {
        shelterName = item.getTitle();
        longitude = item.getPoint().getLongitudeE6() / 1E6;
        latitude = item.getPoint().getLatitudeE6() / 1E6;
        super.onOpen(item);
    }

}
