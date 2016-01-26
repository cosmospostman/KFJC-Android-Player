package org.kfjc.android.player.activity;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.kfjc.android.player.R;

public class NavigationListAdapter extends ArrayAdapter<HomeScreenDrawerActivity.NavItem> {

    private Context context;
    private HomeScreenDrawerActivity.NavItem[] items;

    public NavigationListAdapter(Context context, HomeScreenDrawerActivity.NavItem[] items) {
        super(context, -1, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.list_navigationitem, parent, false);
        ImageView iconView = (ImageView) view.findViewById(R.id.navlistitem_icon);
        TextView textView = (TextView) view.findViewById(R.id.navlistitem_label);

        HomeScreenDrawerActivity.NavItem item = items[position];
        iconView.setImageResource(item.icon);
        textView.setText(item.label);

        

        return view;
    }

    private boolean firstTime = true;
}
