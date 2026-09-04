package com.kisolabs.limbudictionary;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SplashActivity extends AppCompatActivity {

  private LinearLayout containerBrand;
  private LinearLayout linear2;
  private ImageView imgLogo;
  private LinearLayout containerText;
  private TextView textTitleTop;
  private TextView textTitleBottom;
  private TextView textVersion;
  private TextView textDeveloper;

  private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
  private Runnable navigateRunnable;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    SplashScreen.installSplashScreen(this);

    super.onCreate(savedInstanceState);

    enableEdgeToEdge();
    setContentView(R.layout.splash);
    initialize();
    initializeLogic();
  }

  private void enableEdgeToEdge() {
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    
    int bgColor = 0xFFFCFDFE;
    getWindow().setStatusBarColor(bgColor);
    getWindow().setNavigationBarColor(bgColor);

    WindowInsetsControllerCompat controller = 
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
    if (controller != null) {
      controller.setAppearanceLightStatusBars(true);
      controller.setAppearanceLightNavigationBars(true);
    }
  }

  private void initialize() {
    containerBrand = findViewById(R.id.container_brand);
    linear2 = findViewById(R.id.linear2);
    imgLogo = findViewById(R.id.img_logo);
    containerText = findViewById(R.id.container_text);
    textTitleTop = findViewById(R.id.text_title_top);
    textTitleBottom = findViewById(R.id.text_title_bottom);
    textVersion = findViewById(R.id.text_version);
    textDeveloper = findViewById(R.id.text_developer);

    View rootLayout = findViewById(R.id.splash_root);
    if (rootLayout != null) {
      ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
        return insets;
      });
    }
  }

  private void initializeLogic() {
    // Dynamically retrieve versionName from package manifest
    String versionName = "1.0";
    try {
      versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    } catch (Exception e) {
      e.printStackTrace();
    }
    textVersion.setText("Version " + versionName);

    Typeface googleSans;
    try {
      googleSans = Typeface.createFromAsset(getAssets(), "fonts/googlesans.ttf");
    } catch (Exception e) {
      googleSans = Typeface.DEFAULT;
    }

    if (googleSans != null) {
      textTitleTop.setTypeface(googleSans, Typeface.BOLD);
      textTitleBottom.setTypeface(googleSans, Typeface.BOLD);
      textVersion.setTypeface(googleSans, Typeface.NORMAL);
      textDeveloper.setTypeface(googleSans, Typeface.BOLD);
    }

    containerText.setAlpha(0f);

    containerText.post(() -> {
      if (isFinishing() || isDestroyed()) return;

      float textWidth = containerText.getWidth() + 16f;
      float initialShift = textWidth / 2f;

      imgLogo.setTranslationX(initialShift);
      containerText.setTranslationX(-initialShift);

      imgLogo.setAlpha(0f);
      imgLogo.animate()
          .alpha(1f)
          .setDuration(400)
          .withEndAction(() -> {
            if (isFinishing() || isDestroyed()) return;

            imgLogo.animate()
                .translationX(0f)
                .setDuration(700)
                .setInterpolator(new DecelerateInterpolator())
                .start();

            containerText.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(700)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                  navigateRunnable = () -> {
                    if (!isFinishing() && !isDestroyed()) {
                      Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                      startActivity(intent);
                      overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                      finish();
                    }
                  };
                  timeoutHandler.postDelayed(navigateRunnable, 800);
                })
                .start();
          })
          .start();
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (navigateRunnable != null) {
      timeoutHandler.removeCallbacks(navigateRunnable);
    }
  }
}
