package com.tomogoma.util.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.tomogoma.shoppinglistapp.R;
import com.tomogoma.shoppinglistapp.data.DatabaseContract.ItemEntry;
import com.tomogoma.util.Formatter;

/**
 * Created by ogoma on 24/02/15.
 */
public class ItemListAdapter extends CursorAdapter {

	public static final String[] S_ITEMS_PROJECTION = new String[]{
			ItemEntry._ID,
			ItemEntry.COLUMN_NAME,
			ItemEntry.COLUMN_DESC,
			ItemEntry.COLUMN_PRICE,
			ItemEntry.COLUMN_QTTY,
			ItemEntry.COLUMN_MEAS_UNIT,
			ItemEntry.COLUMN_USEFUL_UNIT,
			ItemEntry.COLUMN_USEFUL_PER_MEAS,
			ItemEntry.COLUMN_IN_LIST
	};
	private static final int ITEM_ID_COL_INDEX = 0;
	private static final int ITEM_NAME_COL_INDEX = 1;
	private static final int ITEM_DESC_COL_INDEX = 2;
	private static final int ITEM_PRICE_COL_INDEX = 3;
	private static final int ITEM_QUANTITY_COL_INDEX = 4;
	private static final int ITEM_MEAS_UNIT_COL_INDEX = 5;
	private static final int ITEM_LASTS_FOR_UNIT_COL_INDEX = 6;
	private static final int ITEM_LASTS_FOR_COL_INDEX = 7;
	private static final int ITEM_IN_LIST_COL_INDEX = 8;

	private static final int SELECTED_VIEW_TYPE = 0;
	private static final int NORMAL_VIEW_TYPE = 1;

	private class ViewHolder {

		private TextView title;
		private TextView quantity;
		private TextView measUnit;
		private CheckBox isInList;
		private TextView unitPrice;
		private TextView totalPrice;

		private ViewHolder(View listItemView) {

			title = (TextView) listItemView.findViewById(R.id.title);
			quantity = (TextView) listItemView.findViewById(R.id.quantity);
			measUnit = (TextView) listItemView.findViewById(R.id.measUnit);
			isInList = (CheckBox) listItemView.findViewById(R.id.shoppingListAdd);
			unitPrice = (TextView) listItemView.findViewById(R.id.unitPrice);
			totalPrice = (TextView) listItemView.findViewById(R.id.totalPrice);
		}
	}

	private int mSelectedPosition = -1;
	private OnEditItemRequestListener mOnEditItemRequestListener;
	private OnDeleteItemRequestListener mOnDeleteItemRequestListener;

	public interface OnEditItemRequestListener {
		public void onEditItemRequested(long itemID);
	}

	public interface OnDeleteItemRequestListener {
		public void onDeleteItemRequested(long itemID, String name);
	}

	public ItemListAdapter(Activity activity) {

		super(activity, null, 0);
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		if (mSelectedPosition == position) {
			return SELECTED_VIEW_TYPE;
		}
		return NORMAL_VIEW_TYPE;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		Log.d(getClass().getSimpleName(), "Creating new view...");
		View view;
		switch(getItemViewType(cursor.getPosition())) {
			case SELECTED_VIEW_TYPE:
				Log.d(getClass().getSimpleName(), "New view is a selected type");
				view = LayoutInflater.from(context).inflate(R.layout.list_item_expanded, parent, false);
				break;
			case NORMAL_VIEW_TYPE:
			default:
				Log.d(getClass().getSimpleName(), "New view is a normal type");
				view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
				ViewHolder viewHolder = new ViewHolder(view);
				view.setTag(viewHolder);
		}
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		Log.d(getClass().getSimpleName(), "Binding data to view...");
		if (getItemViewType(cursor.getPosition()) == SELECTED_VIEW_TYPE) {
			Log.d(getClass().getSimpleName(), "View is a selected view");
			bindExpandedView(view, context, cursor);
			Log.d(getClass().getSimpleName(), "Done, final position set to " + mSelectedPosition);
			return;
		}

		Log.d(getClass().getSimpleName(), "View is a normal view");
		ViewHolder viewHolder = (ViewHolder) view.getTag();

		long itemID = cursor.getLong(ITEM_ID_COL_INDEX);
		int inList = cursor.getInt(ITEM_IN_LIST_COL_INDEX);
		float quantity = cursor.getFloat(ITEM_QUANTITY_COL_INDEX);
		double price = cursor.getDouble(ITEM_PRICE_COL_INDEX);
		String name = cursor.getString(ITEM_NAME_COL_INDEX);
		String measUnit = cursor.getString(ITEM_MEAS_UNIT_COL_INDEX);

		viewHolder.title.setText(name);
		setQuantity(viewHolder.quantity, viewHolder.measUnit, measUnit, quantity);
		setPrice(viewHolder.unitPrice, viewHolder.totalPrice, price, quantity);
		setInList(viewHolder.isInList, itemID, inList);
	}

