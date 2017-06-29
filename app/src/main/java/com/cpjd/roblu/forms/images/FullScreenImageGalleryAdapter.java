package com.cpjd.roblu.forms.images;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.etiennelawlor.imagegallery.library.R;
import com.etiennelawlor.imagegallery.library.utilities.DisplayUtility;

import java.io.File;
import java.util.ArrayList;

/**
 * Copyright 2015 Etienne Lawlor

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 Modifications were made to this file.

 @since 3.5.0
 */
public class FullScreenImageGalleryAdapter extends PagerAdapter {

    private final ArrayList<File> images;
    private FullScreenImageLoader fullScreenImageLoader;

    public interface FullScreenImageLoader {
        void loadFullScreenImage(ImageView iv, File imageUrl, int width, LinearLayout bglinearLayout);
    }

    FullScreenImageGalleryAdapter(ArrayList<File> images) {
        this.images = images;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) container.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.fullscreen_image, null);

        ImageView imageView = (ImageView) view.findViewById(R.id.iv);
        final LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.ll);

        File image = images.get(position);

        Context context = imageView.getContext();
        int width = DisplayUtility.getScreenWidth(context);

        fullScreenImageLoader.loadFullScreenImage(imageView, image, width, linearLayout);

        container.addView(view, 0);

        return view;
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    void setFullScreenImageLoader(FullScreenImageLoader loader) {
        this.fullScreenImageLoader = loader;
    }
}