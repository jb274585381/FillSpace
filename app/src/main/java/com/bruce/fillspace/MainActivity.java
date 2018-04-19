package com.bruce.fillspace;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * @author bo.jiang
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String DEV_ZERO = "/dev/zero";
    private final String DIR_FILL_STORAGE = "/FillStorage";
    private final int SIZE_100KB = 1024 * 100;
    private final int SIZE_1MB = 1024 * 1024;
    private final int SIZE_10MB = 1024 * 1024 * 10;
    private final int SIZE_100MB = 1024 * 1024 * 100;
    private final int SIZE_1GB = 1024 * 1024 * 1024;

    private final int SIZE_1 = 1;
    private final int SIZE_10 = 10;
    private final int SIZE_100 = 100;
    private final int SIZE_1024 = 1024;

    private final String KB_100_ = "KB_100_";
    private final String MB_001_ = "MB_001_";
    private final String MB_010_ = "MB_010_";
    private final String MB_100_ = "MB_100_";
    private final String GB_001_ = "GB_001_";

    private TextView tvFreeSpace;
    private TextView tvInfo;
    private EditText etResidualSize;
    private Button btnRefresh;
    private Button btnExecute;
    private Button btnClean;

    private File storage;
    private String storagePath;
    private File desDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initDirectory();
        showStorageFreeSpaceSize();
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

    private void initView() {
        tvFreeSpace = findViewById(R.id.tv_free_space);
        tvInfo = findViewById(R.id.tv_info);
        etResidualSize = findViewById(R.id.et_residual_size);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnExecute = findViewById(R.id.btn_execute);
        btnClean = findViewById(R.id.btn_clean);

        btnRefresh.setOnClickListener(this);
        btnExecute.setOnClickListener(this);
        btnClean.setOnClickListener(this);
        tvInfo.setMovementMethod(ScrollingMovementMethod.getInstance());
        etResidualSize.setSelection(etResidualSize.getText().length());
    }

    private void initDirectory() {
        storage = Environment.getExternalStorageDirectory();
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

        final String residualSize = etResidualSize.getText().toString();


        new AsyncTask<String, Integer, String>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                refreshInfoTextView("开始写入...");
            }

            @Override
            protected String doInBackground(String... strings) {
                try {
                    long residualSize = (long) (Float.valueOf(strings[0]) * SIZE_1MB);
                    handleWrite(residualSize);
                } catch (Exception e) {
                    return e.toString();
                }
                return " 写入完毕!";
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
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, residualSize);
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

    private void handleWrite(long residualSize) throws IOException {
        long writeSize = storage.getFreeSpace() - residualSize;

        if (writeSize >= 0) {
            handlePositive(residualSize);
        } else {
            handleNegative(residualSize);
        }
    }

    private void handlePositive(long residualSize) throws IOException {
        File input = new File(DEV_ZERO);
        FileInputStream fileInputStream = new FileInputStream(input);
        FileChannel inputChannel = fileInputStream.getChannel();

        long writeSize = storage.getFreeSpace() - residualSize;

        while (writeSize > SIZE_100KB) {
            if (writeSize <= SIZE_1MB) {
                File output = new File(storagePath + DIR_FILL_STORAGE + "/" + KB_100_ + System.currentTimeMillis());
                writeKB(inputChannel, output);
            } else if (writeSize <= SIZE_10MB) {
                File output = new File(storagePath + DIR_FILL_STORAGE + "/" + MB_001_ + System.currentTimeMillis());
                writeMB(inputChannel, output, SIZE_1);
            } else if (writeSize <= SIZE_100MB) {
                File output = new File(storagePath + DIR_FILL_STORAGE + "/" + MB_010_ + System.currentTimeMillis());
                writeMB(inputChannel, output, SIZE_10);
            } else if (writeSize <= SIZE_1GB) {
                File output = new File(storagePath + DIR_FILL_STORAGE + "/" + MB_100_ + System.currentTimeMillis());
                writeMB(inputChannel, output, SIZE_100);
            } else {
                File output = new File(storagePath + DIR_FILL_STORAGE + "/" + GB_001_ + System.currentTimeMillis());
                writeMB(inputChannel, output, SIZE_1024);
            }
            writeSize = storage.getFreeSpace() - residualSize;
        }
        inputChannel.close();
        fileInputStream.close();
    }

    private void handleNegative(long residualSize) throws IOException {
        ArrayList<String> kb100List = new ArrayList<>();
        ArrayList<String> mb001List = new ArrayList<>();
        ArrayList<String> mb010List = new ArrayList<>();
        ArrayList<String> mb100List = new ArrayList<>();
        ArrayList<String> gb001List = new ArrayList<>();

        File[] files = desDir.listFiles();
        for (File file : files) {
            String filePath;
            if (file.exists() && file.isFile()) {
                filePath = file.getAbsolutePath();
                if (filePath.contains(KB_100_)) {
                    kb100List.add(filePath);
                } else if (filePath.contains(MB_001_)) {
                    mb001List.add(filePath);
                } else if (filePath.contains(MB_010_)) {
                    mb010List.add(filePath);
                } else if (filePath.contains(MB_100_)) {
                    mb100List.add(filePath);
                } else if (filePath.contains(GB_001_)) {
                    gb001List.add(filePath);
                }
            }
        }
        long writeSize = (storage.getFreeSpace() - residualSize) * (-1);

        while (writeSize > SIZE_100KB) {
            if (writeSize <= SIZE_1MB) {
                if (!kb100List.isEmpty()) {
                    File file = new File(kb100List.get(0));
                    if (file.delete()) {
                        kb100List.remove(0);
                    }
                } else if (!mb001List.isEmpty()) {
                    File file = new File(mb001List.get(0));
                    if (file.delete()) {
                        mb001List.remove(0);
                    }
                } else if (!mb010List.isEmpty()) {
                    File file = new File(mb010List.get(0));
                    if (file.delete()) {
                        mb010List.remove(0);
                    }
                } else if (!mb100List.isEmpty()) {
                    File file = new File(mb100List.get(0));
                    if (file.delete()) {
                        mb100List.remove(0);
                    }
                } else if (!gb001List.isEmpty()) {
                    File file = new File(gb001List.get(0));
                    if (file.delete()) {
                        gb001List.remove(0);
                    }
                }
            } else if (writeSize <= SIZE_10MB) {
                if (!mb001List.isEmpty()) {
                    File file = new File(mb001List.get(0));
                    if (file.delete()) {
                        mb001List.remove(0);
                    }
                } else if (!mb010List.isEmpty()) {
                    File file = new File(mb010List.get(0));
                    if (file.delete()) {
                        mb010List.remove(0);
                    }
                } else if (!mb100List.isEmpty()) {
                    File file = new File(mb100List.get(0));
                    if (file.delete()) {
                        mb100List.remove(0);
                    }
                } else if (!gb001List.isEmpty()) {
                    File file = new File(gb001List.get(0));
                    if (file.delete()) {
                        gb001List.remove(0);
                    }
                }
            } else if (writeSize <= SIZE_100MB) {
                if (!mb010List.isEmpty()) {
                    File file = new File(mb010List.get(0));
                    if (file.delete()) {
                        mb010List.remove(0);
                    }
                } else if (!mb100List.isEmpty()) {
                    File file = new File(mb100List.get(0));
                    if (file.delete()) {
                        mb100List.remove(0);
                    }
                } else if (!gb001List.isEmpty()) {
                    File file = new File(gb001List.get(0));
                    if (file.delete()) {
                        gb001List.remove(0);
                    }
                }
            } else if (writeSize <= SIZE_1GB) {
                if (!mb100List.isEmpty()) {
                    File file = new File(mb100List.get(0));
                    if (file.delete()) {
                        mb100List.remove(0);
                    }
                } else if (!gb001List.isEmpty()) {
                    File file = new File(gb001List.get(0));
                    if (file.delete()) {
                        gb001List.remove(0);
                    }
                }
            } else {
                if (!gb001List.isEmpty()) {
                    File file = new File(gb001List.get(0));
                    if (file.delete()) {
                        gb001List.remove(0);
                    }
                }
            }
            writeSize = (storage.getFreeSpace() - residualSize) * (-1);
        }
        handlePositive(residualSize);
    }

    private void writeMB(FileChannel inputChannel, File output, int count) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(output);
        FileChannel outputChannel = fileOutputStream.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(SIZE_1MB);
        for (int i = 0; i < count; i++) {
            inputChannel.read(buffer);
            buffer.flip();
            outputChannel.write(buffer);
            buffer.clear();
        }
        outputChannel.close();
        fileOutputStream.close();
    }

    private void writeKB(FileChannel inputChannel, File output) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(output);
        FileChannel outputChannel = fileOutputStream.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(SIZE_100KB);
        inputChannel.read(buffer);
        buffer.flip();
        outputChannel.write(buffer);
        buffer.clear();
        outputChannel.close();
        fileOutputStream.close();
    }

}
