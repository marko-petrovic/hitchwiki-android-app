package com.dualquo.te.hitchwiki.extended;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.dualquo.te.hitchwiki.R;

public class LinearLayoutMarkerDetailsMenu extends LinearLayout
{
    private Animation inAnimation;
    private Animation outAnimation;

    public LinearLayoutMarkerDetailsMenu(Context context)
    {
        super(context);
    }
    
    public LinearLayoutMarkerDetailsMenu(Context context, AttributeSet attributeSet)
    {
    	super(context, attributeSet);
    	inAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_up);
    	outAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_down);
    }

    public void setInAnimation(Animation inAnimation)
    {
        this.inAnimation = inAnimation;
    }

    public void setOutAnimation(Animation outAnimation)
    {
        this.outAnimation = outAnimation;
    }

    @Override
    public void setVisibility(int visibility)
    {
        if (getVisibility() != visibility)
        {
            if (visibility == VISIBLE)
            {
                if (inAnimation != null) startAnimation(inAnimation);
            }
            else if ((visibility == INVISIBLE) || (visibility == GONE))
            {
                if (outAnimation != null) startAnimation(outAnimation);
            }
        }

        super.setVisibility(visibility);
    }
}
