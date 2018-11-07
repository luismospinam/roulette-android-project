package com.example.luis.ruleta.utilitario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class ArchivoUtilitario {

    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append(",");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        fin.close();
        return ret;
    }

    public static List<String> orderenarListaNumerosString(List<String> lista){
        return lista.stream()
                .map(s -> s.equals("00") ? -1 : Integer.valueOf(s))
                .sorted()
                .map(i -> i.equals(-1) ? "00" : i.toString())
                .collect(Collectors.toList());

    }

}
