package choi.javareflection;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;


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

    public void loadRaonApi(String currentClassMethod, Context context, Activity act, Long start) {

        String filePath = context.getFilesDir().getPath();

        //activityCall(context, act);
        try {

            ArrayList<String[]> policyLine = new ArrayList<String[]>();

            FileInputStream fin = context.openFileInput("sample.txt");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fin));

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


            for (String[] str: policyLine) {
                String polyClsName = str[0];
                String polyMethodName = str[1];
                String polyLibName = str[2];
                String polyFuncName = str[3];

                Log.e(CLASS_TAG, String.format("readline: %s-%s", polyClsName, currentClassMethod));

                // MainActivity-method pirivate onCreate() 와 비교
                String policyClsMethod = String.format("%s-%s", polyClsName, polyMethodName);
                if (policyClsMethod.equals(currentClassMethod)) {
                    Log.e(CLASS_TAG, String.format("matching: %s : %s", polyClsName, currentClassMethod));

                    /*
                    String CLASS_NAME = String.format("etri.security.%s", polyLibName);
                    Class<?> cls = Class.forName(CLASS_NAME);
                    Log.e(CLASS_TAG, "CLASS_NAME: " + cls.getName());

                     */

                    Context pluginContext = getPackageContext(context, "choi.security");
                    if (pluginContext == null) return;
                    ClassLoader classLoader = pluginContext.getClassLoader();

                    Class<?> pluginClass = classLoader.loadClass(String.format("choi.security.%s", polyLibName));

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


                    //params = new Object[]{myLayout};


                    method.setAccessible(true);
                    method.invoke(obj, pluginContext, act, params);

                    /*
                    Method method = pluginClass.getMethod(polyFuncName);
                    Log.e(CLASS_TAG, "test");
                    val params: ArrayList<Any> =  arrayListOf()
                    // parameter 가 있는 경우 ?
                    if (value.size > 4) {
                        for (i in 4..value.size) {
                            params.add(value[i])
                        }
                    }

                     */

                    Long end = System.nanoTime();
                    Log.e("reflection", String.format("Process Time(run): %d", end - start));
                }
                Long end = System.nanoTime();
                Log.e("reflection", String.format("Process Time(not): %d", end - start));
            }

        } catch (ClassNotFoundException | FileNotFoundException e) {
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

    public void activityCall(Context context, Activity act) {

        ComponentName component = new ComponentName("choi.security","choi.security.MainActivity");
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component);
        context.startActivity(intent);

            /*
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

             */
    }

    /*
        log를 파일로 저장하는 함수
        security 함수에서 호출된 log 들을 저장.
     */
    private void logsSave(String parentMethodName, String path, Activity act) {
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
            Method actMethod = act.getClass().getDeclaredMethod(parentMethodName, Bundle.class);
            Class<?>[] methodPara = actMethod.getParameterTypes();

            StringBuilder logString = new StringBuilder();
            String logPreText = act.getLocalClassName() + " " + actMethod.getName() + "(";
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


