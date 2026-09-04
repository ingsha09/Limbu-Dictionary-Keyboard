package com.kisolabs.limbudictionary;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SketchwareUtil {

  public static final int TOP = 1;
  public static final int CENTER = 2;
  public static final int BOTTOM = 3;

  public static void CustomToast(Context context, String message, int textColor, int textSize,
      int bgColor, int radius, int gravity) {
    Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
    View view = toast.getView();

    // Android 11+ returns null for custom views on standard toasts
    if (view != null) {
      TextView textView = view.findViewById(android.R.id.message);
      if (textView != null) {
        textView.setTextSize(textSize);
        textView.setTextColor(textColor);
        textView.setGravity(Gravity.CENTER);
      }

      GradientDrawable gradientDrawable = new GradientDrawable();
      gradientDrawable.setColor(bgColor);
      gradientDrawable.setCornerRadius(radius);
      view.setBackground(gradientDrawable);
      view.setPadding(15, 10, 15, 10);
      view.setElevation(10);
    }

    setToastGravity(toast, gravity);
    toast.show();
  }

  public static void CustomToastWithIcon(Context context, String message, int textColor,
      int textSize, int bgColor, int radius, int gravity, int icon) {
    Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
    View view = toast.getView();

    if (view != null) {
      TextView textView = view.findViewById(android.R.id.message);
      if (textView != null) {
        textView.setTextSize(textSize);
        textView.setTextColor(textColor);
        textView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
        textView.setGravity(Gravity.CENTER);
        textView.setCompoundDrawablePadding(10);
      }

      GradientDrawable gradientDrawable = new GradientDrawable();
      gradientDrawable.setColor(bgColor);
      gradientDrawable.setCornerRadius(radius);
      view.setBackground(gradientDrawable);
      view.setPadding(10, 10, 10, 10);
      view.setElevation(10);
    }

    setToastGravity(toast, gravity);
    toast.show();
  }

  private static void setToastGravity(Toast toast, int gravity) {
    switch (gravity) {
      case TOP:
        toast.setGravity(Gravity.TOP, 0, 150);
        break;
      case CENTER:
        toast.setGravity(Gravity.CENTER, 0, 0);
        break;
      case BOTTOM:
        toast.setGravity(Gravity.BOTTOM, 0, 150);
        break;
    }
  }

  public static void sortListMap(final ArrayList<HashMap<String, Object>> listMap, final String key,
      final boolean isNumber, final boolean ascending) {
    if (listMap == null || key == null) return;

    Collections.sort(listMap, new Comparator<HashMap<String, Object>>() {
      @Override
      public int compare(HashMap<String, Object> map1, HashMap<String, Object> map2) {
        Object val1 = map1 != null ? map1.get(key) : null;
        Object val2 = map2 != null ? map2.get(key) : null;

        if (val1 == null && val2 == null) return 0;
        if (val1 == null) return ascending ? -1 : 1;
        if (val2 == null) return ascending ? 1 : -1;

        if (isNumber) {
          try {
            double num1 = Double.parseDouble(val1.toString());
            double num2 = Double.parseDouble(val2.toString());
            return ascending ? Double.compare(num1, num2) : Double.compare(num2, num1);
          } catch (NumberFormatException e) {
            return 0;
          }
        } else {
          String str1 = val1.toString();
          String str2 = val2.toString();
          return ascending ? str1.compareTo(str2) : str2.compareTo(str1);
        }
      }
    });
  }

  public static void CropImage(Activity activity, String path, int requestCode) {
    try {
      Intent intent = new Intent("com.android.camera.action.CROP");
      File file = new File(path);

      Uri contentUri;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        contentUri = FileProvider.getUriForFile(activity,
            activity.getPackageName() + ".provider", file);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      } else {
        contentUri = Uri.fromFile(file);
      }

      intent.setDataAndType(contentUri, "image/*");
      intent.putExtra("crop", "true");
      intent.putExtra("aspectX", 1);
      intent.putExtra("aspectY", 1);
      intent.putExtra("outputX", 280);
      intent.putExtra("outputY", 280);
      intent.putExtra("return-data", false);
      activity.startActivityForResult(intent, requestCode);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(activity, "Your device doesn't support the crop action!", Toast.LENGTH_SHORT).show();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static boolean isConnected(Context context) {
    if (context == null) return false;
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm == null) return false;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Network network = cm.getActiveNetwork();
      if (network == null) return false;
      NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
      return capabilities != null && (
          capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
          capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
          capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    } else {
      NetworkInfo info = cm.getActiveNetworkInfo();
      return info != null && info.isConnected();
    }
  }

  public static String copyFromInputStream(InputStream inputStream) {
    if (inputStream == null) return "";
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buf = new byte[2048];
    int len;
    try {
      while ((len = inputStream.read(buf)) != -1) {
        outputStream.write(buf, 0, len);
      }
      return outputStream.toString(StandardCharsets.UTF_8.name());
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        inputStream.close();
      } catch (IOException ignored) {}
    }
    return "";
  }

  public static void hideKeyboard(Activity activity) {
    if (activity == null) return;
    View view = activity.getCurrentFocus();
    if (view != null) {
      InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
      if (imm != null) {
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
      }
    }
  }

  public static void showKeyboard(Context context, View view) {
    if (context == null || view == null) return;
    view.requestFocus();
    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }
  }

  public static void showMessage(Context context, String message) {
    if (context != null) {
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
  }

  public static int getLocationX(View view) {
    if (view == null) return 0;
    int[] location = new int[2];
    view.getLocationInWindow(location);
    return location[0];
  }

  public static int getLocationY(View view) {
    if (view == null) return 0;
    int[] location = new int[2];
    view.getLocationInWindow(location);
    return location[1];
  }

  public static int getRandom(int min, int max) {
    if (min >= max) return min;
    Random random = new Random();
    return random.nextInt((max - min) + 1) + min;
  }

  public static ArrayList<Double> getCheckedItemPositionsToArray(ListView list) {
    ArrayList<Double> result = new ArrayList<>();
    if (list == null) return result;

    SparseBooleanArray arr = list.getCheckedItemPositions();
    if (arr != null) {
      for (int i = 0; i < arr.size(); i++) {
        if (arr.valueAt(i)) {
          result.add((double) arr.keyAt(i));
        }
      }
    }
    return result;
  }

  public static float getDip(Context context, int input) {
    if (context == null) return 0f;
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, input,
        context.getResources().getDisplayMetrics());
  }

  public static int getDisplayWidthPixels(Context context) {
    if (context == null) return 0;
    return context.getResources().getDisplayMetrics().widthPixels;
  }

  public static int getDisplayHeightPixels(Context context) {
    if (context == null) return 0;
    return context.getResources().getDisplayMetrics().heightPixels;
  }

  public static void getAllKeysFromMap(Map<String, Object> map, ArrayList<String> output) {
    if (output == null) return;
    output.clear();
    if (map == null || map.isEmpty()) return;

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      output.add(entry.getKey());
    }
  }
}
