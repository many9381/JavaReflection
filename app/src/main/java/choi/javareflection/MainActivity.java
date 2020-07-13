package choi.javareflection;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //String inputCurrentMethod = getClass().getSimpleName() + '-' + new Exception().getStackTrace()[0].getMethodName(); //"ActivityName-MethodName"
        String inputCurrentMethod = "MainActivity-onCreate";
        JavaReflection.getInstance(this).loadRaonApi(inputCurrentMethod, this,  System.nanoTime());;

        //getApplicationContext().getClass();
        //Context test = MainActivity.this;

        //reflection.activityCall(this, "MainActivity");
    }
}