package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> news=new ArrayList<>();
    ArrayList<String> newsUrl=new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView=findViewById(R.id.listview);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,news);
        listView.setAdapter(arrayAdapter);

        DownloadTask task=new DownloadTask();
        try {
            database=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY,articleId INTEGER, articleTitle VARCHAR,articleUrl VARCHAR)");
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        }catch (Exception e) {
            e.printStackTrace();
        }
        updateList();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(getApplicationContext(),NewsActivity.class);
                intent.putExtra("url",newsUrl.get(position));
                startActivity(intent);
            }
        });
    }


    public class DownloadTask extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            String result="";
            URL url;
            HttpURLConnection urlConnection;

            try {
                url=new URL(strings[0]);
                urlConnection=(HttpURLConnection) url.openConnection();

                InputStream inputStream=urlConnection.getInputStream();
                InputStreamReader reader=new InputStreamReader(inputStream);

                int Data=reader.read();

                while (Data!= -1) {
                    char current=(char) Data;
                    result+=current;
                    Data=reader.read();
                }

                JSONArray jsonArray=new JSONArray(result);
                int numberOfItem=jsonArray.length();

                database.execSQL("DELETE FROM articles");

                for (int i=0;i<numberOfItem;i++) {
                    String articleId=jsonArray.getString(i);
                    url=new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                    urlConnection=(HttpURLConnection) url.openConnection();

                    inputStream=urlConnection.getInputStream();
                    reader=new InputStreamReader(inputStream);

                    Data=reader.read();

                    String articleResult="";

                    while (Data!= -1) {
                        char current=(char) Data;
                        articleResult+=current;
                        Data=reader.read();
                    }
                    JSONObject jsonObject=new JSONObject(articleResult);
                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

//                        url=new URL(articleUrl);
//                        urlConnection=(HttpURLConnection) url.openConnection();
//                        inputStream=urlConnection.getInputStream();
//                        reader=new InputStreamReader(inputStream);
//                        Data=reader.read();
//
//                        String urlContent="";
//                        while() {
//                        }
                        String sql="INSERT INTO articles(articleId,articleTitle,articleUrl) VALUES (?, ?, ?)";
                        SQLiteStatement statement=database.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleUrl);

                        statement.execute();

                    }


                }

                return result;
            }catch (Exception e) {
                e.printStackTrace();
                return "failed";
            }

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateList();

        }
    }

    public void updateList() {
        Cursor cursor=database.rawQuery("SELECT * FROM articles",null);

        int titleIndex= cursor.getColumnIndex("articleTitle");
        int urlIndex= cursor.getColumnIndex("articleUrl");

        if (cursor.moveToFirst()) {
            news.clear();
            newsUrl.clear();
            do {
                news.add(cursor.getString(titleIndex));
                newsUrl.add(cursor.getString(urlIndex));

            }while (cursor.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }
}