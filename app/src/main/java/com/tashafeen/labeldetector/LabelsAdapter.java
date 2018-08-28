package com.tashafeen.labeldetector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tashafeen.labeldetector.POJOs.LabelInfo;

import java.util.ArrayList;

public class LabelsAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<LabelInfo> mDataSource;


    public LabelsAdapter(Context context, ArrayList<LabelInfo> items) {
        mContext = context;
        mDataSource = items;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mDataSource.size();
    }

    @Override
    public Object getItem(int position) {
        return mDataSource.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = mInflater.inflate(R.layout.label_info_layout, parent, false);

        TextView labelNameTextView =
                rowView.findViewById(R.id.label_text);

        TextView entityIDTextView =
                rowView.findViewById(R.id.label_entity_id);

        TextView confidenceTextView =
                rowView.findViewById(R.id.label_confidence);


        LabelInfo label = (LabelInfo) getItem(position);
        labelNameTextView.setText(label.getText());
        entityIDTextView.setText(label.getEntityId());
        confidenceTextView.setText(String.format("%f",label.getConfidence()));

        return rowView;
    }
}
