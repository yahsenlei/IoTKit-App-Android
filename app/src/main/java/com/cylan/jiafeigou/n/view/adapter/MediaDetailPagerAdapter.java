package com.cylan.jiafeigou.n.view.adapter;

import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.support.photoview.PhotoView;
import com.cylan.jiafeigou.support.photoview.PhotoViewAttacher;
import com.cylan.jiafeigou.utils.WonderGlideURL;
import com.cylan.jiafeigou.utils.WonderGlideVideoThumbURL;
import com.cylan.jiafeigou.widget.SimpleProgressBar;

import java.util.List;

import static com.cylan.jiafeigou.dp.DpMsgDefine.DPWonderItem;

/**
 * Created by yzd on 16-12-7.
 */

public class MediaDetailPagerAdapter extends PagerAdapter {

    private List<DPWonderItem> mMediaBeanList;
    private final int mStartPosition;
    private boolean mFirstLoad = true;
    private OnReadToShow mReadToShow;
    private PhotoViewAttacher.OnPhotoTapListener mPhotoTapListener;


    public MediaDetailPagerAdapter(List<DPWonderItem> mediaBeanList, int startPosition) {
        mMediaBeanList = mediaBeanList;
        mStartPosition = startPosition;
    }


    @Override
    public int getCount() {
        return mMediaBeanList == null ? 0 : mMediaBeanList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View contentView;
        DPWonderItem bean = mMediaBeanList.get(position);
        ImageView photoView;
        if (bean.msgType == DPWonderItem.TYPE_VIDEO) {
            contentView = View.inflate(container.getContext(), R.layout.view_video_detail, null);
            ViewHolder holder = new ViewHolder(contentView);
            photoView = holder.mPhotoView;
            contentView.setTag(holder);
            ViewCompat.setTransitionName(photoView, position + JConstant.KEY_SHARED_ELEMENT_TRANSITION_NAME_SUFFIX);
            Glide.with(container.getContext())
                    .load(new WonderGlideVideoThumbURL(bean))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.wonderful_pic_place_holder)
                    .listener((mFirstLoad && position == mStartPosition) ? mListener : null)
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            if (photoView == null) return;
                            ViewGroup.LayoutParams lp = photoView.getLayoutParams();
                            if (lp == null) return;
                            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            photoView.setLayoutParams(lp);
                            photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            photoView.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            //破图的位置,属性不一样.
                            if (photoView == null) return;
                            ViewGroup.LayoutParams lp = photoView.getLayoutParams();
                            if (lp == null) return;
                            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            photoView.setLayoutParams(lp);
                            photoView.setScaleType(ImageView.ScaleType.CENTER);
                            photoView.setImageDrawable(errorDrawable);
                        }
                    });
        } else {
            photoView = new PhotoView(container.getContext());
            contentView = photoView;
            ViewCompat.setTransitionName(photoView, position + JConstant.KEY_SHARED_ELEMENT_TRANSITION_NAME_SUFFIX);
            ((PhotoView) photoView).setOnPhotoTapListener(mPhotoTapListener);
            Glide.with(container.getContext())
                    .load(new WonderGlideURL(bean))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.wonderful_pic_place_holder)
                    .error(R.drawable.broken_image)
                    .listener((mFirstLoad && position == mStartPosition) ? mListener : null)
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            if (photoView == null) return;
                            ViewGroup.LayoutParams lp = photoView.getLayoutParams();
                            if (lp == null) return;
                            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            photoView.setLayoutParams(lp);
                            photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            photoView.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            if (photoView == null) return;
                            ViewGroup.LayoutParams lp = photoView.getLayoutParams();
                            if (lp == null) return;
                            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            photoView.setLayoutParams(lp);
                            photoView.setScaleType(ImageView.ScaleType.CENTER);
                            photoView.setImageDrawable(errorDrawable);
                        }
                    });
        }
        container.addView(contentView);
        return contentView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    private RequestListener<GlideUrl, GlideDrawable> mListener = new RequestListener<GlideUrl, GlideDrawable>() {
        @Override
        public boolean onException(Exception e, GlideUrl model, Target<GlideDrawable> target, boolean isFirstResource) {
            if (mFirstLoad && mReadToShow != null) {
                mReadToShow.onReady();
            }
            mFirstLoad = false;
            return false;
        }

        @Override
        public boolean onResourceReady(GlideDrawable resource, GlideUrl model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
            if (mFirstLoad && mReadToShow != null) {
                mReadToShow.onReady();
            }
            mFirstLoad = false;
            return false;
        }
    };

    public void setOnInitFinishListener(OnReadToShow listener) {
        mReadToShow = listener;
    }

    public interface OnReadToShow {
        void onReady();
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
    }

    public static class ViewHolder {
        public ImageView mPhotoView;
        public TextureView mSurfaceView;
        public SimpleProgressBar mProgressBar;

        public ViewHolder(View root) {
            mPhotoView = (ImageView) root.findViewById(R.id.view_video_picture);
            mSurfaceView = (TextureView) root.findViewById(R.id.view_media_video_view);
            mProgressBar = (SimpleProgressBar) root.findViewById(R.id.view_media_video_loading_bar);
        }
    }

    public void setPhotoTapListener(PhotoViewAttacher.OnPhotoTapListener mPhotoTapListener) {
        this.mPhotoTapListener = mPhotoTapListener;
    }
}