	private void bindExpandedView(View view, final Context context, Cursor cursor) {

		Log.d(getClass().getSimpleName(), "Binding expanded view...");
		TextView title = (TextView) view.findViewById(R.id.title);
		ImageView imgEditItem = (ImageView) view.findViewById(R.id.editItem);
		ImageView imgDeleteItem = (ImageView) view.findViewById(R.id.deleteItem);
		CheckBox shoppingListAdd = (CheckBox) view.findViewById(R.id.shoppingListAdd);
		TextView versionName = (TextView) view.findViewById(R.id.versionName);
		TextView unitPrice = (TextView) view.findViewById(R.id.unitPrice);
		TextView totalPrice = (TextView) view.findViewById(R.id.totalPrice);
		TextView description = (TextView) view.findViewById(R.id.description);
		TextView quantity = (TextView) view.findViewById(R.id.quantity);
		TextView measUnit = (TextView) view.findViewById(R.id.measUnit);
		TextView lastsFor = (TextView) view.findViewById(R.id.lastsFor);
		TextView lastsForUnit = (TextView) view.findViewById(R.id.lastsForUnit);
		TextView openingBracket = (TextView) view.findViewById(R.id.openingParenthesis);
		TextView closingBracket = (TextView) view.findViewById(R.id.closingParenthesis);

		final long itemID = cursor.getLong(ITEM_ID_COL_INDEX);
		final String name = cursor.getString(ITEM_NAME_COL_INDEX);
		int inList = cursor.getInt(ITEM_IN_LIST_COL_INDEX);
		double price = cursor.getDouble(ITEM_PRICE_COL_INDEX);
		String desc = cursor.getString(ITEM_DESC_COL_INDEX);
		float lasts = cursor.getFloat(ITEM_LASTS_FOR_COL_INDEX);
		float qtty = cursor.getFloat(ITEM_QUANTITY_COL_INDEX);
		String unit = cursor.getString(ITEM_MEAS_UNIT_COL_INDEX);
		String lastsUnit = cursor.getString(ITEM_LASTS_FOR_UNIT_COL_INDEX);

		title.setText(name);
		setDescription(description, desc);
		setQuantity(quantity, measUnit, unit, qtty);
		setLastsFor(openingBracket, closingBracket, lastsFor, lastsForUnit, lastsUnit, qtty, lasts);
		setPrice(unitPrice, totalPrice, price, qtty);
		setInList(shoppingListAdd, itemID, inList);

		imgEditItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mOnEditItemRequestListener != null) {
					mOnEditItemRequestListener.onEditItemRequested(itemID);
				}
			}
		});

		imgDeleteItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mOnDeleteItemRequestListener != null) {
					mOnDeleteItemRequestListener.onDeleteItemRequested(itemID, name);
				}
			}
		});
	}

	public void setOnEditItemRequestListener(OnEditItemRequestListener listener) {
		mOnEditItemRequestListener = listener;
	}

	public void setOnDeleteItemRequestListener(OnDeleteItemRequestListener listener) {
		mOnDeleteItemRequestListener = listener;
	}

	public void setSelectedPosition(int position) {
		mSelectedPosition = position;
	}

	public int getSelectedPosition() {
		return mSelectedPosition;
	}

	private void setDescription(TextView tvDesc, String description) {

		if (description == null || description.isEmpty()) {
			tvDesc.setVisibility(View.GONE);
			return;
		}
		tvDesc.setVisibility(View.VISIBLE);
		tvDesc.setText(description);
	}

	private void setQuantity(TextView view, float quantity) {

		String quantityStr = Formatter.formatQuantity(quantity);
		view.setText(quantityStr);
	}

	private void setPrice(TextView tvPrice, TextView tvTotalPrice, double price, float quantity) {

		//  if no price to show, hide the price view
		if (price == 0d) {

			tvPrice.setVisibility(View.GONE);
			tvTotalPrice.setVisibility(View.GONE);
			return;
		}

		tvPrice.setVisibility(View.VISIBLE);
		tvTotalPrice.setVisibility(View.VISIBLE);

		//  assume total price is for one item if quantity not set
		quantity = (quantity == 0f)? 1f: quantity;

		String unitPriceStr = Formatter.formatUnitPrice(price);
		String totalPriceStr = Formatter.formatPrice(price, quantity);

		tvPrice.setText(unitPriceStr);
		tvTotalPrice.setText(totalPriceStr);
	}

	private void setQuantity (TextView tvQuantity, TextView tvMeasUnit, String measUnit, float quantity) {

		//  Hide quantity views if no quantity present to display
		if (quantity == 0) {
			tvQuantity.setVisibility(View.GONE);
			tvMeasUnit.setVisibility(View.GONE);
			return;
		}

		tvQuantity.setVisibility(View.VISIBLE);
		tvMeasUnit.setVisibility(View.VISIBLE);

		measUnit = Formatter.formatMeasUnit(measUnit);

		setQuantity(tvQuantity, quantity);
		tvMeasUnit.setText(measUnit);
	}

	private void setLastsFor (TextView openingBracket, TextView closingBracket,
	                          TextView tvLastsFor, TextView tvLastsFroUnit,
	                          String lastsForUnit, float quantity, float lastsFor) {

		//  Hide lastsFor views if no quantity present to display or lasts for not set
		if (quantity == 0 || lastsFor == 0) {
			openingBracket.setVisibility(View.GONE);
			tvLastsFor.setVisibility(View.GONE);
			tvLastsFroUnit.setVisibility(View.GONE);
			closingBracket.setVisibility(View.GONE);
			return;
		}

		openingBracket.setVisibility(View.VISIBLE);
		tvLastsFor.setVisibility(View.VISIBLE);
		tvLastsFroUnit.setVisibility(View.VISIBLE);
		closingBracket.setVisibility(View.VISIBLE);

		setQuantity(tvLastsFor, lastsFor * quantity);
		tvLastsFroUnit.setText(lastsForUnit);
	}

	private void setInList(CheckBox isInList, long itemID, int inList) {

		boolean flag;
		try {
			if (inList == 1) {
				flag = true;
			} else {
				flag = false;
			}
		} catch (Exception e) {
			flag = false;
		}

		isInList.setChecked(flag);
		isInList.setOnCheckedChangeListener(new OnCheckListener(itemID));
	}

	private class OnCheckListener implements OnCheckedChangeListener {

		private long itemID;

		OnCheckListener(long itemID) {
			this.itemID = itemID;
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

			String selection = ItemEntry._ID + " = ?";
			String[] selectionArgs = new String[] {String.valueOf(itemID)};
			Uri uri = ItemEntry.CONTENT_URI;

			int databaseEntry;
			if (isChecked) {
				databaseEntry = 1;
			} else {
				databaseEntry = 0;
			}

			ContentValues contentValues = new ContentValues();
			contentValues.put(ItemEntry.COLUMN_IN_LIST, databaseEntry);

			mContext.getContentResolver().update(uri, contentValues, selection, selectionArgs);
		}
	}

}