package com.kisolabs.limbudictionary;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class FileUtil {

  public static void createNewFile(String path) {
    int lastSep = path.lastIndexOf(File.separator);
    if (lastSep > 0) {
      String dirPath = path.substring(0, lastSep);
      makeDir(dirPath);
    }

    File file = new File(path);
    try {
      if (!file.exists()) {
        file.createNewFile();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String readFile(String path) {
    if (!isExistFile(path)) return "";

    StringBuilder sb = new StringBuilder();
    try (FileInputStream fis = new FileInputStream(path);
         InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {

      char[] buff = new char[1024];
      int length;
      while ((length = isr.read(buff)) > 0) {
        sb.append(buff, 0, length);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  public static void writeFile(String path, String str) {
    createNewFile(path);
    try (FileOutputStream fos = new FileOutputStream(path, false);
         OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
      osw.write(str);
      osw.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void copyFile(String sourcePath, String destPath) {
    if (!isExistFile(sourcePath)) return;
    createNewFile(destPath);

    try (FileInputStream fis = new FileInputStream(sourcePath);
         FileOutputStream fos = new FileOutputStream(destPath, false)) {

      byte[] buff = new byte[2048];
      int length;
      while ((length = fis.read(buff)) > 0) {
        fos.write(buff, 0, length);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void copyDir(String oldPath, String newPath) {
    File oldFile = new File(oldPath);
    File[] files = oldFile.listFiles();
    File newFile = new File(newPath);
    if (!newFile.exists()) {
      newFile.mkdirs();
    }
    if (files != null) {
      for (File file : files) {
        if (file.isFile()) {
          copyFile(file.getPath(), newPath + File.separator + file.getName());
        } else if (file.isDirectory()) {
          copyDir(file.getPath(), newPath + File.separator + file.getName());
        }
      }
    }
  }

  public static void moveFile(String sourcePath, String destPath) {
    copyFile(sourcePath, destPath);
    deleteFile(sourcePath);
  }

  public static void deleteFile(String path) {
    File file = new File(path);

    if (!file.exists()) return;

    if (file.isFile()) {
      file.delete();
      return;
    }

    File[] fileArr = file.listFiles();
    if (fileArr != null) {
      for (File subFile : fileArr) {
        if (subFile.isDirectory()) {
          deleteFile(subFile.getAbsolutePath());
        } else if (subFile.isFile()) {
          subFile.delete();
        }
      }
    }
    file.delete();
  }

  public static boolean isExistFile(String path) {
    if (path == null) return false;
    File file = new File(path);
    return file.exists();
  }

  public static void makeDir(String path) {
    if (!isExistFile(path)) {
      File file = new File(path);
      file.mkdirs();
    }
  }

  public static void listDir(String path, ArrayList<String> list) {
    File dir = new File(path);
    if (!dir.exists() || dir.isFile() || list == null) return;

    File[] listFiles = dir.listFiles();
    if (listFiles == null) return;

    list.clear();
    for (File file : listFiles) {
      list.add(file.getAbsolutePath());
    }
  }

  public static boolean isDirectory(String path) {
    return isExistFile(path) && new File(path).isDirectory();
  }

  public static boolean isFile(String path) {
    return isExistFile(path) && new File(path).isFile();
  }

  public static long getFileLength(String path) {
    if (!isExistFile(path)) return 0;
    return new File(path).length();
  }

  public static String getPackageDataDir(Context context) {
    File filesDir = context.getExternalFilesDir(null);
    return (filesDir != null) ? filesDir.getAbsolutePath() : context.getFilesDir().getAbsolutePath();
  }

  public static String convertUriToFilePath(final Context context, final Uri uri) {
    if (uri == null) return null;
    String path = null;

    if (DocumentsContract.isDocumentUri(context, uri)) {
      if (isExternalStorageDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
          path = context.getExternalFilesDir(null) + "/" + split[1];
        }
      } else if (isDownloadsDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        if (!android.text.TextUtils.isEmpty(docId)) {
          if (docId.startsWith("raw:")) {
            return docId.replaceFirst("raw:", "");
          }
          try {
            final Uri contentUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), Long.parseLong(docId));
            path = getDataColumn(context, contentUri, null, null);
          } catch (NumberFormatException e) {
            return null;
          }
        }
      } else if (isMediaDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[] { split[1] };
        path = getDataColumn(context, contentUri, selection, selectionArgs);
      }
    } else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
      path = getDataColumn(context, uri, null, null);
    } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
      path = uri.getPath();
    }

    if (path != null) {
      try {
        return URLDecoder.decode(path, StandardCharsets.UTF_8.name());
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
    if (uri == null) return null;
    final String column = MediaStore.Images.Media.DATA;
    final String[] projection = { column };

    try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
      if (cursor != null && cursor.moveToFirst()) {
        final int columnIndex = cursor.getColumnIndex(column);
        if (columnIndex != -1) {
          return cursor.getString(columnIndex);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  private static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  private static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }

  private static void saveBitmap(Bitmap bitmap, String destPath) {
    if (bitmap == null) return;
    createNewFile(destPath);
    try (FileOutputStream out = new FileOutputStream(destPath)) {
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static Bitmap getScaledBitmap(String path, int max) {
    Bitmap src = decodeSampleBitmapFromPath(path, max, max);
    if (src == null) return null;

    int width = src.getWidth();
    int height = src.getHeight();
    float rate;

    if (width > height) {
      rate = max / (float) width;
      height = (int) (height * rate);
      width = max;
    } else {
      rate = max / (float) height;
      width = (int) (width * rate);
      height = max;
    }

    Bitmap scaled = Bitmap.createScaledBitmap(src, width, height, true);
    if (scaled != src) {
      src.recycle();
    }
    return scaled;
  }

  public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {
      final int halfHeight = height / 2;
      final int halfWidth = width / 2;

      while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
        inSampleSize *= 2;
      }
    }
    return inSampleSize;
  }

  public static Bitmap decodeSampleBitmapFromPath(String path, int reqWidth, int reqHeight) {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(path, options);

    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(path, options);
  }

  public static void resizeBitmapFileRetainRatio(String fromPath, String destPath, int max) {
    if (!isExistFile(fromPath)) return;
    Bitmap bitmap = getScaledBitmap(fromPath, max);
    saveBitmap(bitmap, destPath);
    if (bitmap != null) bitmap.recycle();
  }

  public static void resizeBitmapFileToSquare(String fromPath, String destPath, int max) {
    if (!isExistFile(fromPath)) return;
    Bitmap src = decodeSampleBitmapFromPath(fromPath, max, max);
    if (src == null) return;
    Bitmap bitmap = Bitmap.createScaledBitmap(src, max, max, true);
    saveBitmap(bitmap, destPath);
    src.recycle();
    if (bitmap != src) bitmap.recycle();
  }

  public static void resizeBitmapFileToCircle(String fromPath, String destPath) {
    if (!isExistFile(fromPath)) return;
    Bitmap src = decodeSampleBitmapFromPath(fromPath, 500, 500);
    if (src == null) return;

    Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);

    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Rect rect = new Rect(0, 0, src.getWidth(), src.getHeight());

    canvas.drawARGB(0, 0, 0, 0);
    paint.setColor(0xff424242);
    canvas.drawCircle(src.getWidth() / 2f, src.getHeight() / 2f, src.getWidth() / 2f, paint);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(src, rect, rect, paint);

    saveBitmap(bitmap, destPath);
    src.recycle();
    bitmap.recycle();
  }

  public static int getJpegRotate(String filePath) {
    int rotate = 0;
    try {
      ExifInterface exif = new ExifInterface(filePath);
      int iOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);

      switch (iOrientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          rotate = 90;
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          rotate = 180;
          break;
        case ExifInterface.ORIENTATION_ROTATE_270:
          rotate = 270;
          break;
      }
    } catch (IOException e) {
      return 0;
    }
    return rotate;
  }

  public static File createNewPictureFile(Context context) {
    SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
    String fileName = date.format(new Date()) + ".jpg";
    File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    return new File(dir, fileName);
  }
}
