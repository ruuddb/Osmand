/**
 * 
 */
package com.osmand.activities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

/**
 * @author Maxim Frolov
 * 
 */
public class FavouritesActivity extends ListActivity {

	public static final int NAVIGATE_TO = 0;
	public static final int DELETE_ITEM = 1;
	public static final int EDIT_ITEM = 2;
	

	private List<FavouritePoint> favouritesList;
	private FavouritesDbHelper helper;
	private FavouritesAdapter favouritesAdapter;
	

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.favourites);

		helper = new FavouritesDbHelper(this);
		favouritesList = helper.getFavouritePoints();
		
		ListView lv = getListView();
		favouritesAdapter = new FavouritesAdapter(favouritesList);
		lv.setAdapter(favouritesAdapter);
		/* Add Context-Menu listener to the ListView. */
        lv.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener(){

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(R.string.favourites_context_menu_title);
				menu.add(0, NAVIGATE_TO, 0, R.string.favourites_context_menu_navigate);
				menu.add(0, EDIT_ITEM, 1, R.string.favourites_context_menu_edit);
				menu.add(0, DELETE_ITEM, 2, R.string.favourites_context_menu_delete);
			}
        	
        });
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		FavouritePoint point = favouritesList.get(position);
		OsmandSettings.setMapLocationToShow(this, point.getLatitude(), point.getLongitude());
		Intent newIntent = new Intent(FavouritesActivity.this, MapActivity.class);
		startActivity(newIntent);
	}
	
	@Override
      public boolean onContextItemSelected(MenuItem aItem) {
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem.getMenuInfo();
		final FavouritePoint point = (FavouritePoint) favouritesList.get(menuInfo.position);
		if(aItem.getItemId() == NAVIGATE_TO){
			OsmandSettings.setMapLocationToShow(this, point.getLatitude(), point.getLongitude());
			OsmandSettings.setPointToNavigate(this, point.getLatitude(), point.getLongitude());
			Intent newIntent = new Intent(FavouritesActivity.this, MapActivity.class);
			startActivity(newIntent);
		} else if(aItem.getItemId() == EDIT_ITEM){
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.favourites_edit_dialog_title);
			final EditText editText = new EditText(this);
			builder.setView(editText);
			editText.setText(point.getName());
			builder.setNegativeButton(R.string.default_buttons_cancel, null);
			builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					boolean editied = helper.editFavouriteName(point, editText.getText().toString());
					if (editied) {
						favouritesAdapter.notifyDataSetChanged();
					}

				}
			});
			builder.create().show();
			return true;
		} if(aItem.getItemId() == DELETE_ITEM){
			final Resources resources = this.getResources();
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.favourites_remove_dialog_title);
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					boolean deleted = helper.deleteFavourite(point);
					if (deleted) {
						Toast.makeText(FavouritesActivity.this, MessageFormat.format(resources.getString(R.string.favourites_remove_dialog_success), point.getName()),
								Toast.LENGTH_SHORT).show();
						favouritesList.remove(point);
						favouritesAdapter.notifyDataSetChanged();
					}

				}
			});
			builder.create().show();
			return true;
		}
		return false;
	}
	
	public static class FavouritesDbHelper extends SQLiteOpenHelper {

	    private static final int DATABASE_VERSION = 1;
	    private static final String FAVOURITE_TABLE_NAME = "favourite"; //$NON-NLS-1$
	    private static final String FAVOURITE_COL_NAME = "name"; //$NON-NLS-1$
	    private static final String FAVOURITE_COL_LAT = "latitude"; //$NON-NLS-1$
	    private static final String FAVOURITE_COL_LON = "longitude"; //$NON-NLS-1$
	    private static final String FAVOURITE_TABLE_CREATE =   "CREATE TABLE " + FAVOURITE_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
	                FAVOURITE_COL_NAME + " TEXT, " + FAVOURITE_COL_LAT + " double, " + //$NON-NLS-1$ //$NON-NLS-2$
	                FAVOURITE_COL_LON + " double);"; //$NON-NLS-1$

	    FavouritesDbHelper(Context context) {
	        super(context, FAVOURITE_TABLE_NAME, null, DATABASE_VERSION);
	    }
	    
	    public boolean addFavourite(FavouritePoint p){
	    	SQLiteDatabase db = getWritableDatabase();
	    	if(db != null){
	    		db.execSQL("INSERT INTO " + FAVOURITE_TABLE_NAME + " VALUES (?, ?, ?)",new Object[]{p.getName(), p.getLatitude(), p.getLongitude()}); //$NON-NLS-1$ //$NON-NLS-2$
	    		return true;
	    	}
	    	return false;
	    }
	    
	    public List<FavouritePoint> getFavouritePoints(){
	    	SQLiteDatabase db = getReadableDatabase();
	    	ArrayList<FavouritePoint> list = new ArrayList<FavouritePoint>();
	    	if(db != null){
	    		Cursor query = db.rawQuery("SELECT " + FAVOURITE_COL_NAME +", " + FAVOURITE_COL_LAT +"," + FAVOURITE_COL_LON +" FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
	    				FAVOURITE_TABLE_NAME, null);
	    		if(query.moveToFirst()){
	    			do {
	    				FavouritePoint p = new FavouritePoint();
	    				p.setName(query.getString(0));
	    				p.setLatitude(query.getDouble(1));
	    				p.setLongitude(query.getDouble(2));
	    				list.add(p);
	    			} while(query.moveToNext());
	    		}
	    		query.close();
	    	}
	    	return list;
	    }
	    
	    public boolean editFavouriteName(FavouritePoint p, String newName){
	    	SQLiteDatabase db = getWritableDatabase();
	    	if(db != null){
	    		db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET name = ? WHERE name = ?",new Object[]{newName, p.getName()}); //$NON-NLS-1$ //$NON-NLS-2$
	    		p.setName(newName);
	    		return true;
	    	}
	    	return false;
	    }
	    
	    public boolean deleteFavourite(FavouritePoint p){
	    	SQLiteDatabase db = getWritableDatabase();
	    	if(db != null){
	    		db.execSQL("DELETE FROM " + FAVOURITE_TABLE_NAME + " WHERE name = ?",new Object[]{p.getName()}); //$NON-NLS-1$ //$NON-NLS-2$
	    		return true;
	    	}
	    	return false;
	    }
	    

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	        db.execSQL(FAVOURITE_TABLE_CREATE);
	    }

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	
	public static class FavouritePoint {
		private String name;
		private double latitude;
		private double longitude;

		public double getLatitude() {
			return latitude;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return "Favourite " + getName(); //$NON-NLS-1$
		}
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(helper != null){
			helper.close();
		}
	}

	class FavouritesAdapter extends ArrayAdapter<FavouritePoint> {
		FavouritesAdapter(List<FavouritePoint> list) {
			super(FavouritesActivity.this, R.layout.favourites_list_item, list);
			this.setNotifyOnChange(false);
		}


		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.favourites_list_item, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.favourite_label);
			TextView distanceLabel = (TextView) row.findViewById(R.id.favouritedistance_label);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
			FavouritePoint model = (FavouritePoint) getItem(position);
			icon.setImageResource(R.drawable.poi);
			LatLon lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(FavouritesActivity.this);
			int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(), 
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
			distanceLabel.setText(MapUtils.getFormattedDistance(dist));
			label.setText(model.getName());
			return row;
		}
	}

}
