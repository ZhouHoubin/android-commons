package java.net;

import java.io.InputStream;
import java.io.Streams;
import java.util.HashMap;
import java.util.Iterator;

public class Https {
    public class HttpResult{
        public int code;
        public InputStream errorInputStream;
        public InputStream inputStream;
    }

    /**
     * 请求头
     */
    public class Headers {
        private HashMap<String, String> headers = new HashMap<>();

        public void add(String k, String v) {
            headers.put(k, v);
        }

        /**
         * 从字符串读取头,换行分割
         *
         * @param txt
         * @return
         */
        public Headers from(String txt) {
            try {
                String[] headers = txt.split("\r\n");
                for (String header : headers) {
                    String[] kvs = header.split(":");
                    this.headers.put(kvs[0], kvs[1]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return this;
        }

        private HashMap<String, String> getHeaders() {
            return headers;
        }
    }

    public static Get get(String url) {
        return new Get(url);
    }

    public static class Get {
        private String address;
        private Headers headers;

        public Get(String address) {
            this.address = address;
        }

        public Get headers(Headers headers) {
            this.headers = headers;
            return this;
        }

        public Get data(String data) {
            return this;
        }

        public HttpResult send() {
            HttpResult result = new Https().new HttpResult();
            String content = "";
            try {
                URL url = new URL(address);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(true);
                if (headers != null) {
                    HashMap<String, String> map = headers.getHeaders();
                    for (String k : map.keySet()) {
                        connection.setRequestProperty(k, map.get(k));
                    }
                }
                connection.connect();
                result.code = connection.getResponseCode();
                if (result.code == 302) {
                    String location = connection.getHeaderField("Location");
                    if (!Strings.isEmpty(location)) {
                        address = location;
                        result = send();
                    }
                } else if (result.code != 200) {
                    result.errorInputStream = connection.getErrorStream();
                } else {
                    result.inputStream = connection.getInputStream();
                }
                //connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        public void asyncSend(final DataCallBack callBack) {
            if (callBack != null) {
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        callBack.onCall(send());
                    }
                }.start();
            }
        }
    }

    public static Post post(String url) {
        return new Post(url);
    }

    public static class Post {
        private String address;
        private Headers headers;
        private String data;

        private Post(String address) {
            this.address = address;
        }

        public Post headers(Headers headers) {
            this.headers = headers;
            return this;
        }

        public Post data(String data) {
            this.data = data;
            return this;
        }

        public void asyncSend(final DataCallBack callBack) {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    if (callBack != null) {
                        callBack.onCall(send());
                    }
                }
            }.start();
        }

        public String send() {
            String result = "";
            try {
                URL url = new URL(address);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setInstanceFollowRedirects(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                if (headers != null) {
                    HashMap<String, String> map = headers.getHeaders();
                    for (String k : map.keySet()) {
                        connection.setRequestProperty(k, map.get(k));
                    }
                }
                connection.connect();
                Streams.write(connection.getOutputStream(), data);
                if (connection.getResponseCode() != 200) {
                    result = Streams.stream2String(connection.getErrorStream());
                } else {
                    result = Streams.stream2String(connection.getInputStream());
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    }
}
