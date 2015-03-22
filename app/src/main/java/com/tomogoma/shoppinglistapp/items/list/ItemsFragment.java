package com.tomogoma.shoppinglistapp.items.list;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.tomogoma.shoppinglistapp.R;
import com.tomogoma.shoppinglistapp.data.ContentLoader;
import com.tomogoma.shoppinglistapp.data.ContentLoader.OnLoadFinishedListener;
import com.tomogoma.shoppinglistapp.data.Currency;
import com.tomogoma.shoppinglistapp.data.DatabaseContract.CategoryEntry;
import com.tomogoma.shoppinglistapp.data.DatabaseContract.CurrencyEntry;
import com.tomogoma.shoppinglistapp.data.DatabaseContract.ItemEntry;
import com.tomogoma.shoppinglistapp.items.ItemListAdapter;
import com.tomogoma.shoppinglistapp.items.ItemListAdapter.OnDeleteItemRequestListener;
import com.tomogoma.shoppinglistapp.items.ItemListAdapter.OnEditItemRequestListener;
import com.tomogoma.shoppinglistapp.items.manipulate.edit.EditItemActivity;
import com.tomogoma.shoppinglistapp.util.Preference;
import com.tomogoma.shoppinglistapp.util.UI;

/**
 * Created by ogoma on 01/03/15.
 */
public class ItemsFragment extends Fragment
		implements OnItemClickListener, OnEditItemRequestListener, OnDeleteItemRequestListener, OnLoadFinishedListener {

	public static final String EXTRA_long_CATEGORY_ID = ItemsFragment.class.getName() + "_extra.category.id";
	public static final String EXTRA_String_CATEGORY_NAME = ItemsFragment.class.getName() + "_extra.category.name";

	private static final String LOG_TAG = ItemsFragment.class.getSimpleName();

	private ItemListAdapter itemsAdapter;
	private long mCategoryID;
	private String mCategoryName;
	private int mCurrencyLoaderID;
	private ContentLoader mContentLoader;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		Bundle arguments = getArguments();
		if (arguments == null) {
			mCategoryID = CategoryEntry.DEFAULT_ID;
			mCategoryName = CategoryEntry.DEFAULT_NAME;
		} else {
			mCategoryID = arguments.getLong(EXTRA_long_CATEGORY_ID,
			                                CategoryEntry.DEFAULT_ID);
			mCategoryName = arguments.getString(EXTRA_String_CATEGORY_NAME,
			                                    CategoryEntry.DEFAULT_NAME);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.default_list_layout, container, false);
		ListView listView = (ListView) rootView.findViewById(android.R.id.list);
		if (savedInstanceState == null) {
			itemsAdapter = new ItemListAdapter(getActivity());
			itemsAdapter.setOnEditItemRequestListener(this);
			itemsAdapter.setOnDeleteItemRequestListener(this);
		}
		listView.setAdapter(itemsAdapter);
		listView.setOnItemClickListener(this);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		getActivity().setTitle(mCategoryName);

		String itemSortOrder = ItemEntry.TABLE_NAME + "." + ItemEntry.COLUMN_NAME + " ASC";
		Uri itemUri = ItemEntry.buildItemsInCategoryUri(mCategoryID, true);
		String[] itemProjection = ItemListAdapter.S_ITEMS_PROJECTION;

		long preferredCurrencyID = Preference.getPreferredCurrencyID(getActivity());
		Uri currencyUri = CurrencyEntry.buildCurrencyUri(preferredCurrencyID);
		String[] currencyProjection = new String[] {
				CurrencyEntry.COLUMN_SYMBOL,
				CurrencyEntry.COLUMN_CODE,
				CurrencyEntry.COLUMN_LAST_CONVERSION
		};

		mContentLoader = new ContentLoader(getActivity(), this);
		mContentLoader.setOnLoadFinishedListener(this);

		mCurrencyLoaderID = mContentLoader.loadContent(currencyUri, currencyProjection, null);
		mContentLoader.loadContent(itemUri, itemsAdapter, itemProjection, itemSortOrder);

		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		if (position == itemsAdapter.getSelectedPosition()) {
			itemsAdapter.setSelectedPosition(-1);
		} else {
			itemsAdapter.setSelectedPosition(position);
		}
		//  TODO figure out how to use the following instead of re-querying the db:
//		itemsAdapter.getView(position, null, parent);
		//  passing null (instead of view) to ensure that newView() is called in order
		//  to re-inflate the view with the new layout.
		//  tried it debugged it, logged it -> goes through all expected method calls but
		//  does not update the actual view, Bummer!
		itemsAdapter.notifyDataSetChanged();
	}

	@Override
	public void onEditItemRequested(long itemID) {

		Intent editItemActivityIntent = new Intent(getActivity(), EditItemActivity.class);
		editItemActivityIntent.putExtra(EditItemActivity.EXTRA_String_CATEGORY_NAME, mCategoryName);
		editItemActivityIntent.putExtra(EditItemActivity.EXTRA_long_ITEM_ID, itemID);
		editItemActivityIntent.putExtra(EditItemActivity.EXTRA_Class_CALLING_ACTIVITY, getActivity().getClass());
		getActivity().startActivity(editItemActivityIntent);
	}

	@Override
	public void onDeleteItemRequested(long itemID, String itemName) {
		String whereClause = ItemEntry._ID + " = ?";
		String[] whereArgs = new String[] {String.valueOf(itemID)};
		ContentResolver contentResolver = getActivity().getContentResolver();
		int count = contentResolver.delete(ItemEntry.CONTENT_URI, whereClause, whereArgs);
		//  TODO do not delete immediately, archive and allow undo
		if (count==0) {
			UI.showToast(getActivity(), getString(R.string.error_toast_db_delete_fail));
		} else if (count>1) {
			UI.showToast(getActivity(), getString(R.string.error_toast_db_potential_data_corruption));
		} else {
			String successfulMessage = getString(R.string.toast_successful_delete);
			UI.showToast(getActivity(), String.format(successfulMessage, itemName));
		}
	}

	@Override
	public void onLoadFinished(int id) {

		if (id == mCurrencyLoaderID) {

			Currency currency;
			Cursor cursor = mContentLoader.getLoadedCursor(id);
			if (cursor.moveToFirst()) {

				double lastConversion = cursor.getDouble(cursor.getColumnIndex(CurrencyEntry.COLUMN_LAST_CONVERSION));
				String code = cursor.getString(cursor.getColumnIndex(CurrencyEntry.COLUMN_CODE));
				String symbol = cursor.getString(cursor.getColumnIndex(CurrencyEntry.COLUMN_SYMBOL));
				lastConversion = (lastConversion<=0)? 1d: lastConversion;
				currency = new Currency(code, symbol, null, null, lastConversion);
			}
			else {
				Log.e(LOG_TAG, "No currencies found, not even default; count=" + cursor.getCount());
				currency = new Currency(
						CurrencyEntry.DEFAULT_CODE,
						CurrencyEntry.DEFAULT_SYMBOL,
						CurrencyEntry.DEFAULT_NAME,
						CurrencyEntry.DEFAULT_COUNTRY
					);
			}
			itemsAdapter.setCurrency(currency);
		}
	}
}
