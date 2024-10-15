package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//    10/1
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {
    // 변수
    private static final int REQUEST_CODE = 1; // 요청 코드 정의

    private ActivityResultLauncher<String> audioPickerLauncher;
    private TextView textViewSelectedAudio;
    private Button buttonAnalyzeAudio;

    private Interpreter tfliteInterpreter;

    private Uri selectedAudioUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // activity_main.xml 레이아웃 사용

        // 권한 요청
        checkPermissions();

        // activity_main.xml에서 정의한 뷰와 버튼 참조
        TextView textViewTop = findViewById(R.id.textViewTop);
        Button buttonUploadAudio = findViewById(R.id.buttonUploadAudio);
        textViewSelectedAudio = findViewById(R.id.textViewSelectedAudio);
        buttonAnalyzeAudio = findViewById(R.id.buttonAnalyzeAudio);
        TextView textViewBottom = findViewById(R.id.textViewBottom);

        // ActivityResultLauncher 초기화
        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        // 선택된 음성 파일 URI 처리
                        handleSelectedAudio(uri);
                    } else {
                        // 선택 취소 시 처리
                        Toast.makeText(MainActivity.this, "음성 파일 선택 취소됨", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 버튼 클릭 이벤트 리스너 설정
        buttonUploadAudio.setOnClickListener(v -> {
            // 버튼이 클릭되면 실행되는 기능
            Toast.makeText(MainActivity.this, "음성 업로드 버튼 클릭됨", Toast.LENGTH_SHORT).show();
            // 음성 파일 선택을 위한 인텐트 시작
            audioPickerLauncher.launch("audio/*");
        });

        // "음성 분석" 버튼 클릭 이벤트 리스너 설정
        buttonAnalyzeAudio.setOnClickListener(v -> {
            if (selectedAudioUri != null) {
                analyzeAudio(selectedAudioUri);
            } else {
                Toast.makeText(MainActivity.this, "분석할 음성 파일이 선택되지 않았습니다.", Toast.LENGTH_SHORT).show();
            }
        });



        ///// TFLite 모델 초기화
        try {
            tfliteInterpreter = new Interpreter(loadModelFile("model.tflite"));
            Toast.makeText(this, "모델이 성공적으로 로드되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "모델 로드에 실패했습니다."+ e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    // 권한을 확인하고 요청하는 메서드
    private void checkPermissions() {
        Log.d("Permissions", "checkPermissions() 호출됨");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "READ_EXTERNAL_STORAGE 권한 없음, 요청합니다.");
                // 권한 요청 전에 사용자에게 설명
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "음성 파일을 선택하려면 외부 저장소에 대한 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                }
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE); // REQUEST_CODE 사용
            } else {
                Log.d("Permissions", "READ_EXTERNAL_STORAGE 권한이 이미 허용됨");
            }
        }
    }



    // 사용자가 권한 요청 결과를 수신하는 메서드
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("Permissions", "onRequestPermissionsResult called with requestCode: " + requestCode);

        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "READ_EXTERNAL_STORAGE permission granted.");
                // 권한이 부여된 경우 수행할 작업
            } else {
                Log.d("Permissions", "READ_EXTERNAL_STORAGE permission denied.");
                // 권한이 거부된 경우 수행할 작업
            }
        }
    }




     ///// 선택된 음성 파일 URI를 처리하는 메서드 /////
    private void handleSelectedAudio(Uri uri) {
        // 선택된 음성 파일의 URI를 텍스트뷰에 표시
        textViewSelectedAudio.setText("선택된 음성 파일 URI: " + uri.toString());
        // "음성 분석" 버튼 활성화(초기에 비활성됨)
        buttonAnalyzeAudio.setEnabled(true);

        // 선택된 URI를 변수에 저장
        selectedAudioUri = uri; //중요!!


        // 추가로 음성 파일을 재생하거나 업로드하는 기능을 구현할 수 있음
        Toast.makeText(this, "음성 파일이 선택되었습니다.", Toast.LENGTH_SHORT).show();

    }




    ///// TFLite 모델 파일을 로드하는 메서드 //////
    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    ///// 선택된 음성 파일을 TFLite 모델로 분석하는 메서드 /////
    private void analyzeAudio(Uri uri) {
        Toast.makeText(this, "음성 분석을 시작합니다...", Toast.LENGTH_SHORT).show();

        // 실제 분석 로직 구현(미완)
        new Thread(() -> {
            try {
                // 음성 파일을 바이트 배열로 읽기
                byte[] audioData = Utils.getBytesFromUri(this, uri);

                // 모델 입력 준비
                // 모델의 입력 형태에 맞게 데이터 변환 필요
                // float 배열 등
                float[][] input = preprocessAudio(audioData);

                // 모델 출력 준비
                float[][] output = new float[1][/* 출력 차원 ??? */];

                // 모델 실행
                tfliteInterpreter.run(input, output);

                // 결과 처리
                runOnUiThread(() -> {
                    // 출력 결과를 UI에 표시
                    // 예: textViewSelectedAudio.setText("분석 결과: " + Arrays.toString(output[0]));
                    Toast.makeText(MainActivity.this, "분석이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "분석 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


     ///// 음성 데이터를 모델 입력 형식으로 전처리하는 메서드/////
    /////@return 모델 입력 형식 (예: float[][]) /////
    private float[][] preprocessAudio(byte[] audioData) {
        // 실제 모델에 맞게 음성 데이터를 전처리해야 함.
        // e.g. MFCC 추출, 정규화 등

        // 여기서는 예시로 더미 데이터를 반환
        return new float[][]{{0.0f}};
        /////실제 전처리 구현해야 함!!!!!
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tfliteInterpreter != null) {
            tfliteInterpreter.close();
        }
    }


}

