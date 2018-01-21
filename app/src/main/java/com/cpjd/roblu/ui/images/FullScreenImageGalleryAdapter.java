package com.cpjd.roblu.ui.images;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.cpjd.roblu.R;
import com.cpjd.roblu.utils.Utils;

import java.util.ArrayList;


public class FullScreenImageGalleryAdapter extends PagerAdapter {

    private ArrayList<byte[]> images;
    private FullScreenImageLoader fullScreenImageLoader;

    public interface FullScreenImageLoader {
        void loadFullScreenImage(ImageView iv, byte[] image, int width, LinearLayout bglinearLayout);
    }

    FullScreenImageGalleryAdapter(ArrayList<byte[]> images) {
        this.images = images;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) container.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.fullscreen_image, null);

        ImageView imageView = view.findViewById(R.id.iv);
        final LinearLayout linearLayout = view.findViewById(R.id.ll);

        Context context = imageView.getContext();
        int width = Utils.getScreenWidth(context);

        fullScreenImageLoader.loadFullScreenImage(imageView, ImageGalleryActivity.IMAGES.get(position), width, linearLayout);

        container.addView(view, 0);

        return view;
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    void setFullScreenImageLoader(FullScreenImageLoader loader) {
        this.fullScreenImageLoader = loader;
    }
}
