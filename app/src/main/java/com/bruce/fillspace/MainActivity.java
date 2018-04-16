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
import android.widget.RadioGroup;
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
    private final int STATUS_INTERNAL = 0;
    private final int STATUS_EXTERNAL = 1;

    private TextView tvFreeSpace;
    private TextView tvInfo;
    private EditText etFileSize;
    private Button btnRefresh;
    private Button btnExecute;
    private Button btnClean;
    private RadioGroup radioGroup;

    private File storage;
    private String storagePath;
    private File desDir;
    private int status = STATUS_INTERNAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initDirectory(status);
        showStorageFreeSpaceSize();
    }

    private void initView() {
        tvFreeSpace = findViewById(R.id.tv_free_space);
        tvInfo = findViewById(R.id.tv_info);
        etFileSize = findViewById(R.id.et_file_size);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnExecute = findViewById(R.id.btn_execute);
        btnClean = findViewById(R.id.btn_clean);
        radioGroup = findViewById(R.id.radio_gruop);

        btnRefresh.setOnClickListener(this);
        btnExecute.setOnClickListener(this);
        btnClean.setOnClickListener(this);
        tvInfo.setMovementMethod(ScrollingMovementMethod.getInstance());
        etFileSize.setSelection(etFileSize.getText().length());

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.internal:
                        status = STATUS_INTERNAL;
                        break;
                    case R.id.external:
                        status = STATUS_EXTERNAL;
                        break;
                    default:
                        break;
                }
                initDirectory(status);
                showStorageFreeSpaceSize();
            }
        });
    }

    private void initDirectory(int status) {

        switch (status) {
            case STATUS_INTERNAL:
                storage = getFilesDir();
                break;
            case STATUS_EXTERNAL:
                storage = Environment.getExternalStorageDirectory();
                break;
            default:
                storage = getFilesDir();
                break;
        }

        storagePath = storage.getAbsolutePath();
        desDir = new File(storagePath + DIR_FILL_STORAGE);

        if (!desDir.exists()) {
            if (desDir.mkdir()) {
                refreshInfoTextView("创建目录：" + desDir.getAbsolutePath());
            } else {
                refreshInfoTextView("创建失败：" + desDir.getAbsolutePath());
            }
        } else {
            refreshInfoTextView("路径: " + desDir.getAbsolutePath());
        }
    }

    private void showStorageFreeSpaceSize() {
        String freeSpaceSize = Formatter.formatFileSize(this, storage.getFreeSpace());
        tvFreeSpace.setText(freeSpaceSize);
    }

    @SuppressLint("StaticFieldLeak")
    private void executeWrite() {

        String fillSize = etFileSize.getText().toString();
        String fileNameWithTimeStamp = storagePath + DIR_FILL_STORAGE + "/" + System.currentTimeMillis();

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
                File[] files = desDir.listFiles();
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
