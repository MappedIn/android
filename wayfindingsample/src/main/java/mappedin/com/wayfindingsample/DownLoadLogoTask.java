package mappedin.com.wayfindingsample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

/**
 * Created by Peter on 2018-02-27.
 */

public class DownLoadLogoTask extends AsyncTask<URL, Void, Bitmap> {
    private WeakReference<ImageView> imageView;

    public DownLoadLogoTask(ImageView imageView){
        this.imageView = new WeakReference<>(imageView);
    }

    protected Bitmap doInBackground(URL...urls){
        Bitmap logo = null;
        try{
            InputStream is = urls[0].openStream();
            logo = BitmapFactory.decodeStream(is);
        }catch(Exception e){ // Catch the download exception
            e.printStackTrace();
        }
        return logo;
    }

    protected void onPostExecute(Bitmap result){ imageView.get().setImageBitmap(result); }
}
