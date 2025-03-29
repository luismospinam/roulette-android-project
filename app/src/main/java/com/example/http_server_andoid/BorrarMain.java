package com.example.http_server_andoid;

import java.util.Arrays;
import java.util.List;

public class BorrarMain {
    public static void main(String[] args) {
        List<String> apostar = Arrays.asList("8,31,20,28,4,6,12,1,11,34,13,19,9,32,14".split(","));
        List<String> jugadas = Arrays.asList("11,26,6,26,10,15,21,24,20,32,34,21,17,8,29,26,32,36,3,32,2,27,36,35,9,2,29,22,35,23,30,22,28,28,30,34,20,6,31,17,20,18,35,00,35,10,15,0,5,10,2,26,9,19,3,34,26,34,26,29,29,18,25,34,4,11,8,8,14,35,13,33,10,10,28,10,0,20,34,26,3,11,27,12,11,16,12,7,27,30,0,14,19,15,31,6,16,5,6,23,17,28,36,0,12,00,28,23,6,18,19,29".split(","));
        int count = 0;

        for (String jugada : jugadas) {
            if (apostar.contains(jugada)) {
                count += (36 - apostar.size());
            } else {
                count -= apostar.size();
            }
        }

        System.out.println(count);
    }
}
