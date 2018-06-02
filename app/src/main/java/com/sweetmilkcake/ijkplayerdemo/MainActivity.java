package com.sweetmilkcake.ijkplayerdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sweetmilkcake.ijkplayerdemo.player.PlayActivity;

public class MainActivity extends AppCompatActivity {

    private Button mPlayButton;
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditText = (EditText) findViewById(R.id.et_video_url);

        mPlayButton = (Button) findViewById(R.id.bt_play);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = mEditText.getText().toString();
                if (url != null && !url.isEmpty()) {

                    Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                    intent.putExtra("url", url);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Input Url Error", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

}
