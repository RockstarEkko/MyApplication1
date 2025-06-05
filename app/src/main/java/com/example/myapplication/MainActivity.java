package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button fetchButton;
    private ProgressBar progressBar;
    private TextView resultTextView;
    private TextView errorTextView;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fetchButton = findViewById(R.id.fetchButton);
        progressBar = findViewById(R.id.progressBar);
        resultTextView = findViewById(R.id.resultTextView);
        errorTextView = findViewById(R.id.errorTextView);

        // 创建线程池和主线程Handler
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        fetchButton.setOnClickListener(v -> fetchExchangeRate());
    }

    private void fetchExchangeRate() {
        // 显示进度条，隐藏错误信息
        progressBar.setVisibility(View.VISIBLE);
        errorTextView.setVisibility(View.GONE);
        fetchButton.setEnabled(false);

        executorService.execute(() -> {
            try {
                // 获取中国银行外汇牌价页面
                Document doc = Jsoup.connect("https://www.boc.cn/sourcedb/whpj/")
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .get();

                // 解析HTML获取美元汇率
                Elements tables = doc.select("table");
                if (tables.size() > 1) {
                    Element rateTable = tables.get(1); // 第二个表格包含汇率数据
                    Elements rows = rateTable.select("tr");

                    // 查找美元行
                    for (Element row : rows) {
                        Elements cols = row.select("td");
                        if (cols.size() >= 8 && cols.get(0).text().contains("美元")) {
                            final String currency = cols.get(0).text();
                            final String buyingRate = cols.get(1).text();
                            final String sellingRate = cols.get(2).text();

                            // 更新UI
                            mainHandler.post(() -> {
                                resultTextView.setText(String.format(
                                        "货币: %s\n现汇买入价: %s\n现汇卖出价: %s",
                                        currency, buyingRate, sellingRate));
                            });
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    errorTextView.setText("获取汇率失败: " + e.getMessage());
                    errorTextView.setVisibility(View.VISIBLE);
                });
            } finally {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    fetchButton.setEnabled(true);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}