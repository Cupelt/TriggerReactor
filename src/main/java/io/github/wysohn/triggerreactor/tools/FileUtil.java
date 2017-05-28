/*******************************************************************************
 *     Copyright (C) 2017 wysohn
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package io.github.wysohn.triggerreactor.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;

public class FileUtil {
    /**
     *
     * @param file target file
     * @param str string to save
     * @throws IOException
     */
    public static void writeToFile(File file, String str) throws IOException{
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File temp = File.createTempFile("CopyOf_"+file.getName(), ".tmp", file.getParentFile());

        try(FileOutputStream fos = new FileOutputStream(temp);
                OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");){
            osw.write(str);
        }catch(IOException e){
            throw e;
        }

        try (FileInputStream istream = new FileInputStream(temp);
                FileOutputStream ostream = new FileOutputStream(file)) {
            FileChannel src = istream.getChannel();
            FileChannel dest = ostream.getChannel();
            dest.transferFrom(src, 0, src.size());
        } catch (IOException e) {
            throw e;
        }

        temp.delete();
    }

    /**
     * same as file.delete() if 'file' is file; recursively deletes all elements inside if 'file' is directory.
     * @param file
     */
    public static void delete(File file){
        if(file.isFile()){
            file.delete();
        }else{
            for(File f : file.listFiles()){
                delete(f);
            }
            file.delete();
        }
    }
}
