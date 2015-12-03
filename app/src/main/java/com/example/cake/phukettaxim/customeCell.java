package com.example.cake.phukettaxim;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/**
 * Created by cake on 8/8/15 AD.
 */
public class customeCell extends ArrayAdapter<driverModel> {

    public customeCell(Context context, int resource, List<driverModel> objects) {
        super(context, resource, objects);
    }




    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        driverModel user = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.customcell, parent, false);
        }
        // Lookup view for data population
        TextView tvDriverName = (TextView) convertView.findViewById(R.id.driverName);
        TextView tvCarmodel = (TextView) convertView.findViewById(R.id.carModel);
        TextView distance = (TextView) convertView.findViewById(R.id.distance);
        ImageView imDriverImage = (ImageView) convertView.findViewById(R.id.img);
        // Populate the data into the template view using the data object
        tvDriverName.setText(user.driverName);
        tvCarmodel.setText(user.carModel);
        distance.setText(user.distance);
       // Bitmap bMapScaled = getResizedBitmap(user.im, 64, 64);
        imDriverImage.setImageBitmap(user.im);
        // Return the completed view to render on screen
        return convertView;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

}
