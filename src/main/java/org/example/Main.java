package org.example;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        System.out.println("MyHashMap test:");
//        Map<String, Integer> map =  new HashMap<>(9,0.75f);
        MyHashMap<String, Integer> map = new MyHashMap<>(9, 0.75f);
//        MyHashMap<String, Integer> map = new MyHashMap<>();

        map.put("k1", 100);
        map.put("k2", 99);

        System.out.println("k1 value: "+map.get("k1")); // 100
        System.out.println("k2 value: "+map.get("k2")); // 99

        map.put("k1", 777);

        System.out.println("k1 new value: "+map.get("k1")); // 777
        System.out.println("current map size: "+map.size());

        map.remove("k1"); // 777 -> null

        System.out.println("k1 value after remove: "+map.get("k1")); // null
        System.out.println("current map size: "+map.size()); // 1
    }
}