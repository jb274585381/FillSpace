package com.bruce.fillspace;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author bo.jiang
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String DEV_ZERO = "/dev/zero";
    private final String DIR_FILL_STORAGE = "/FillStorage";
    private final int bufferSize = 1024 * 1024;

    private TextView tvFreeSpace;
    private TextView tvInfo;
    private EditText etFileSize;
    private Button btnRefresh;
    private Button btnExecute;
    private Button btnClean;

    private File sdCard;
    private String sdcardPath;
    private File destinationDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initDirectory();
        showStorageFreeSpaceSize();
    }

    private void initView() {
        tvFreeSpace = findViewById(R.id.tv_free_space);
        tvInfo = findViewById(R.id.tv_info);
        etFileSize = findViewById(R.id.et_file_size);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnExecute = findViewById(R.id.btn_execute);
        btnClean = findViewById(R.id.btn_clean);

        btnRefresh.setOnClickListener(this);
        btnExecute.setOnClickListener(this);
        btnClean.setOnClickListener(this);
        tvInfo.setMovementMethod(ScrollingMovementMethod.getInstance());
        etFileSize.setSelection(etFileSize.getText().length());
    }

    private void initDirectory() {
        sdCard = Environment.getExternalStorageDirectory();
        sdcardPath = sdCard.getAbsolutePath();

        destinationDir = new File(sdcardPath + DIR_FILL_STORAGE);
        if (!destinationDir.exists()) {
            if (destinationDir.mkdir()) {
                refreshInfoTextView("mkdir: " + destinationDir.getAbsolutePath());
            } else {
                refreshInfoTextView("error: 创建文件夹失败!");
            }
        } else {
            refreshInfoTextView("文件夹路径: " + destinationDir.getAbsolutePath());
        }
    }

    private void showStorageFreeSpaceSize() {
        String freeSpaceSize = Formatter.formatFileSize(this, sdCard.getFreeSpace());
        tvFreeSpace.setText(freeSpaceSize);
    }

    @SuppressLint("StaticFieldLeak")
    private void executeWrite() {

        String fillSize = etFileSize.getText().toString();
        String fileNameWithTimeStamp = sdcardPath + DIR_FILL_STORAGE + "/" + System.currentTimeMillis();

        new AsyncTask<String, Integer, String>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                refreshInfoTextView("开始写入...");
            }

            @Override
            protected String doInBackground(String... strings) {

                int fillSize = Integer.valueOf(strings[0]);
                String fileName = strings[1];

                File from = new File(DEV_ZERO);
                File to = new File(fileName);

                try {
                    FileInputStream fileInputStream = new FileInputStream(from);
                    FileOutputStream fileOutputStream = new FileOutputStream(to);
                    FileChannel fromChannel = fileInputStream.getChannel();
                    FileChannel toChannel = fileOutputStream.getChannel();

                    ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                    for (int i = 1; i <= fillSize; i++) {
                        fromChannel.read(buffer);
                        buffer.flip();
                        toChannel.write(buffer);
                        buffer.clear();
                        if (i % 100 == 0) {
                            publishProgress(i * 100 / fillSize);
                        }
                    }
                    fileInputStream.close();
                    fileOutputStream.close();
                    fromChannel.close();
                    toChannel.close();
                } catch (Exception e) {
                    return e.getMessage();
                }

                return to.getName() + " 写入完毕!";
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                refreshInfoTextView("已写入..." + values[0] + "%");
                showStorageFreeSpaceSize();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                refreshInfoTextView(s);
                showStorageFreeSpaceSize();

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fillSize, fileNameWithTimeStamp);
    }

    @SuppressLint("StaticFieldLeak")
    private void cleanSpace() {
        new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                refreshInfoTextView("开始清理...");
            }

            @Override
            protected String doInBackground(String... strings) {
                File[] files = destinationDir.listFiles();
                for (File file : files) {
                    if (file.exists() && file.isFile()) {
                        String fileName = file.getName();
                        if (file.delete()) {
                            publishProgress(fileName + " 删除成功！");
                        } else {
                            publishProgress(fileName + " 删除失败！");
                        }
                    }
                }
                return "清理完毕!";
            }

            @Override
            protected void onProgressUpdate(String... values) {
                super.onProgressUpdate(values);
                refreshInfoTextView(values[0]);
                showStorageFreeSpaceSize();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                refreshInfoTextView(s);
                showStorageFreeSpaceSize();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void refreshInfoTextView(String info) {
        tvInfo.append(info + "\n");
        int offset = tvInfo.getLineCount() * tvInfo.getLineHeight();
        if (offset > tvInfo.getHeight()) {
            tvInfo.scrollTo(0, offset - tvInfo.getHeight());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_refresh:
                showStorageFreeSpaceSize();
                break;
            case R.id.btn_execute:
                executeWrite();
                break;
            case R.id.btn_clean:
                cleanSpace();
                break;
            default:
                break;
        }
    }

}
