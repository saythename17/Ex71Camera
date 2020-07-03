package com.icandothisallday2020.ex71camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    ImageView iv;
    Uri imgUri;//캡쳐한 이미지 경로 Uri

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv=findViewById(R.id.iv);
        //외부저장소 사용에 대한 동적퍼미션
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
            int permissionOnResult=checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if(permissionOnResult== PackageManager.PERMISSION_DENIED){
                //퍼미션 체크 다이얼로그
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},100);
            }
        }
    }//onCreate method

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode){
            case 100:
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "사진저장가능", Toast.LENGTH_SHORT).show();
                else Toast.makeText(this, "촬영된 사진저장 불가", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void clickFAB(View view) {
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //카메라 앱에게 캡쳐한 이미지를 저장하게 하려면
        //저장할 이미지의 파일경로 Uri 를 미리 지정해야 함
        setImgUri();

        if(imgUri!=null) intent.putExtra(MediaStore.EXTRA_OUTPUT,imgUri);
        startActivityForResult(intent,30);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch (requestCode){
            case 30:
                if (resultCode==RESULT_OK) {
                    //사진정보를 가지고 돌아온 Intent 객체에게 정보요청
                    //저장(putExtra)작업시 디바이스에 따라 Intent 가 돌아오지 않는 경우도 있기때문에
                    if(data!=null){
                        Uri uri=data.getData();
                        if(uri!=null){//Uri 로 온경우
                            Glide.with(this).load(uri).into(iv);
                            Toast.makeText(this, "Uri Image", Toast.LENGTH_SHORT).show();
                        }
                        else{//Bitmap 으로 온 경우 - 해상도 깨지기 때문에 
                            if(imgUri!=null) Glide.with(this).load(imgUri).into(iv);
                            Toast.makeText(this, "Bitmap Image", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        //돌아온 인텐트객체가 없는 경우가 있으므로 
                        // 카메라앱에게 저장하도록 요청했던 사진경로 Uri 를 이용하여 이미지 보여주기
                        if(imgUri!=null) Glide.with(this).load(imgUri).into(iv);
                        Toast.makeText(this, "인텐트가 돌아오지 않음", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    //캡쳐된 이미지 Uri 를 정하는 메소드
    public void setImgUri() {
        //외부저장소(SD card)에 저장하는 것을 권장
        //****외부저장소의 두가지 영역
        //****1.외부저장소 내 앱에 할당된 영역-퍼미션 없어도 됨:앱을 지우면 저장된 사진도 같이 지워짐
        //File path=getExternalFilesDir("photo");
        //****2.외부저장소 공용영역-동적 퍼미션 필요 : 앱을 지워도 사진은 남아있음
        File path = Environment.getExternalStorageDirectory();//외부메모리 최상위(root) 경로
        //경로: [storage/emulated]
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //경로:[storage/emulated/0/pictures]
        //this.imgUri = imgUri;
        //경로에 저장된 파일명- 같은 이름으로 저장하면 덮어쓰기 되므로 저장할때마다 다른이름
        //통상적으로 일시를 이용해 파일명 정함 ex) "IMG_202006111004231.jpg"
        //1)날씨를 이용
        SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMddhhmmss");
        String fileName="IMG_"+sdf.format(new Date())+".jpg";
        //파일명과 경로 합성->File 객체 생성
        File file=new File(path,fileName);

        //2)자동으로 임시파일명을 만드는 메소드 이용
//        try {
//            file=File.createTempFile("IMG_",".jpg",path);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }--랜덤한이름으로 저장되기에 순서대로 저장되지 않음

        //카메라앱에 전달해줄 저장파일경로 = File 객체가 아니라 Uri 객체여야 함
        //File--->Uri 변환
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N) imgUri= Uri.fromFile(file);
        else{//누가(ver.N|24~) 변환이 어려워짐
            //다른 앱에게 파일 접근을 허용하는 Content Provider 이용
            //Provider 중 안드로이드에 이미 만들어져 있는 FileProvider 호출
            //1.Manifest.xml 에 FileProvider 클래스 등록
            //2.FileProvider 가 공개할 파일의 경로를
            // res/xml 폴더안 "paths.xml"이라는 이름으로 만들어 <path>태그로 경로 지정
            //3. 자바에서 <provider> tag 에 작성한 속성중 authorities(권한,식별자)=""에 작성한 값을 사용

            imgUri= FileProvider.getUriForFile(this,"com.icandothisallday2020.ex71camera.provider",file);
        }//imgUri 작업 끝
        new AlertDialog.Builder(this).setMessage(imgUri.toString()).create().show();//작업확인



    }
}
