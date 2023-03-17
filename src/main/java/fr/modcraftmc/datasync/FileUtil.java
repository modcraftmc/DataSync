package fr.modcraftmc.datasync;

import java.io.*;

public class FileUtil {
    public static boolean ensureFileExist(File file){
        if(file.exists()) return true;
        file.getParentFile().mkdirs();
        try {
            if(!file.createNewFile()){
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean write(File file, String data){
        try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file));
            writer.write(data);
            writer.close();
        } catch (IOException e) {
            DataSync.LOGGER.error(e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean read(File file, StringBuilder dataHolder){
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
            StringWriter writer = new StringWriter();
            reader.transferTo(writer);
            reader.close();
            dataHolder.append(writer);
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
