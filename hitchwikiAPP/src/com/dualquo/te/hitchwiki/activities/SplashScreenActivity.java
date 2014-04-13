package com.dualquo.te.hitchwiki.activities;

import com.dualquo.te.hitchwiki.R;
import com.dualquo.te.hitchwiki.R.layout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreenActivity extends Activity
{
    // display time, in milliseconds:
    private final int SPLASH_DISPLAY_LENGTH = 2500;
 
    @Override 
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.splash_layout);
    }
 
    @Override 
    protected void onResume()  
    {
        super.onResume();

            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    //Finish the splash activity so it can't be returned to.
                	SplashScreenActivity.this.finish();
                    // Create an Intent that will start the main activity.
                    Intent mainIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    SplashScreenActivity.this.startActivity(mainIntent);
                }
            }, SPLASH_DISPLAY_LENGTH);
    }
    
    @Override
    public void onBackPressed()
	{
    	//blocking back button while Splash Screen is on
	}
}