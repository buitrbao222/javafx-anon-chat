package com.anon_chat.utils;

import java.util.List;
import java.util.Random;

public class ListUtils {
    public static int getRandomIndex(List<?> list) {
        return new Random().nextInt(list.size());
    }

    public static <T> T removeRandomItem(List<T> list) {
        int randomIndex = getRandomIndex(list);
        return list.remove(randomIndex);
    }
}
