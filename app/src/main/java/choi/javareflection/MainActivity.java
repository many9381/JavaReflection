package choi.javareflection;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Long now = System.nanoTime();
        String classname = getClass().getSimpleName();
        String method = new Exception().getStackTrace()[0].getMethodName();

        String inputCurrentMethod = classname + '-' + method; //"ActivityName-MethodName"


        JavaReflection reflection = new JavaReflection();

        reflection.loadRaonApi(inputCurrentMethod, getApplication(),
                this, now);


        //reflection.activityCall(getApplication(), this);

    }
}