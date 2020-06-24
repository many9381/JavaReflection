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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import dalvik.system.PathClassLoader;

public class JavaReflection {

    private String CLASS_TAG = getClass().getSimpleName();

    public static Context getPackageContext(Context context, String packageName) {
        try {
            return context.getApplicationContext().createPackageContext(packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    //activityCall(context, act);

    public ArrayList<String[]> readExternal(String fileName) {

        File polyFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + fileName);
        if(!polyFile.exists()) {
            try {
                polyFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String[]> policyLine = new ArrayList<String[]>();

        try {
            FileReader stream = new FileReader(polyFile);
            BufferedReader bufferedReader = new BufferedReader(stream);

            while(true) {
                try {
                    String line;
                    if (((line = bufferedReader.readLine()) != null)) {
                        String[] temp2 = line.split(",");
                        policyLine.add(temp2);
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

        return policyLine;
    }

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

    public void loadRaonApi(String currentClassMethod, Activity act, Long start) {

        String filePath = act.getFilesDir().getPath();

        //activityCall(act, "MainActivity");

        try {

            ArrayList<String[]> policyLine = null;
            requestPermission(act);
            policyLine = readExternal("/Download/sample.txt");


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

                    Context pluginContext =
                            getPackageContext(act, "choi.security");
                    if (pluginContext == null) return;
                    ClassLoader classLoader = pluginContext.getClassLoader();

                    Class<?> pluginClass =
                            classLoader.loadClass(String.format("choi.security.%s", polyLibName));

                    String parentMethodName = currentClassMethod.split("-")[1];
                    logsSave(parentMethodName, filePath, act);

                    // method invoke
                    Object obj = pluginClass.getDeclaredConstructors()[0].newInstance();
                    Object[] params = null;
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

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }




        //Object obj = classname.newInstance();

    }

    /*
    public void activityCall(Activity act, final String actName) {

        final String clsName =  "choi.security";
        ComponentName component =
                new ComponentName(clsName, String.format("%s.%s", clsName, actName));
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component);
        act.startActivity(intent);



            Class className = Class.forName("android.content.Intent");
            ComponentName component = new ComponentName("com.example.security","com.example.security.MainActivity");
            Object intent = className.newInstance();

            Method methodSetAction = intent.getClass().getMethod("setAction", String.class);
            methodSetAction.invoke(intent, "android.intent.action.MAIN");
            Method methodAddCategory = intent.getClass().getMethod("addCategory", String.class);
            methodAddCategory.invoke(intent, "android.intent.CATEGORY_LAUNCHER");
            Method methodSetComponent = intent.getClass().getMethod("setComponent", ComponentName.class);
            methodSetComponent.invoke(intent, component);

            // TODO: 이렇게 강제 캐스팅 가능한지 확인 필요
            context.startActivity((android.content.Intent) intent);




    }

    private void activityCall2(Context context, Activity act, final String actName) {

        final String packagePath =  "choi.security";
        final String classPath =  String.format("%s.%s", packagePath, actName);

        try {
            String apkName = context.getPackageManager().getApplicationInfo(
                    packagePath, 0).sourceDir;
            PathClassLoader pathClassLoader = new PathClassLoader(apkName,
                    ClassLoader.getSystemClassLoader());
            Class<Activity> handler = (Class<Activity>) Class.forName(
                    classPath, true, pathClassLoader);
            Method[] mm = handler.getDeclaredMethods();

            for (Method m : mm) {
                Log.d("Method", m.getName());
            }

            Method method = mm[0];

            method.setAccessible(true);
            //Object tttt = method.invoke(handler.newInstance(), new android.os.Bundle());
            Object tttt = method.invoke(handler.newInstance());

            Log.d("test" ,"test") ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     */


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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                timeDateText = ") " + LocalDateTime.now().format(dateFormat);
            }
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
