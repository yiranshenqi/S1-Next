package cl.monsoon.s1next.binding;

import android.content.res.ColorStateList;
import android.databinding.BindingAdapter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.widget.ImageView;

import cl.monsoon.s1next.R;

public final class ImageViewBindingAdapter {

    private ImageViewBindingAdapter() {

    }

    @BindingAdapter("imageDrawable")
    public static void setImageDrawable(ImageView imageView, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @ColorInt int rippleColor = imageView.getContext().getResources().getColor(
                    R.color.ripple_material_dark);
            // add ripple effect
            RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(rippleColor),
                    drawable, null);
            imageView.setImageDrawable(rippleDrawable);
        } else {
            imageView.setImageDrawable(drawable);
        }
    }
}
