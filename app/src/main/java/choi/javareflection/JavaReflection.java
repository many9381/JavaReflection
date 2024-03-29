package choi.javareflection;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TimeUtils;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import dalvik.system.PathClassLoader;

public class JavaReflection {

    private String CLASS_TAG = getClass().getSimpleName();
    private static JavaReflection javaReflection = null;
    private static ArrayList<String[]> policyLine = null;
    private static String filePath = null;
    //private static SecuritySwitch securitySwitch = null;
    private static HashMap<String, Boolean> securitySwitch = null;



    private JavaReflection(Activity act) {
        requestPermission(act);
    }


    private static HashMap<String, Boolean> initHashMap() {

        HashMap<String, Boolean> securitySwitch = new HashMap<>();

        securitySwitch.put("captureLock", false);

        return securitySwitch;
    }

    /*
        불필요하게 자주 반복되는 작업을 줄이기 위한
        Singletone 클래스
     */
   public static JavaReflection getInstance(Activity act) {

       if(securitySwitch == null) {
           //securitySwitch = new SecuritySwitch();
           securitySwitch = initHashMap();
       }

        if(javaReflection == null){
            javaReflection = new JavaReflection(act);
            policyLine = readExternal("/Download/sample.txt");
            filePath = act.getFilesDir().getPath();
        }
        return javaReflection;
    }




    public static Context getPackageContext(Context context, String packageName) {
        try {
            return context.getApplicationContext().createPackageContext(packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    //activityCall(context, act);


    /*
        외부 저장소에 있는 정책파일을 읽는 함수
        줄 단위로 읽어서 ArrayList<String> 형태로 리
     */
    public static ArrayList<String[]> readExternal(String fileName) {

        File polyFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + fileName);
        if(!polyFile.exists()) {
            try {
                polyFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String[]> readPolicyLine = new ArrayList<String[]>();
        try {
            FileReader stream = new FileReader(polyFile);
            BufferedReader bufferedReader = new BufferedReader(stream);

            while(true) {
                try {
                    String line;
                    if (((line = bufferedReader.readLine()) != null)) {
                        String[] temp2 = line.split(",");
                        readPolicyLine.add(temp2);
                    }
                    else {
                        bufferedReader.close();
                        break;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return readPolicyLine;
    }

    /*
        외부 저장소에 접근하려면 권한이 필요하므로
        필요한 권한 요청을 하기위한 함수
     */
    private boolean requestPermission(Activity act){

        if (act.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                act.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {


            act.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    1004);

        }

        return true;
    }

    /*
        보안 앱의 Context를 얻는 함수
     */
    private Class<?> getPluginClass(Activity act, String polyLibName) throws ClassNotFoundException {
        Context pluginContext =
                getPackageContext(act, "choi.security");
        if (pluginContext == null) {
            Toast.makeText(act, "Security App이 없습니다.", Toast.LENGTH_SHORT).show();
            return null;
        }


        return pluginContext.getClassLoader().
                loadClass(String.format("choi.security.%s", polyLibName));

    }


    /*
        보안 앱의 기능을 호출하는 함수
     */
    public void loadRaonApi(String currentClassMethod, Activity act, Long start) {

        //activityCall(act, "MainActivity");

        if(securitySwitch == null) {
            //securitySwitch = new SecuritySwitch();
            securitySwitch = initHashMap();
            Log.e(CLASS_TAG, "CHECK securitySWitch");
        }

        try {

            for (String[] str: policyLine) {
                String polyClsName = str[0];
                String polyMethodName = str[1];
                String polyLibName = str[2];
                String polyFuncName = str[3];

                Log.e(CLASS_TAG, String.format("readline: %s-%s", polyClsName, currentClassMethod));

                // MainActivity-method pirivate onCreate() 와 비교
                String policyClsMethod = String.format("%s-%s", polyClsName, polyMethodName);
                if (policyClsMethod.equals(currentClassMethod)) {
                    Log.e(CLASS_TAG, String.format("matching: %s : %s",
                            polyClsName, currentClassMethod));


                    Class<?> pluginClass = getPluginClass(act, polyLibName);

                    String parentMethodName = currentClassMethod.split("-")[1];
                    logsSave(parentMethodName, filePath, act);

                    // method invoke
                    Object obj = pluginClass.getDeclaredConstructors()[0].newInstance();
                    Object[] params = null;
                    params = new Object[1];
                    params[0] = (Object) securitySwitch;

                    Method[] methods = pluginClass.getDeclaredMethods();

                    Method method = null;
                    for (Method it : methods) {
                        if(it.getName().equals(polyFuncName)) {
                            method = it;
                            break;
                        }
                    }

                    method.setAccessible(true);
                    method.invoke(obj, act, params);

                    Long end = System.nanoTime();
                    Log.e("reflection", String.format("Process Time(run): %d", end - start));
                }
                Long end = System.nanoTime();
                Log.e("reflection", String.format("Process Time(not): %d", end - start));
            }

        } catch (ClassNotFoundException | IllegalAccessException |
                InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }

        //Object obj = classname.newInstance();

    }


    /*
        log를 파일로 저장하는 함수
        security 함수에서 호출된 log 들을 저장.
     */
    private void logsSave(String parentMethodName, String path, Context act) {
        try {
            String filePath = path + "/log.txt";
            File file = new File(filePath);
            if(!file.exists()) {
                file.createNewFile();
            }

            /*
             임시용 로그
             TODO: (파라미터 알아내는 방법 찾아야함)
             */

            String str = act.getClass().getName();

            Method actMethod = act.getClass().getDeclaredMethod(parentMethodName, Bundle.class);
            Class<?>[] methodPara = actMethod.getParameterTypes();

            StringBuilder logString = new StringBuilder();
            //String logPreText = act.getLocalClassName() + " " + actMethod.getName() + "(";
            String logPreText = "asdf" + " " + actMethod.getName() + "(";
            logString.append(logPreText);
            for (int i = 0; i < methodPara.length; i++) {
                logString.append(String.format("%s: ", methodPara[i].getName().replace('.', '/')));
            }
            String timeDateText = "None";


            timeDateText = ")" + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
            /*
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                timeDateText = ") " + LocalDateTime.now().format(dateFormat);
            }

             */
            logString.append(timeDateText);

            FileWriter fw = new FileWriter(filePath, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(logString.toString());
            bw.newLine();
            bw.close();
        }
        catch (Exception e) {
            Log.e(CLASS_TAG, "error:  ${e.cause} , ${e.message}");
        }
    }


}
