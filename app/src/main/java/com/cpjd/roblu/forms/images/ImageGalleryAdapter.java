package com.cpjd.roblu.forms.images;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.etiennelawlor.imagegallery.library.R;
import com.etiennelawlor.imagegallery.library.utilities.DisplayUtility;

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
public class ImageGalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<byte[]> images;
    private final int gridItemWidth;
    private OnImageClickListener onImageClickListener;
    private ImageThumbnailLoader imageThumbnailLoader;

    interface OnImageClickListener {
        void onImageClick(int position);
    }

    public interface ImageThumbnailLoader {
        void loadImageThumbnail(ImageView iv, byte[] image, int dimension);
    }

    ImageGalleryAdapter(Context context, ArrayList<byte[]> images) {
        this.images = images;

        int screenWidth = DisplayUtility.getScreenWidth(context);
        int numOfColumns;
        if (DisplayUtility.isInLandscapeMode(context)) {
            numOfColumns = 4;
        } else {
            numOfColumns = 3;
        }

        gridItemWidth = screenWidth / numOfColumns;
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.image_thumbnail, viewGroup, false);
        v.setLayoutParams(getGridItemLayoutParams(v));

        return new ImageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        final ImageViewHolder holder = (ImageViewHolder) viewHolder;

        byte[] image = images.get(position);

        imageThumbnailLoader.loadImageThumbnail(holder.imageView, image, gridItemWidth);

        holder.frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPos = holder.getAdapterPosition();
                if(adapterPos != RecyclerView.NO_POSITION){
                    if (onImageClickListener != null) {
                        onImageClickListener.onImageClick(adapterPos);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (images != null) {
            return images.size();
        } else {
            return 0;
        }
    }

    void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }

    void setImageThumbnailLoader(ImageThumbnailLoader loader) {
        this.imageThumbnailLoader = loader;
    }

    private ViewGroup.LayoutParams getGridItemLayoutParams(View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

        layoutParams.width = gridItemWidth;
        //noinspection SuspiciousNameCombination
        layoutParams.height = gridItemWidth;

        return layoutParams;
    }
    private static class ImageViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final FrameLayout frameLayout;

        ImageViewHolder(final View view) {
            super(view);

            imageView = (ImageView) view.findViewById(R.id.iv);
            frameLayout = (FrameLayout) view.findViewById(R.id.fl);
        }
    }
}
