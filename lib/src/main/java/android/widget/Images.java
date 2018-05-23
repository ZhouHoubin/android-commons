package android.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.Https;

public class Images {

    public Images() {

    }

    public static class Builder {
        private Context context;

        private Bitmap bitmap;

        private int flag = 0;

        private Object dist;

        Handler handler = new Handler();

        public Builder with(Context context) {
            this.context = context;
            return this;
        }

        public Builder assets(String name) {
            flag = 0;
            dist = name;
            InputStream inputStream = null;
            try {
                inputStream = context.getAssets().open(name);
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return this;
        }

        public Builder drawable(int id) {
            flag = 1;
            dist = id;
            bitmap = BitmapFactory.decodeResource(context.getResources(), id);
            return this;
        }

        public Builder network(String url) {
            flag = 2;
            dist = url;
            return this;
        }

        private void asyncLoad(final ImageView image, final boolean background) {
            if (flag == 2) {
                Https.get(String.valueOf(dist)).asyncSend(new DataCallBack() {
                    @Override
                    public void onCall(Object object) {
                        if (object != null && object instanceof Https.HttpResult) {
                            Https.HttpResult result = (Https.HttpResult) object;
                            if (result.code == 200) {
                                bitmap = BitmapFactory.decodeStream(result.inputStream);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (background) {
                                            image.setBackground(new BitmapDrawable(context.getResources(), bitmap));
                                        } else {
                                            image.setImageBitmap(bitmap);
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
            }
        }

        public Builder intoImage(ImageView imageView) {
            if (flag == 2) {
                asyncLoad(imageView, false);
            } else if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
            return this;
        }

        public Builder intoBackground(ImageView imageView) {
            if (flag == 2) {
                asyncLoad(imageView, true);
            } else if (bitmap != null) {
                imageView.setBackground(new BitmapDrawable(context.getResources(), bitmap));
            }
            return this;
        }
    }
}
