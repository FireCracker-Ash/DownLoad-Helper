package org.http;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static final int MAX_THREADS=32;//调节下载的线程数量
    public static final ExecutorService executor= Executors.newFixedThreadPool(MAX_THREADS);

    public static final String filePath="url.txt";
    public static final List<String> errorDownloads=new ArrayList<>();

    public static Map<String,Integer> stringCounts=new HashMap<>();

    public static void main(String[] args) {
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(filePath)), StandardCharsets.UTF_8))){
            String line;
            while ((line=reader.readLine())!=null){
                line=extractUrl(line);
                if(line.startsWith("https://")){
                    String finalLine = line;
                    executor.submit(()->downloadImg(finalLine));
                }
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(Colour.GREEN+"\nDownload completed"+Colour.RESET);
        if(!errorDownloads.isEmpty()){
            System.out.println("\n"+Colour.RED+"Error downloads:"+Colour.MAGENTA);
            for (String errorDownload : errorDownloads) {
                System.out.println(errorDownload);
            }
            System.out.print(Colour.RESET);
        }
        //End
    }

    public static void downloadImg(String imgUrl){
        String processCode="Not started";//错误分析状态码
        float wrongTime=0;//错误次数
        String fileName=imgUrl.substring(imgUrl.lastIndexOf("/")+1);
        //fileName=stringSet(fileName);//出现重复文件名时调用
        File file=new File(fileName);
        System.out.println();
        while(!processCode.equals("Successful")&&wrongTime<4){
            try{
                URL url=new URL(imgUrl);
                HttpURLConnection connection= (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setReadTimeout(4000);

                //测试连接性
                processCode="Connecting";
                int responseCode=connection.getResponseCode();
                if(responseCode!=HttpURLConnection.HTTP_OK){
                    System.out.println(Colour.RED+"Failed to connect"+Colour.RESET);
                    System.out.println(responseCode);
                    wrongTime+=0.3F;
                    continue;
                }else{
                    System.out.println("Successful connect");
                }

                //检查类型是否为图片
                //System.out.println();
                processCode="Check";
                String type=connection.getContentType();
                if(type.startsWith("image/")){
                    System.out.println("Type: "+type);
                }else{
                    System.out.println(Colour.RED+"Wrong type");
                    System.out.println("Type: "+type+Colour.RESET);
                    return;
                }

                //下载图片
                System.out.println("File name: "+fileName);//输出文件的名称
                processCode="Downloading";
                try(InputStream in= connection.getInputStream()){
                    FileOutputStream out=new FileOutputStream(fileName);
                    byte[] buffer=new byte[4096];
                    int byteRead;
                    System.out.println("Start downloading");
                    while ((byteRead=in.read(buffer))>=0){
                        out.write(buffer,0,byteRead);
                    }
                    out.flush();
                    processCode="Successful";
                    System.out.println("Download successfully");
                }catch (IOException e){
                    System.out.println(Colour.RED+"Failed to write to file"+Colour.RESET);
                    wrongTime+=0.4F;
                }
            }catch (IOException e){
                System.out.print(Colour.RED);
                file.delete();
                //System.out.println("Delete");
                switch (processCode){
                    case "Connecting":
                        System.out.println("[Catch] Failed to connect");
                        wrongTime++;
                        break;
                    case "Downloading":
                        System.out.println("[Catch] Failed to write to file");
                        wrongTime+=0.3F;
                        break;
                    default:
                        e.printStackTrace();
                        wrongTime++;
                        break;
                }
                System.out.print(Colour.RESET);
            }
        }
        if(!(processCode.equals("Successful"))){
            errorDownloads.add(fileName);
        }
    }

    public static String extractUrl(String original){
        if(original.contains("http")){
            return original.substring(original.indexOf("http"));
        }
        return "";
    }

    public static class Colour{
        public static String RED="\u001B[31m";
        public static String GREEN="\u001B[32m";
        public static String YELLOW="\u001B[33m";
        public static String MAGENTA="\u001B[35m";
        public static String RESET="\u001B[0m";
    }

    public static int count=1;

    public static String stringSet(String input){
        if(input.equals("001.jpg")){//特征名称，需修改
            if(stringCounts.containsKey(input)){
                count++;
            }else{
                stringCounts.put(input,1);
            }
        }
        return "("+count+") "+input;
    }

}