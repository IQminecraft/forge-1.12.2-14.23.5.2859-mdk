package com.example.mymod;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.util.Set;

public class GameParams {
    private static PropertyMap map;

    public static void init() {
        Minecraft mc = Minecraft.getMinecraft();
        try {
            Field[] fields = Minecraft.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(mc);
                if (value instanceof PropertyMap) {
                    map = (PropertyMap) value;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getValue(String key) {
        if (map != null && map.containsKey(key)) {
            Set<Property> properties = (Set<Property>) map.get(key);
            return properties.iterator().next().getValue();
        }
        return null;
    }
}