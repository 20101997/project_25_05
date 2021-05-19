package com.caen.easyController;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RFIDTagAdapter extends ArrayAdapter<RFIDTag> {

	ArrayList<RFIDTag> items;

	public RFIDTagAdapter(Context context, int resource) {
		super(context, resource);
		items = new ArrayList<RFIDTag>();
	}

	static class ViewHolderItem {
		TextView epc;
		TextView counter;
	}

	ViewHolderItem viewHolder;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout view =(LinearLayout) convertView;
		if (view == null) {
			LayoutInflater vi = (LayoutInflater) this.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = (LinearLayout) vi.inflate(R.layout.inventory_row,
					null);

			viewHolder = new ViewHolderItem();
			viewHolder.epc = view
					.findViewById(R.id.inventory_item);
			viewHolder.counter = view
					.findViewById(R.id.inventory_item_counter);
			view.setTag(viewHolder);
		} else {

			viewHolder = (ViewHolderItem) view.getTag();
		}
		RFIDTag item = items.get(position);
		if (item != null) {
			if (viewHolder != null) {
				viewHolder.epc.setText(item.getId());
				viewHolder.counter.setText(Integer.toString(item
						.getCounter()));
			}
		}
		if(((TextView)view.getChildAt(0)).getText().equals("")){
			System.out.println();
		}
		return view;
	}
	public void clear(){
		super.clear();
		items.clear();
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public RFIDTag getItem(int position) {
		return items.get(position);
	}

	@Override
	public int getPosition(RFIDTag item) {
		return items.indexOf(item);
	}

	@Override
	public void insert(RFIDTag object, int index) {

		if ((index >= 0) || (index < (items.size())))
			items.add(index, object);
	}

	@Override
	public void remove(RFIDTag object) {
		items.remove(object);
	}

	public void addTag(RFIDTag rfidTag, short maxRssi, short minRssi) {
		int itemIndex = items.indexOf(rfidTag);
		if (itemIndex == -1)
			items.add(rfidTag);
		else {
			items.get(itemIndex).setCounter(
					items.get(itemIndex).getCounter() + 1);

		}
		this.notifyDataSetChanged();
	}
}
