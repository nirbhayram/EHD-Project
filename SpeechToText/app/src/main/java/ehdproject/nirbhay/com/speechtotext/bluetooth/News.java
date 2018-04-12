package ehdproject.nirbhay.com.speechtotext.bluetooth;


public class News {
    static String news[];

    public static String[] getNews() {
        return news;
    }

    public static void setNews(String[] news) {
        News.news = new String[news.length];
        for ( int i=0;i<news.length;i++){
            News.news[i] = news[i];
        }
    }
}
