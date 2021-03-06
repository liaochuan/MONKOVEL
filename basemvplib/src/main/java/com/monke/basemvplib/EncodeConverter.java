package com.monke.basemvplib;

import android.support.annotation.NonNull;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public class EncodeConverter extends Converter.Factory {

    private EncodeConverter(){

    }

    public static EncodeConverter create(){
        return new EncodeConverter();
    }

    @Override
    public Converter<ResponseBody, String> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new Converter<ResponseBody, String>() {
            @Override
            public String convert(@NonNull ResponseBody value) throws IOException {
                byte[] responseBytes = value.bytes();
                UniversalDetector detector = new UniversalDetector(null);
                detector.handleData(responseBytes, 0, responseBytes.length);
                detector.dataEnd();
                return new String(responseBytes, detector.getDetectedCharset());
            }
        };
    }
}
