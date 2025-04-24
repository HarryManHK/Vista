package com.example.vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vista.DatabaseHelper.SettingDatabaseHelper;
import com.example.vista.TextToSpeech.CustomTextToSpeech;
import com.example.vista.SettingPage;

public class VoiceGenderSetting extends AppCompatActivity {
    private ListView lvVoiceGenderSetting;
    private Button btnVoiceGenderConfirm, btnVoiceGenderNext;
    private CustomTextToSpeech tts;
    private SettingDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voice_gender_setting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // define the gender of voice
        final String[] genders = {"male", "female"};
        final String[] EnglishGenderLabels = {"Male voice", "Female voice"};
        final String[] ChineseGenderLabels = {"男性配音", "女性配音"};

        // initial database 
        dbHelper = SettingDatabaseHelper.getInstance(getApplicationContext());
        tts = new CustomTextToSpeech(this);
 
        // find the ui components
        lvVoiceGenderSetting = findViewById(R.id.lvVoiceGenderSetting);
        btnVoiceGenderConfirm = findViewById(R.id.btnVoiceGenderConfirm);
        btnVoiceGenderNext = findViewById(R.id.btnVoiceGenderNext);

        // setting ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item_white_text, genders);
        lvVoiceGenderSetting.setAdapter(adapter);
        lvVoiceGenderSetting.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // user update the database and speak the selected gender
        lvVoiceGenderSetting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedGender = genders[position];
                dbHelper.setVoiceGender(selectedGender);
                tts.speak(new String[]{
                    EnglishGenderLabels[position],
                    ChineseGenderLabels[position]
                });
            }
        });

        // Confirm 
        btnVoiceGenderConfirm.setOnClickListener(v -> {
            int pos = lvVoiceGenderSetting.getCheckedItemPosition();
            if (pos != AdapterView.INVALID_POSITION) {
                tts.speak(new String[]{
                    "You selected" + EnglishGenderLabels[pos],
                    "你已選擇" + ChineseGenderLabels[pos]
                });
                startActivity(new Intent(VoiceGenderSetting.this, SettingPage.class));
                finish();
            } else {
                Toast.makeText(this, "Please select a gender.", Toast.LENGTH_SHORT).show();
            }
        });

        // Next 按鈕：目前無動作，可視需求新增
    }
}