package org.oucho.radio2.images;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;

import org.oucho.radio2.R;

import java.io.ByteArrayOutputStream;


public class ImageFactory {

    public static Bitmap getResizedBitmap(Context context, Bitmap image) { // redimessione l'image par matrice (plus propre qu'un simple redimenssionnement)

        int logoSize = context.getResources().getDimensionPixelSize(R.dimen.logo_full);

        int newWidth;
        int newHeight;

        if (image.getWidth() > image.getHeight()) {
            float aspectRatio = image.getWidth() / (float) image.getHeight();
            newWidth = logoSize;
            newHeight = Math.round(newWidth / aspectRatio);
        } else {
            float aspectRatio =  image.getHeight() / (float) image.getWidth();
            newHeight = logoSize;
            newWidth = Math.round(newHeight / aspectRatio);
        }


        int width = image.getWidth();
        int height = image.getHeight();

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(image, 0, 0, width, height, matrix, false);
        image.recycle();
        return resizedBitmap;
    }


    // convert from bitmap to byte array
    public static byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    // convert from byte array to bitmap
    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    // convert from string to bitmap
    public static Bitmap stringToBitmap(String image) {

        byte[] img = Base64.decode(image, Base64.DEFAULT);

        return getImage(img);
    }


    // convert from byte array to string
    public static String byteToString(byte[] logo) {

        return bitmapToString(getImage(logo));
    }

    // convert from bitmap to string
    public static String bitmapToString(Bitmap image) {

        //Bitmap one = BitmapFactory.decodeResource(image);
        byte[] two = getBytes(image);

        return Base64.encodeToString(two, Base64.DEFAULT);
    }

    // convert from drawable to string
    public static String drawableResourceToBitmap(Context context, int drawable) {

        Bitmap one = BitmapFactory.decodeResource(context.getResources(), drawable);
        byte[] two = getBytes(one);

        return Base64.encodeToString(two, Base64.DEFAULT);
    }



}
